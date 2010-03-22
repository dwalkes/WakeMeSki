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
package com.dwalkes.android.wakemeski;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.skireport.LocationFinderActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * A list activity that allows selection of resorts for the purposes of 
 * setting up wakeup on 24hr snow totals.  Uses android.skireport.LocationFinderActivity
 * to select locations, then allows the user to configure whether a wakeup is desired on this 
 * particular location. 
 * 
 * @author dan
 *
 */
public class ResortListActivity extends ListActivity {
    private ResortManager mResortManager;
	private static final int SELECT_LOCATION = 1;
	private Resort[] mResortList;
	private static final int ADD_ID = Menu.FIRST;
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, ADD_ID, 0, R.string.add_location);
		item.setIcon(android.R.drawable.ic_menu_add);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == ADD_ID)
		{
			launchFinder();
			return true;
		} 
	
		return super.onOptionsItemSelected(item);
	}
	
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mResortManager = ResortManager.getInstance(this.getApplicationContext());
		mResortList = mResortManager.getResorts();

    }

    /**
     * Launch the finder application from android.skireport
     */
    private void launchFinder() {
		Intent i = new Intent(this,LocationFinderActivity.class);
		startActivityForResult(i,SELECT_LOCATION);
    }
    
   
    private void addResort( Resort r ) 
    {
    	Set<Resort> resortSet = new HashSet<Resort>(Arrays.asList(mResortList));
    	Resort matchingLocation = Resort.getResortWithLocation(r.getLocation(), resortSet);
    	if( matchingLocation == null ) {
    		// this resort does not exist in the list yet, add it
    		resortSet.add(r);
    	} else {
    		// update the resort
    		resortSet.remove(matchingLocation);
    		resortSet.add(r);
    	}
    	mResortList = resortSet.toArray(new Resort[resortSet.size()]);
    	mResortManager.update(mResortList);
    }
    
    /**
     * Handle results from LocationFinderActivity
     */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if( requestCode == SELECT_LOCATION && resultCode == RESULT_OK)
		{
			String url = data.getStringExtra("url");
			String loc = data.getStringExtra("location");
			Resort r = new Resort(loc, url);
			r.setWakeupEnabled(true);
			addResort(r);
		}
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		if( mResortList.length == 0 ) {
			launchFinder();
		}
		else {
			setListAdapter(new WakeupResortListAdapter(ResortListActivity.this,
											mResortList));
			
			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			int i=0;
			for( Resort resort : mResortList ) {
				getListView().setItemChecked(i++, resort.isWakeupEnabled());
			}
		}
	}
	
	protected void onListItemClick( ListView l, View v, int position, long id) {
		Resort resort = (Resort)l.getItemAtPosition(position);
		if( l.isItemChecked(position) ) {
			resort.setWakeupEnabled(true);
		} else {
			resort.setWakeupEnabled(false);
		}
    	mResortManager.update(mResortList);
	}

	
}
