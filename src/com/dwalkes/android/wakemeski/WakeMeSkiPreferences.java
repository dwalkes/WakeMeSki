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

import com.dwalkes.android.wakemeski.alarmclock.AlarmPreference;
import com.dwalkes.android.wakemeski.alarmclock.AlarmPreference.IRingtoneChangedListener;

/**
 * The main preferences activity, used to show wakemeski application preferences.
 * 
 * @author dan
 *
 */
public class WakeMeSkiPreferences extends PreferenceActivity 
		implements IRingtoneChangedListener,SharedPreferences.OnSharedPreferenceChangeListener {
    
	private RepeatDayPreference mDayPreference;
	private TimePickerPreference mWakeUpTimePreference;
	private CheckBoxPreference	mAlarmEnablePreference;
	private AlarmPreference mAlarmTonePreference;
	private PreferenceScreen mDashboardPreference;
	private PreferenceScreen mResortsPreference;
	private String TAG = "WakeMeSkiPreferences";
	private AlarmController mAlarmController;
	public static final String ALARM_ENABLE_PREF_KEY = "alarm_enable" ;
	public static final String ALARM_TONE_PREF_KEY = "alarm_tone";
	public static final String REPEAT_DAYS_PREF_KEY = "alarm_repeat_days";
	public static final String ALARM_WAKEUP_TIME_PREF_KEY =  "alarm_wakeup_time";
	public static final String SNOW_SETTINGS_PREF_KEY = "snow_settings";
	public static final String SELECTED_RESORTS_PREF_KEY = "selected_resorts";
	public static final boolean DEBUG = false;
	private static final int TEST_ALARM_FIRE_ID = Menu.FIRST+1;
	private static final int TEST_SERVICE_FIRE_ID = Menu.FIRST+2;
	/**
	 * Updates the alarm when preferences change.
	 */
	private void updateAlarm() {
		if( mAlarmEnablePreference.isChecked() ) {
			AlarmCalculator calculator = new AlarmCalculator(mDayPreference.getSharedPreference(),mWakeUpTimePreference.getSharedPreference());
			Calendar nextAlarm = calculator.getNextAlarm();
			if( nextAlarm != null ) {
				if( mAlarmController.setAlarm(nextAlarm) ) {
					Toast toast = Toast.makeText(this, R.string.alarm_updated, Toast.LENGTH_SHORT);
					toast.show();
				}
			} else { 
				Log.d(TAG,"No days selected");
				Toast toast = Toast.makeText(this, R.string.alarm_disabled, Toast.LENGTH_SHORT);
				toast.show();
				mAlarmController.clearAlarm();
			}
		} else {
			Log.d(TAG,"Alarm is not enabled");
			Toast toast = Toast.makeText(this, R.string.alarm_disabled, Toast.LENGTH_SHORT);
			toast.show();
			mAlarmController.clearAlarm();
		}
	}
	
	private void updateToneSummary(Uri ringtoneUri) {
		// this code doesn't work for some reason - I can't get the real name of the ringtone
//        RingtoneManager ringtoneManager = new RingtoneManager(this);
//        int ringtonePosition = ringtoneManager.getRingtonePosition(ringtoneUri);
//        if( ringtonePosition >= 0 ) {
//        	Ringtone ringtone = ringtoneManager.getRingtone(ringtonePosition); 
//        	if(ringtone != null ) {
//        		mAlarmTonePreference.setSummary(ringtone.getTitle(WakeMeSkiPreferences.this));
//        	}
//        }	
	}
	
		
	public void onRingtoneChanged(Uri ringtoneUri) {
		updateToneSummary(ringtoneUri);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateAlarm();
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
        mAlarmTonePreference.setRingtoneChangedListener(this);
        
		
        Uri defaultAlarm = getDefaultAlarm();
        if( defaultAlarm != null ) {
        	Log.i(TAG,"Setting default alarm tone " + defaultAlarm);
        	mAlarmTonePreference.setDefaultValue(defaultAlarm);
        }
        updateToneSummary(defaultAlarm);

        updateAlarmPreferences();

        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);       
        
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
		if( mAlarmEnablePreference.isChecked() ) {
			mWakeUpTimePreference.setEnabled(true);
			mDayPreference.setEnabled(true);
			mAlarmTonePreference.setEnabled(true);
		} else {
			mWakeUpTimePreference.setEnabled(false);
			mDayPreference.setEnabled(false);
			mAlarmTonePreference.setEnabled(false);
		}
    }
    
	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if( preference == mAlarmEnablePreference ) {
			updateAlarmPreferences();
			updateToneSummary(mAlarmTonePreference.getTone());
		}
		else if (preference == mDashboardPreference) {
			startActivity( new Intent(Intent.ACTION_MAIN,null,this,WakeMeSkiDashboard.class));
		} else if ( preference == mResortsPreference ) {
			startActivity( new Intent(Intent.ACTION_MAIN,null,this,ResortListActivity.class));
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	/**
	 * Handles menu items invoked from the menu
	 * @param item the item invoked
	 * @return true if handled
	 */
	private boolean applyMenuChoice(MenuItem item) {
		if( WakeMeSkiPreferences.DEBUG ) {
			switch(item.getItemId()) {
				case	TEST_SERVICE_FIRE_ID:
					Calendar nextAlarm = Calendar.getInstance();
					// set alarm to fire in 5 seconds
					nextAlarm.add(Calendar.SECOND,5);
					if(  mAlarmController.setAlarm(nextAlarm) ) {
						Toast toast =Toast.makeText(this, "Alarm check will occur in 5 seconds", Toast.LENGTH_SHORT); 
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
		if( WakeMeSkiPreferences.DEBUG ) {
			menu.add(Menu.NONE, TEST_ALARM_FIRE_ID, Menu.NONE, "Test Fire");
			menu.add(Menu.NONE, TEST_SERVICE_FIRE_ID, Menu.NONE, "Schedule Check");
		}
		return super.onCreateOptionsMenu(menu);
	}

}