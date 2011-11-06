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
package com.wakemeski.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.wakemeski.Log;
import com.wakemeski.R;
import com.wakemeski.core.Report;
import com.wakemeski.core.ReportController;
import com.wakemeski.core.ReportListener;
import com.wakemeski.core.ResortManager;
import com.wakemeski.core.WakeMeSkiFactory;

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
	private static final int ALERTS_ID     = Menu.FIRST + 2;
	private static final String TAG = "WakeMeSkiDashboard";
	private int mApVersion = 0;
	private int mApLatestVersion = 0;
	private ReportController mReportController;
	private ResortManager	 mResortManager;
	private static final int DIALOG_ID_LESS_THAN_MIN_VERSION = 0;
	private static final int DIALOG_ID_LESS_THAN_LATEST_VERSION = 1;
	private static final String UPDATE_IGNORE_PREF_KEY = "updateIgnoredVersion";
	private Button mAddResortsButton;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Eula.show(this);

		mReportController = WakeMeSkiFactory.getInstance(this.getApplicationContext()).getReportController();
		mResortManager = WakeMeSkiFactory.getInstance(this.getApplicationContext()).getRestortManager();

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
		mAddResortsButton = (Button)findViewById(R.id.add_resort_button_dashboard);

		/*
		 * If the user hasn't configured a resort we will turn this button on with
		 * setVisibility() to allow them to select a resort.
		 * By default it will be invisible
		 */
		mAddResortsButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(WakeMeSkiDashboard.this,
						ResortListActivity.class));
			}
		});

		mListAdapter = new ReportListAdapter(getApplicationContext());
		mReportsList.setAdapter(mListAdapter);

		mReportsList.setOnItemClickListener(mClickListener);
	}

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * Update visibility of the add resorts button based on number of
		 * configured resorts
		 */
		updateAddResortsButton();

		mReportController.addListener(mReportListener);
		if( mReportController.isBusy() ) {
			setProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		mReportController.removeListener(mReportListener);
	}

	/**
	 * Update visibility of the mAddResortsButton button based on number
	 * of configured resorts.  When resorts are configured, remove from the view.
	 */
	private void updateAddResortsButton() {
		if( mResortManager.getResorts().length != 0 ) {
			mAddResortsButton.setVisibility(View.GONE);
		} else {
			mAddResortsButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);
		MenuItem refresh = menu.findItem(REFRESH_ID);
		if( refresh != null ) {
			/*
			 * Don't allow refresh while the report controller is busy with another request.
			 */
			refresh.setEnabled(!mReportController.isBusy());
		}
		return result;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, REFRESH_ID, 0, R.string.refresh);
		item.setIcon(R.drawable.ic_menu_refresh);

		item = menu.add(0, ALERTS_ID, 0, R.string.show_alerts);
		item.setIcon(R.drawable.ic_menu_notifications);

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
			/*
			 * Start the report load as a background process
			 */
			mReportController.forceLoadReports(true);
			return true;
		} else if( item.getItemId() == ALERTS_ID ) {
			Intent i = new Intent(this, AlertsActivity.class);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	private final ReportListener mReportListener = new ReportListener() {
		Handler h = new Handler();

		@Override
		public void onUpdated() {
			h.post(new Runnable() {
				@Override
				public void run() {
					updateAddResortsButton();
				}
			});
		}
		@Override
		public void onAdded(Report r) {
			if( r.getServerInfo().getApMinSupportedVersion() > mApVersion ) {
				h.post(new Runnable() {
					@Override
					public void run() {
						showDialog(DIALOG_ID_LESS_THAN_MIN_VERSION);
					}
				});
			} else if ( r.getServerInfo().getApLatestVersion() > mApVersion ) {
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

				/*
				 * I'd rather pass this in as a bundle item to showDialog
				 * but it looks like we don't have API support for this with 1.5
				 */
				mApLatestVersion = r.getServerInfo().getApLatestVersion();

            	if( !prefs.contains(UPDATE_IGNORE_PREF_KEY) ||
            			prefs.getInt(UPDATE_IGNORE_PREF_KEY,0)
            				< r.getServerInfo().getApLatestVersion() ) {
    				h.post(new Runnable() {
    					@Override
						public void run() {
    						showDialog(DIALOG_ID_LESS_THAN_LATEST_VERSION);
    					}
    				});
            	} else {
            		Log.d("Not on latest version " + r.getServerInfo().getApLatestVersion() + " but I've already bugged you once...");
            	}


			}
		}

		@Override
		public void onLoading(final boolean started) {
		}

		@Override
		public void onBusy(final boolean isBusy) {
			h.post(new Runnable() {
				@Override
				public void run() {
					setProgressBarIndeterminateVisibility(isBusy);
				}
			});
		}
	};


    @Override
	protected Dialog onCreateDialog(int id) {
        if( id == DIALOG_ID_LESS_THAN_MIN_VERSION) {
	        return new AlertDialog.Builder(this)
	            .setTitle(R.string.please_upgrade)
	            .setMessage(R.string.no_longer_supported)
	            .setPositiveButton(R.string.update_button, new DialogInterface.OnClickListener() {
	                @Override
					public void onClick(DialogInterface dialog, int which) {
	                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
	                        "http://market.android.com/details?id=" + getPackageName()));
	                    startActivity(marketIntent);
	                }
	            })
	            .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
	                @Override
					public void onClick(DialogInterface dialog, int which) {
	                    finish();
	                }
	            })
	            .create();
        } else if (id ==  DIALOG_ID_LESS_THAN_LATEST_VERSION ) {
	        return new AlertDialog.Builder(this)
            .setTitle(R.string.please_upgrade)
            .setMessage(R.string.out_of_date_dialog_body)
            .setPositiveButton(R.string.update_button, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int which) {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                    startActivity(marketIntent);
                }
            })
            .setNegativeButton(R.string.ignore_button, new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int which) {
					/*
					 * If the user selected ignore and this isn't a mandatory
					 * update don't bother them until the next update.
					 */
                	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                	SharedPreferences.Editor editor = prefs.edit();
                	/*
                	 * Save the fact that the user ignored this update, don't bug
                	 * again until the next update
                	 */
                	editor.putInt(UPDATE_IGNORE_PREF_KEY, mApLatestVersion);
                	if( editor.commit() ) {
                		Log.d("OK I promise I won't bug you again... until the next version");
                	} else {
                		Log.w(TAG, "Ignore pref key commit failed");
                	}

                }
            })
            .create();
        } else {
        	return super.onCreateDialog(id);
        }
    }


	private final OnItemClickListener mClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
		{
			Report r = (Report)mListAdapter.getItem(pos);
			if( r != null ) {
				Intent i = new Intent(WakeMeSkiDashboard.this, ReportActivity.class);
				i.putExtra("report", r);
				startActivityForResult(i, 0);
			}
		}
	};
}
