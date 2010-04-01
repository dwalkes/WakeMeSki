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
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.wakemeski.R;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.ResortManager;
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
	private ViewGroup mSnowListLayout;
	private Handler mHandler;
	private SnowSettingsSharedPreference mSnowSettings;
	private String TAG = "WakeMeSkiDashboard";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.dashboard);
		mSnowListLayout = (ViewGroup) this.findViewById(R.id.snow_list_layout);
		mHandler = new Handler();
	}

	@Override
	protected void onResume() {
		boolean startService = false;
		super.onResume();
		mSnowListLayout.removeAllViews();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		ResortManager selectedResorts = ResortManager
				.getInstance(getApplicationContext());
		// If the user hasn't selected any resorts, no sense trying to start the
		// service.. put up config issue text instead
		if (selectedResorts.getResorts().length == 0) {
			View configIssue = getLayoutInflater().inflate(
					R.layout.dashboard_config_detail, mSnowListLayout);
			Drawable d = getResources().getDrawable(
					R.drawable.dashboard_config_issue_background);
			configIssue.setBackgroundDrawable(d);
			TextView configIssueText = (TextView) configIssue
					.findViewById(R.id.config_detail);
			configIssueText.setText(R.string.no_resorts_selected_please_select);
		} else {
			startService = true;
		}
		mSnowSettings = new SnowSettingsSharedPreference();
		if (!mSnowSettings.setFromPreferences(prefs)) {
			Log.e(TAG, "Error obtaining snow settings preferences");
		}

		if (startService) {
			View waitText = getLayoutInflater().inflate(
					R.layout.dashboard_config_detail, null);
			TextView configIssueText = (TextView) waitText
					.findViewById(R.id.config_detail);
			configIssueText.setText(R.string.please_wait_for_service);
			Drawable d = getResources().getDrawable(
					R.drawable.dashboard_wait_background);
			waitText.setBackgroundDrawable(d);
			mSnowListLayout.addView(waitText);

			Intent serviceIntent = new Intent(
					WakeMeSkiService.ACTION_DASHBOARD_POPULATE, null, this,
					WakeMeSkiService.class);
			bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
			startService(serviceIntent);
		}
	}

	/**
	 * Called by WakeMeSkiService when its refreshing all reports
	 */
	@Override
	public void onClear() {
		mHandler.post(new Runnable() {
			public void run() {
				mSnowListLayout.removeAllViews();
			}
		});
	}

	@Override
	public void onReport(final Report r) {

		mHandler.post(new Runnable() {
			public void run() {
				if (r == null) {
					mSnowListLayout.removeViewAt(0);
					return;
				}
				View v = getLayoutInflater()
						.inflate(R.layout.snow_layout, null);

				Drawable d;
				if (Report.meetsPreference(r, mSnowSettings)) {
					d = getResources().getDrawable(
							R.drawable.exceeds_threshold_background);
				} else {
					d = getResources().getDrawable(
							R.drawable.below_threshold_background);
				}
				v.setBackgroundDrawable(d);

				TextView tv = (TextView) v.findViewById(R.id.resort_name);
				tv.setText(r.getLabel());
				tv = (TextView) v.findViewById(R.id.snow_value);
				tv.setText(r.getFreshAsString());
				mSnowListLayout.addView(v);
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
