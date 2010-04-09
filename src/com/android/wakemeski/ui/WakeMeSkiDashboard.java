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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.wakemeski.R;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.WakeMeSkiService;

/**
 * The application dashboard for WakeMeSki. Shows the status of configured
 * resorts in a list based on total snowfall.
 *
 * @author dan
 *
 */
public class WakeMeSkiDashboard extends Activity implements
		WakeMeSkiService.SnowInfoListener {

	private WakeMeSkiService mBoundService;
	private Handler mHandler;

	private ListView          mReportsList;
	private ReportListAdapter mListAdapter;
	private static final int PREFERENCES_ID = Menu.FIRST;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.dashboard);

		mHandler = new Handler();

		mReportsList = (ListView)findViewById(R.id.dashboard_list);
		mListAdapter = new ReportListAdapter(getApplicationContext());
		mReportsList.setAdapter(mListAdapter);

		mReportsList.setOnItemClickListener(mClickListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mListAdapter.clearReports();

		Intent serviceIntent = new Intent(
					WakeMeSkiService.ACTION_DASHBOARD_POPULATE, null, this,
					WakeMeSkiService.class);

		setProgressBarIndeterminateVisibility(true);
		bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
		startService(serviceIntent);
	}

	/**
	 * Called by WakeMeSkiService when its refreshing all reports
	 */
	@Override
	public void onClear() {
		mHandler.post(new Runnable() {
			public void run() {
				mListAdapter.clearReports();
			}
		});
	}

	@Override
	public void onReport(final Report r) {

		mHandler.post(new Runnable() {
			public void run() {
				if (r == null) {
					setProgressBarIndeterminateVisibility(false);
				}
				mListAdapter.addReport(r);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mBoundService != null) {
			mBoundService.unregisterListener(this);
			unbindService(mConnection);
			mBoundService = null;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, PREFERENCES_ID, 0, R.string.set_preferences);
		item.setIcon(android.R.drawable.ic_menu_preferences);
		return result;
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == PREFERENCES_ID) {
			Intent i = new Intent(this, WakeMeSkiPreferences.class);
			startActivity(i);
			return true;
		} 
		return super.onOptionsItemSelected(item);
	}


	private OnItemClickListener mClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
		{
			Report r = (Report)mListAdapter.getItem(pos);
			if( r != null ) {
				Intent i = new Intent(WakeMeSkiDashboard.this, ReportActivity.class);
				i.putExtra("report", r);
				startActivityForResult(i, 0);
			}
			else {
				//there are no reports and the user pressed the
				// "please configure" item
				Intent i = new Intent(WakeMeSkiDashboard.this, WakeMeSkiPreferences.class);
				startActivity(i);
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((WakeMeSkiService.LocalBinder) service)
					.getService();
			mBoundService.registerListener(WakeMeSkiDashboard.this);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
		}
	};

}
