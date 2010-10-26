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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.wakemeski.R;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.ReportController;
import com.android.wakemeski.core.ReportListener;
import com.android.wakemeski.core.WakeMeSkiFactory;

/**
 * The application dashboard for WakeMeSki. Shows the status of configured
 * resorts in a list based on total snowfall.
 *
 * @author dan
 *
 */
public class WakeMeSkiDashboard extends Activity {

	private ListView          mReportsList;
	private ReportListAdapter mListAdapter;
	private static final int PREFERENCES_ID = Menu.FIRST;
	private static final int REFRESH_ID     = Menu.FIRST + 1;
	private static final String TAG = "WakeMeSkiDashboard";
	private int mApVersion = 0;
	private ReportController mReportController;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mReportController = WakeMeSkiFactory.getInstance(this.getApplicationContext()).getReportController();

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.dashboard);

		try 
		{
			mApVersion = getApplicationContext().getPackageManager().
					getPackageInfo(getPackageName(),PackageManager.GET_ACTIVITIES).versionCode;
		} catch (NameNotFoundException e)
		{
			Log.e(TAG,"Caught NameNotFoundException quering my package!" + e.getLocalizedMessage() );
		}
		
		mReportsList = (ListView)findViewById(R.id.dashboard_list);
		mListAdapter = new ReportListAdapter(getApplicationContext());
		mReportsList.setAdapter(mListAdapter);

		mReportsList.setOnItemClickListener(mClickListener);
	}

	@Override
	protected void onResume() {
		super.onResume();		

		mReportController.addListener(mReportListener);
	}

	@Override
	protected void onPause() {
		super.onPause();

		mReportController.removeListener(mReportListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, REFRESH_ID, 0, R.string.refresh);
		item.setIcon(R.drawable.ic_menu_refresh);

		item = menu.add(0, PREFERENCES_ID, 0, R.string.set_preferences);
		item.setIcon(android.R.drawable.ic_menu_preferences);		

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == PREFERENCES_ID) {
			Intent i = new Intent(this, WakeMeSkiPreferences.class);
			startActivity(i);
			return true;
		} else if (item.getItemId() == REFRESH_ID) {
			mReportController.loadReports();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	
	private ReportListener mReportListener = new ReportListener() {
		Handler h = new Handler();

		@Override
		public void onAdded(Report r) {
			if( r.getApMinSupportedVersion() > mApVersion ) {
				h.post(new Runnable() {
					public void run() {
						showDialog(0);
					}
				});
			}
		}

		@Override
		public void onRemoved(Report r) {	
		}

		@Override
		public void onLoading(final boolean started) {
			h.post(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(started);
				}
			});	
		}
	};
	
    protected Dialog onCreateDialog(int id) {
        
        return new AlertDialog.Builder(this)
            .setTitle(R.string.out_of_date_dialog_title)
            .setMessage(R.string.out_of_date_dialog_body)
            .setPositiveButton(R.string.update_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                    startActivity(marketIntent);
                }
            })
            .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .create();
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
}
