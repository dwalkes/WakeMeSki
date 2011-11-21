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

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.wakemeski.Log;
import com.wakemeski.R;
import com.wakemeski.WakeMeSki;
import com.wakemeski.core.Resort;
import com.wakemeski.core.ResortManager;
import com.wakemeski.core.WakeMeSkiFactory;
import com.wakemeski.core.alert.AlertPollingController;
import com.wakemeski.deskclock_custom.WakeMeSkiAlarmCustomization;
import com.wakemeski.generic_deskclock.Alarms;

/**
 * The main preferences activity, used to show wakemeski application
 * preferences.
 *
 * @author dan
 *
 */
public class WakeMeSkiPreferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private CheckBoxPreference mAlarmEnablePreference;
	private PreferenceScreen mDashboardPreference;
	private PreferenceScreen mResortsPreference;
	private PreferenceScreen mSendLogsPreference;
	private SnowSettingsPreference mWakeupSnowSettings;
	private PreferenceScreen mAlarmSettingsPreference;
	private ResortManager	mResortManager;
	public static final String ALARM_ENABLE_PREF_KEY = "alarm_enable";
	public static final String SELECTED_RESORTS_PREF_KEY = "selected_resorts";
	/*
	 * Note: this was the name used before notifications were used.  Don't change it now
	 * otherwise previous user settings will be lost.
	 */
	public static final String SNOW_WAKEUP_SETTINGS_KEY = "snow_settings";
	public static final String SNOW_ALERT_SETTINGS_KEY = "snow_alert_settings";
	public static final String NOTIFY_ENABLE_PREF_KEY	= "notification_enable";
	public static final boolean DEBUG = WakeMeSki.DEBUG;
	private static final int TEST_ALARM_FIRE_ID = Menu.FIRST + 1;
	private static final int TEST_SERVICE_FIRE_ID = Menu.FIRST + 2;

	/**
	 * Use the intent extra EXTRA_START_PREF_SCREEN_WITH_KEY to start a specific
	 * preference within the preferences application
	 */
	public static final String EXTRA_START_PREF_SCREEN_WITH_KEY = "start_pref_with_key";
	public static final String NOTIFY_PREFS_SCREEN_KEY = "notification_prefs";
	// Spawned activity id's
	private final static int ALARM_CONFIGURE = 2;

	/**
	 * Based on change in alarm scheduling related preferences, pop up a toast message
	 * telling the user if the alarm is ready to go or not
	 * @param wakeupEnabled true if the "enable wake-up" box is checked
	 */
	private void alarmSchedulingPreferenceUpdated(boolean wakeupEnabled) {
	
		if ( wakeupEnabled ) {
			
			if( Alarms.getEnabledAlarmsQuery(getContentResolver()).getCount() != 0 ) {
				int wakeupEnabledResorts = 0;
				for( Resort r: mResortManager.getResorts()) {
					if( r.isWakeupEnabled() ) {
						wakeupEnabledResorts ++;
					}
				}
				Toast toast ;
				if( wakeupEnabledResorts != 0 ) {
					toast = Toast.makeText(this, R.string.alarm_updated,
							Toast.LENGTH_SHORT);
				} else {
					toast = Toast.makeText(this, R.string.please_enable_wakeup_on_resort,
							Toast.LENGTH_SHORT);
				}
				toast.show();
			} else {
				Log.d("No alarm configured");
				Toast toast = Toast.makeText(this,
						R.string.alarm_not_set,
						Toast.LENGTH_SHORT);
				toast.show();
			}
		} else {
			Log.d("Alarm is not enabled");
			Toast toast = Toast.makeText(this, R.string.wakeup_disabled,
					Toast.LENGTH_SHORT);
			toast.show();
		}
	}


	/**
	 * @param sharedPreferences the preferences object containing shared preferences
	 * for the application
	 * @return true if alert notification is enabled in this preference object
	 */
	public static boolean isAlertNotificationEnabled(SharedPreferences sharedPreferences) {
		return sharedPreferences.getBoolean(NOTIFY_ENABLE_PREF_KEY, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if( key.equals(ALARM_ENABLE_PREF_KEY) ) {
			boolean enabled = sharedPreferences.getBoolean(key, false);
			alarmSchedulingPreferenceUpdated(enabled);
		} else if (key.equals(NOTIFY_ENABLE_PREF_KEY)) {
			/*
			 * Turn on/off alert wakeup service funtionality based on the new
			 * settings of the notify enable preference.
			 */
			if( isAlertNotificationEnabled(sharedPreferences) ) {
				AlertPollingController.getInstance(getApplicationContext()).enableAlertPolling();
			} else {
				AlertPollingController.getInstance(getApplicationContext()).disableAlertPolling();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mAlarmEnablePreference = (CheckBoxPreference) findPreference(ALARM_ENABLE_PREF_KEY); 
		mDashboardPreference = (PreferenceScreen) findPreference("dashboard");
		mResortsPreference = (PreferenceScreen) findPreference("selected_resorts");
		mWakeupSnowSettings = (SnowSettingsPreference) findPreference(SNOW_WAKEUP_SETTINGS_KEY);
		mSendLogsPreference = (PreferenceScreen) findPreference("send_logs");
		mResortManager = WakeMeSkiFactory.getInstance(this.getApplicationContext()).getRestortManager();
		mAlarmSettingsPreference = (PreferenceScreen) findPreference("alarm_settings");
		updateAlarmPreferences();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);




	}

	@Override
	public void onResume() {
		super.onResume();
		Intent i = getIntent();
		/**
		 * If we specified starting at a specific preference screen,
		 * start it based on the value in the intent extra.
		 */
		if( i != null && i.hasExtra(EXTRA_START_PREF_SCREEN_WITH_KEY) ) {
			String startPrefScreen = i.getExtras().getString(EXTRA_START_PREF_SCREEN_WITH_KEY);
			setPreferenceScreen((PreferenceScreen) findPreference(startPrefScreen));
		}
	}

	/**
	 * Syncs the enabled state of alarm related settings to the enable checkbox
	 */
	private void updateAlarmPreferences() {
		boolean enabled = mAlarmEnablePreference.isChecked();
		mWakeupSnowSettings.setEnabled(enabled);
		String alarmSettingsSummary = getString(R.string.count_alarms_enabled ,
				Alarms.getEnabledAlarmsQuery(getContentResolver()).getCount());
		/**
		 * Set the number of currently enabled alarms in the alarm preferences dialog
		 */
		mAlarmSettingsPreference.setSummary(alarmSettingsSummary);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ALARM_CONFIGURE) {
			alarmSchedulingPreferenceUpdated(mAlarmEnablePreference.isChecked());
			updateAlarmPreferences();
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mAlarmSettingsPreference ) {
			startActivityForResult(new Intent(this, WakeMeSkiAlarmCustomization.getInstance().getAlarmClock()),ALARM_CONFIGURE);
		}
		if (preference == mAlarmEnablePreference) {
			updateAlarmPreferences();
		} else if (preference == mDashboardPreference) {
			startActivity(new Intent(Intent.ACTION_MAIN, null, this,
					WakeMeSkiDashboard.class));
		} else if (preference == mResortsPreference) {
			startActivity(new Intent(Intent.ACTION_MAIN, null, this,
					ResortListActivity.class));
		} else if (preference == mSendLogsPreference) {
			File logFile = Log.getInstance().getLogFile();
			if( logFile == null ) {
				Toast toast = Toast.makeText(this,
						R.string.log_file_not_available_insert_sdcard,
						Toast.LENGTH_SHORT);
				toast.show();
			} else {
				Resort[] resorts =WakeMeSkiFactory.getInstance(this.getApplicationContext()).getRestortManager().getResorts();
				Log.d("Configured resorts:");
				for( Resort resort : resorts ) {
					Log.d(resort + " isWakeupEnabled= " + resort.isWakeupEnabled());
				}
				Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			    emailIntent.setType("plain/text");
			    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]
			    {"wakemeski@ddubtech.com"});
			    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
			    "WakeMeSki debug log");
			    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
			    "Description of problem:");
			    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ logFile.getAbsolutePath()));
			    startActivity(Intent.createChooser(emailIntent,this.getString(R.string.send_mail)));
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	/**
	 * Handles menu items invoked from the menu
	 *
	 * @param item
	 *            the item invoked
	 * @return true if handled
	 */
	private boolean applyMenuChoice(MenuItem item) {
		/*
		if (WakeMeSkiPreferences.DEBUG) {
			switch (item.getItemId()) {
			case TEST_SERVICE_FIRE_ID:
				Calendar nextAlarm = Calendar.getInstance();
				// set alarm to fire in 5 seconds
				nextAlarm.add(Calendar.SECOND, 5);
				if (mAlarmController.setAlarm(nextAlarm)) {
					Toast toast = Toast.makeText(this,
							"Alarm check will occur in 5 seconds",
							Toast.LENGTH_SHORT);
					toast.show();
				}
				return true;

			case TEST_ALARM_FIRE_ID:
				AlarmController alarmController = new AlarmController(this);
				alarmController.fireAlarm();
				return true;

			}
		}
		*/
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return applyMenuChoice(item) || super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (WakeMeSkiPreferences.DEBUG) {
			menu.add(Menu.NONE, TEST_ALARM_FIRE_ID, Menu.NONE, "Test Fire");
			menu.add(Menu.NONE, TEST_SERVICE_FIRE_ID, Menu.NONE,
					"Schedule Check");
		}
		return super.onCreateOptionsMenu(menu);
	}
}
