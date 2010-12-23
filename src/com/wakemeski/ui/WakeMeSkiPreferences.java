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

import java.util.Calendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.wakemeski.R;
import com.wakemeski.core.alert.AlertPollingController;
import com.wakemeski.pref.RepeatDaySharedPreference;
import com.wakemeski.pref.TimeSettingsSharedPreference;
import com.wakemeski.ui.alarmclock.AlarmPreference;
import com.wakemeski.ui.alarmclock.AlarmPreference.IRingtoneChangedListener;

/**
 * The main preferences activity, used to show wakemeski application
 * preferences.
 * 
 * @author dan
 * 
 */
public class WakeMeSkiPreferences extends PreferenceActivity implements
		IRingtoneChangedListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	private RepeatDayPreference mDayPreference;
	private TimePickerPreference mWakeUpTimePreference;
	private CheckBoxPreference mAlarmEnablePreference;
	private AlarmPreference mAlarmTonePreference;
	private PreferenceScreen mDashboardPreference;
	private PreferenceScreen mResortsPreference;
	private SnowSettingsPreference mWakeupSnowSettings;
	private String TAG = "WakeMeSkiPreferences";
	private AlarmController mAlarmController;
	public static final String ALARM_ENABLE_PREF_KEY = "alarm_enable";
	public static final String ALARM_TONE_PREF_KEY = "alarm_tone";
	public static final String REPEAT_DAYS_PREF_KEY = "alarm_repeat_days";
	public static final String ALARM_WAKEUP_TIME_PREF_KEY = "alarm_wakeup_time";
	public static final String SELECTED_RESORTS_PREF_KEY = "selected_resorts";
	/*
	 * Note: this was the name used before notifications were used.  Don't change it now
	 * otherwise previous user settings will be lost.
	 */
	public static final String SNOW_WAKEUP_SETTINGS_KEY = "snow_settings";
	public static final String SNOW_ALERT_SETTINGS_KEY = "snow_alert_settings";
	public static final String NOTIFY_ENABLE_PREF_KEY	= "notification_enable";
	public static final boolean DEBUG = false;
	private static final int TEST_ALARM_FIRE_ID = Menu.FIRST + 1;
	private static final int TEST_SERVICE_FIRE_ID = Menu.FIRST + 2;


	private void updateToneSummary(Uri ringtoneUri) {
		// this code doesn't work for some reason - I can't get the real name of
		// the ringtone
		// RingtoneManager ringtoneManager = new RingtoneManager(this);
		// int ringtonePosition =
		// ringtoneManager.getRingtonePosition(ringtoneUri);
		// if( ringtonePosition >= 0 ) {
		// Ringtone ringtone = ringtoneManager.getRingtone(ringtonePosition);
		// if(ringtone != null ) {
		// mAlarmTonePreference.setSummary(ringtone.getTitle(WakeMeSkiPreferences.this));
		// }
		// }
	}

	public void onRingtoneChanged(Uri ringtoneUri) {
		updateToneSummary(ringtoneUri);
	}

	private void alarmSchedulingPreferenceUpdated(SharedPreferences sharedPreferences,
			String key) {
		boolean alarmEnabled = mAlarmEnablePreference.isChecked();
		/**
		 * If the enable key was the key changed, get the latest value
		 * from the passed object.
		 */
		if( key.equals(ALARM_ENABLE_PREF_KEY) ) {
			alarmEnabled = sharedPreferences.getBoolean(key, false);
		}
		
		if ( alarmEnabled ) {
			
			RepeatDaySharedPreference dayPref = mDayPreference
													.getSharedPreference();
			TimeSettingsSharedPreference timePref=	mWakeUpTimePreference
													.getSharedPreference();
			
			/**
			 * I've found through testing that onSharedPreference() is fired multiple
			 * times when preferences change and unfortunately in some cases at least
			 * the second time the preferences value returned by .getSharedPreference()
			 * returns a preference containing the previous values.
			 * To work around this, check for keys with value REPEAT_DAYS_PREF_KEY or
			 * ALARM_WAKEUP_TIME_PREF_KEY.  If we find this key, create a new 
			 * preferences object with the value of the shared preference and use this
			 * instead of the member.getSharedPreference() value.
			 */
			if(key.equals(REPEAT_DAYS_PREF_KEY) ||
					key.equals(ALARM_WAKEUP_TIME_PREF_KEY)) {
				String value = sharedPreferences.getString(key, "");
				if(value.length() > 0) {
					if (key.equals(REPEAT_DAYS_PREF_KEY)) {
						dayPref = new RepeatDaySharedPreference();
						dayPref.setFromPersistString(value);
					} else if (key.equals(ALARM_WAKEUP_TIME_PREF_KEY)) {
						timePref = new TimeSettingsSharedPreference(this.getApplicationContext());
						timePref.setTimeFromPersistString(value);
					}
				}
			} 
			AlarmCalculator calculator = new AlarmCalculator(dayPref,timePref);
			Calendar nextAlarm = calculator.getNextAlarm();
			if (nextAlarm != null) {
				if (mAlarmController.setAlarm(nextAlarm)) {
					Toast toast = Toast.makeText(this, R.string.alarm_updated,
							Toast.LENGTH_SHORT);
					toast.show();
				}
			} else {
				Log.d(TAG, "No days selected");
				Toast toast = Toast.makeText(this, 
						R.string.alarm_disabled_no_days_selected,
						Toast.LENGTH_SHORT);
				toast.show();
				mAlarmController.clearAlarm();
			}
		} else {
			Log.d(TAG, "Alarm is not enabled");
			Toast toast = Toast.makeText(this, R.string.alarm_disabled,
					Toast.LENGTH_SHORT);
			toast.show();
			mAlarmController.clearAlarm();
		}
	}

	/**
	 * @param key
	 * @return true if this key value is a preference which relates to alarm scheduling.
	 */
	private boolean isAlarmSchedulingRelatedPreferenceKey(String key) {
		return key.equals(ALARM_ENABLE_PREF_KEY) ||
				key.equals(REPEAT_DAYS_PREF_KEY) ||
				key.equals(ALARM_WAKEUP_TIME_PREF_KEY);
	}
	
	/**
	 * @param sharedPreferences the preferences object containing shared preferences
	 * for the application
	 * @return true if alert notification is enabled in this preference object
	 */
	public static boolean isAlertNotificationEnabled(SharedPreferences sharedPreferences) {
		return sharedPreferences.getBoolean(NOTIFY_ENABLE_PREF_KEY, false);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if( isAlarmSchedulingRelatedPreferenceKey(key) ) {
			alarmSchedulingPreferenceUpdated(sharedPreferences,key);
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
		mAlarmController = new AlarmController(this.getApplicationContext());
		addPreferencesFromResource(R.xml.preferences);
		mDayPreference = (RepeatDayPreference) findPreference(REPEAT_DAYS_PREF_KEY);
		mWakeUpTimePreference = (TimePickerPreference) findPreference(ALARM_WAKEUP_TIME_PREF_KEY);
		mAlarmEnablePreference = (CheckBoxPreference) findPreference(ALARM_ENABLE_PREF_KEY);
		mAlarmTonePreference = (AlarmPreference) findPreference(ALARM_TONE_PREF_KEY);
		mDashboardPreference = (PreferenceScreen) findPreference("dashboard");
		mResortsPreference = (PreferenceScreen) findPreference("selected_resorts");
		mWakeupSnowSettings = (SnowSettingsPreference) findPreference(SNOW_WAKEUP_SETTINGS_KEY);
		
		mAlarmTonePreference.setRingtoneChangedListener(this);

		Uri defaultAlarm = getDefaultAlarm();
		if (defaultAlarm != null) {
			Log.i(TAG, "Setting default alarm tone " + defaultAlarm);
			mAlarmTonePreference.setDefaultValue(defaultAlarm);
		}
		updateToneSummary(defaultAlarm);

		updateAlarmPreferences();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	/**
	 * picks the first alarm available
	 */
	private Uri getDefaultAlarm() {
		RingtoneManager ringtoneManager = new RingtoneManager(this);
		ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
		return ringtoneManager.getRingtoneUri(0);
	}

	/**
	 * Syncs the enabled state of alarm related settings to the enable checkbox
	 */
	private void updateAlarmPreferences() {
		boolean enabled = mAlarmEnablePreference.isChecked();
		mWakeUpTimePreference.setEnabled(enabled);
		mDayPreference.setEnabled(enabled);
		mAlarmTonePreference.setEnabled(enabled);
		mWakeupSnowSettings.setEnabled(enabled);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mAlarmEnablePreference) {
			updateAlarmPreferences();
			updateToneSummary(mAlarmTonePreference.getTone());
		} else if (preference == mDashboardPreference) {
			startActivity(new Intent(Intent.ACTION_MAIN, null, this,
					WakeMeSkiDashboard.class));
		} else if (preference == mResortsPreference) {
			startActivity(new Intent(Intent.ACTION_MAIN, null, this,
					ResortListActivity.class));
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
