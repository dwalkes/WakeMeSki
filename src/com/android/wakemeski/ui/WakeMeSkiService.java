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

import java.util.Calendar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.wakemeski.core.ResortManager;
import com.android.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

/**
 * A service which runs in the background to obtain resort information
 * for the purposes of populating a dashboard with current resort status
 * or starting an alarm in the case a wakeup condition has occurred. 
 * 
 * @author dan
 *
 */
public class WakeMeSkiService extends IntentService {

	private static final String TAG = "WakeMeSkiService";
	public static final String ACTION_WAKE_CHECK = "com.android.wakemeski.ui.ACTION_WAKE_CHECK";
	public static final String ACTION_ALARM_SCHEDULE = "com.walkes.android.wakemeski.ACTION_ALARM_SCHEDULE";
	public static final String ACTION_DASHBOARD_POPULATE = "com.walkes.android.wakemeski.ACTION_DASHBOARD_POPULATE";
	private ResortSnowInfo[] mLastSnowInfo = null;
	private  SnowInfoListener mListener = null;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
	public WakeMeSkiService() {
		super("WakeMeSkiService");
	}
    
	private String mCurrentAction;
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
    	WakeMeSkiService getService() {
            return WakeMeSkiService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    
    public void onCreate() {
        // Maintain a lock during the checking of the alarm. This lock may have
        // already been acquired in AlarmReceiver. If the process was killed,
        // the global wake lock is gone. Acquire again just to be sure.
        AlarmAlertWakeLock.acquireCpuWakeLock(this.getApplicationContext());
        super.onCreate();
    }
    
    public interface SnowInfoListener {
    	public void onSnowInfoChanged( ResortSnowInfo[] newSnowInfo );
    	void onErrorObtainingSnowInfo( String userVisibleError );
    }
    
    synchronized public void registerListener( SnowInfoListener listener ) {
    	mListener = listener;
    	mListener.onSnowInfoChanged(mLastSnowInfo);
    }
    
    synchronized public void unregisterListener( SnowInfoListener listener ) {
    	mListener = null;
    }
    
    synchronized private void notifyResortSnowInfoChange( ResortSnowInfo[] lastSnowInfo) {
    	if( mListener != null ) {
    		mListener.onSnowInfoChanged(lastSnowInfo);
    	}
    	mLastSnowInfo = lastSnowInfo;
    }
    
	/**
	 * @return true if the alarm should fire based on preferences and current snow settings
	 */
	private boolean checkAlarmAction() {
		boolean alarmAction = false;
		SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SnowSettingsSharedPreference snowSettings = new SnowSettingsSharedPreference();
		if( snowSettings.setFromPreferences(prefs) ) {
			ResortManager resortManager = ResortManager.getInstance(getApplicationContext());
			Resort [] wakeupEnabledResorts = resortManager.getWakeupEnabledResorts();
			if( wakeupEnabledResorts.length > 0 ) {
				SkiReportManager skiReportMgr = new SkiReportManager(getApplicationContext(),
						(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE));
				ResortSnowInfo[] resortSnowInfoList = skiReportMgr.getSnowInfo(wakeupEnabledResorts);
				for( ResortSnowInfo snowInfo : resortSnowInfoList ) {
					if( snowInfo.exceedsOrMatchesPreference(snowSettings)) {
						alarmAction = true;
					} else { 
						Log.d(TAG,"Resort" + snowInfo.toString() + " did not exceed preference " + snowSettings );
					}
				}
				notifyResortSnowInfoChange(resortSnowInfoList);
			} else {
				Log.d(TAG,"no resorts enabled, skipping resort check");
			}
			
		} else {
			Log.e(TAG,"snow settings not found, skipping resort check");
		}
		return alarmAction;
	}
	
	/**
	 * Calculates when the next alarm should occur based on shared prefs
	 * @return a calendar object representing the next alarm to fire, or null if no alarm should be scheduled
	 */
	private Calendar getNextAlarm() {
		Calendar nextAlarm = null;
		// TODO: read enable, enable alarm if necessary
		SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if( prefs == null ) {
			Log.w(TAG,"Unable to get shared preferences, no alarm scheduled");
			return null;
		}
		
		if( prefs.getBoolean(WakeMeSkiPreferences.ALARM_ENABLE_PREF_KEY, false) ) {
			
			RepeatDaySharedPreference repeatDay = new RepeatDaySharedPreference();
			if( repeatDay.setFromPersistString(prefs.getString(
											WakeMeSkiPreferences.REPEAT_DAYS_PREF_KEY, null)) ) {
				TimeSettingsSharedPreference timeSettings = new TimeSettingsSharedPreference();
				if( timeSettings.setTimeFromPersistString(prefs.getString(WakeMeSkiPreferences.ALARM_WAKEUP_TIME_PREF_KEY, null))) {
					AlarmCalculator calculator = new AlarmCalculator(repeatDay,timeSettings);
					nextAlarm = calculator.getNextAlarm();
					if( nextAlarm == null ) {
						Log.d(TAG,"Alarm caculator returnned null, no alarm scheduled");
					}
				} else {
					Log.d(TAG,"No time set, no alarm scheduled");
				}
			} else {
				Log.d(TAG,"No repeat day setting, no alarm scheduled");
			}
		} else {
			Log.d(TAG,"Alarm is disabled, no alarm scheduled");
		}
		return nextAlarm;
	}
	
	@Override
	public void onHandleIntent(Intent intent) {
		boolean alarmFired = false;
		
		mCurrentAction = intent.getAction();
		
		if( mCurrentAction == null ) {
			Log.e(TAG, "Error - null intent action passed");
			AlarmAlertWakeLock.release(); // bad intent, release wake lock
			return;
		}
		if( mCurrentAction.equals(ACTION_DASHBOARD_POPULATE) ) {
			checkAlarmAction();
			AlarmAlertWakeLock.release();	// this wasn't a wakeup, just a check to populate teh dashboard
		}
		else {
			
			if( mCurrentAction.equals(ACTION_WAKE_CHECK)) {
				if( checkAlarmAction() ) {
					AlarmController alarmController = new AlarmController(this.getApplicationContext());
					alarmController.fireAlarm();
					alarmFired = true;
				}
			} else if ( mCurrentAction.equals(ACTION_ALARM_SCHEDULE) ) {
				Calendar nextAlarm = getNextAlarm();
				if( nextAlarm != null ) {
					AlarmController alarmController = new AlarmController(this.getApplicationContext());
					alarmController.setAlarm(nextAlarm);
				}
			} else if ( mCurrentAction.equals(OnAlarmReceiver.ACTION_SNOOZE)) {
				AlarmController alarmController = new AlarmController(this.getApplicationContext());
				alarmController.fireAlarm();
				alarmFired = true;
			} 
			if( !alarmFired ) {
				AlarmAlertWakeLock.release();
			}
		}

	}
	


}
