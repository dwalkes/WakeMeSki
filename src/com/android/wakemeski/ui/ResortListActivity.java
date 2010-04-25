/*
 * Copyright (C) 2010 Dan Walkes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.android.wakemeski.ui;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.wakemeski.R;
import com.android.wakemeski.core.ReportController;
import com.android.wakemeski.core.Resort;
import com.android.wakemeski.core.ResortManager;

/**
 * A list activity that allows selection of resorts for the purposes of setting
 * up wakeup on 24hr snow totals. Uses
 * com.android.wakemeski.core.LocationFinderActivity to select locations, then
 * allows the user to configure whether a wakeup is desired on this particular
 * location.
 * 
 * @author dan
 * 
 */
public class ResortListActivity extends ListActivity {
	private ResortManager mResortManager;
	private static final int SELECT_LOCATION = 1;
	private Resort[] mResortList;
	private static final int ADD_ID = Menu.FIRST;
	private static final int CLEAR_ID = ADD_ID + 1;	
	private static final int REMOVE_ID = ADD_ID + 2;
	private WakeupResortListAdapter mListAdapter;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, ADD_ID, 0, R.string.add_location);
		item.setIcon(android.R.drawable.ic_menu_add);
		item = menu.add(0, CLEAR_ID, 0, R.string.clear_locations);
		item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == ADD_ID) {
			launchFinder();
			return true;
		} else if (item.getItemId() == CLEAR_ID) {
			clearAllResorts();
			update();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mResortManager = ResortManager
				.getInstance(this.getApplicationContext());
		mResortList = mResortManager.getResorts();
		setTitle(R.string.resort_list_title);
		setContentView(R.layout.resort_list_activity);
		registerForContextMenu(getListView());

	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE,REMOVE_ID,Menu.NONE,R.string.remove_location);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		if( item.getItemId() == REMOVE_ID ) {
			 Resort resort = mListAdapter.getItem(info.position);
			 removeResort(resort);
			 update();
			 return true;
		}
		return super.onContextItemSelected(item);
	}
	/**
	 * Launch the finder application from com.android.wakemeski.core
	 */
	private void launchFinder() {
		Intent i = new Intent(this, LocationFinderActivity.class);
		startActivityForResult(i, SELECT_LOCATION);
	}

	private void addResort(Resort r) {
		Resort matchingLocation =
			Resort.getResortWithLocation(r.getLocation(), mResortList);
		ArrayList<Resort> rl =
			new ArrayList<Resort>(Arrays.asList(mResortList));
		if (matchingLocation != null) {
			// remove the resort, and we'll add the latest one selected
			rl.remove(matchingLocation);
		}
		rl.add(r);
		mResortList = rl.toArray(new Resort[rl.size()]);
		Arrays.sort(mResortList);
		ReportController.getInstance(null).addResort(r);
		mResortManager.update(mResortList);
	}

	private void removeResort(Resort r) {
		Resort matchingLocation =
			Resort.getResortWithLocation(r.getLocation(), mResortList);
		if (matchingLocation != null) {
			ArrayList<Resort> rl = 
				new ArrayList<Resort>(Arrays.asList(mResortList));
			rl.remove(matchingLocation);
			ReportController.getInstance(null).removeResort(r);
			mResortList = rl.toArray(new Resort[rl.size()]);
			mResortManager.update(mResortList);
		}
	}

	private void clearAllResorts() {
		mResortList = new Resort[0];
		mResortManager.update(mResortList);
	}
	/**
	 * Handle results from LocationFinderActivity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SELECT_LOCATION && resultCode == RESULT_OK) {
			String url = data.getStringExtra("url");
			String loc = data.getStringExtra("location");
			Resort r = new Resort(loc, url);
			r.setWakeupEnabled(true);
			addResort(r);
		} else if (resultCode == RESULT_CANCELED) {
			/* 
			 * If the user cancelled out of the location finder, they also want to back out of this 
			 * activity, since it appears to the user as the same activity.
			 */
			finish();
		}
	}

	/**
	 * Updates the list view to match the current values set in the resort list
	 */
	private void update() {
		if (mResortList.length == 0) {
			launchFinder();
		} else {
			mListAdapter = new WakeupResortListAdapter(ResortListActivity.this,
					mResortList);
			setListAdapter(mListAdapter);

			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			int i = 0;
			for (Resort resort : mResortList) {
				getListView().setItemChecked(i++, resort.isWakeupEnabled());
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		update();
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Resort resort = (Resort) l.getItemAtPosition(position);
		if (l.isItemChecked(position)) {
			resort.setWakeupEnabled(true);
		} else {
			resort.setWakeupEnabled(false);
		}
		mResortManager.update(mResortList);
	}

}
