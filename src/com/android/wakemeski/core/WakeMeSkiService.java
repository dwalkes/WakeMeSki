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
package com.android.wakemeski.core;

import java.util.Calendar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.wakemeski.pref.RepeatDaySharedPreference;
import com.android.wakemeski.pref.SnowSettingsSharedPreference;
import com.android.wakemeski.pref.TimeSettingsSharedPreference;
import com.android.wakemeski.ui.AlarmCalculator;
import com.android.wakemeski.ui.AlarmController;
import com.android.wakemeski.ui.OnAlarmReceiver;
import com.android.wakemeski.ui.WakeMeSkiPreferences;
import com.android.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

/**
 * A service which runs in the background to obtain resort information for the
 * purposes of populating a dashboard with current resort status or starting an
 * alarm in the case a wakeup condition has occurred.
 * 
 * @author dan
 * 
 */
public class WakeMeSkiService extends Service {

	private static final String TAG = "WakeMeSkiService";
	public static final String ACTION_WAKE_CHECK = "com.dwalkes.android.wakemeski.ACTION_WAKE_CHECK";
	public static final String ACTION_ALARM_SCHEDULE = "com.walkes.android.wakemeski.ACTION_ALARM_SCHEDULE";
	public static final String ACTION_DASHBOARD_POPULATE = "com.walkes.android.wakemeski.ACTION_DASHBOARD_POPULATE";

	private SnowSettingsSharedPreference	mSnowSettings = null;
	private SharedPreferences			 	mSharedPreferences = null;
	private AlarmController				 	mAlarmController=null;
	private int mActiveStartId;
	/**
	 * Handle events in the service thread
	 */
	Handler h = new Handler();
	
	@Override
	public IBinder onBind( Intent intent ) {
		return null;
	}
	
	/**
	 * A report listener class which receives notifications on new
	 * reports from ReportController
	 */
	private ReportListener mReportListener = new ReportListener() {


		@Override
		public void onAdded(final Report r) {
			h.post(new Runnable() {
				public void run() {
					onReportAdded(r);
				}
			});
		}

		@Override
		public void onRemoved(Report r) {	
		}

		@Override
		public void onLoading(final boolean started) {
			h.post(new Runnable() {
				public void run() {
					onReportLoading(started);
				}
			});
		}
	};
	
	/**
	 * Called in the service thread when the ReportListener onAdded() callback occurs
	 * and mDoAlarmActionCheck is true
	 * @param r The report added to the list of reports
	 */
	private void onReportAdded(Report r) {
		if( r.getResort().isWakeupEnabled() ) {
			if (r.meetsPreference(getSnowSettings())) {
				Log.i(TAG, "Resort " + r.getResort() + " met preference "
						+ getSnowSettings());
				/**
				 * Done listening now that we've fired the alarm
				 */
				if( ReportController.getInstance(null).removeListener(mReportListener) ) {
					/**
					 * Only fire the alarm once.  By checking for removeListener = true we know
					 * that this was the first case and not a queue'd handler thread action
					 * Don't release the wake lock, we will do that when the alarm activity starts
					 */
					getAlarmController().fireAlarm();
					Log.d(TAG, "Found wakeup resort, stopping service");
					stopSelf(mActiveStartId);
				}
			} else {
				Log.d(TAG, "Resort " + r.getResort() + " did not meet preference "
						+ getSnowSettings());
			}
		} else {
			Log.d(TAG, "Resort " + r.getResort() + " is not wakeup enabled");
		}
	}
	
	/**
	 * Called in the service thread when ReportListener onAdded() callback occurs and
	 * mDoAlarmActionCheck is true
	 * @param started true when the report loading has started, false when completed
	 */
	private void onReportLoading(final boolean started) {
		if( started == true ) {
			/**
			 * Force a reload of snow settings when the report loading starts
			 */
			mSnowSettings = null;	
		} else {
			Log.d(TAG, "Report load completed, stopping service");
			/**
			 * Done listening now that report load has completed
			 * Note: missing logic to determine whether the listener was active for at least
			 * one start + one stop.  This means we could potentially just catch the end of
			 * a loadReport() started by another thread.  This seems unlikely enough that we should
			 * be safe to ignore it.
			 */
			ReportController.getInstance(null).removeListener(mReportListener);
			/**
			 * If the alarm didn't fire and report load has complete, release the wake lock and
			 * stop the service.  Nothing else to do at this time
			 */
			AlarmAlertWakeLock.release();
			stopSelf(mActiveStartId);
		}
	}
	
	
	/**
	 * @return An alarm controller instance used by the service
	 */
	private AlarmController getAlarmController() {
		if( mAlarmController == null ) {
			mAlarmController = new AlarmController(this
				.getApplicationContext());
		}
		return mAlarmController;
	}
	
	private SharedPreferences getSharedPreferences() {
		if( mSharedPreferences == null ) {
			Context ctx = getApplicationContext();
			mSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences;
	}
	
	private SnowSettingsSharedPreference getSnowSettings() {

		if( mSnowSettings == null ) {
			mSnowSettings = new SnowSettingsSharedPreference();
			// update snow settings based on current preferences
			if( !mSnowSettings.setFromPreferences(getSharedPreferences()) ) {
				Log.e(TAG, "snow settings not found");
			}
		}

		return mSnowSettings;
	}
		
	@Override
	public void onCreate() {
		// Maintain a lock during the checking of the alarm. This lock may have
		// already been acquired in AlarmReceiver. If the process was killed,
		// the global wake lock is gone. Acquire again just to be sure.
		AlarmAlertWakeLock.acquireCpuWakeLock(this.getApplicationContext());
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		/**
		 * Make sure our listener is removed before destroy.  Should be safe to call
		 * even if the listener is not currently in the list
		 */
		ReportController.getInstance(null).removeListener(mReportListener);
		super.onDestroy();
	}
	/**
	 * Checks whether an alarm action needs to occur based on stored preferences.
	 * Kicks off report load using ReportController.  onReportAdded() and onReportLoading() will
	 * take care of handling the cases where an alarm action is or is not needed
	 */
	private void checkAlarmAction() {
		/**
		 * Add our listener here - note in the odd case where we've already added a listener
		 * and have not removed it (two checks in a row occuring before first load completed) it should
		 * still be safe to call addListener - the second add will be a no-op and the listener will
		 * be removed when the first loadReports() completes
		 */
		ReportController.getInstance(null).addListener(mReportListener);
		ReportController.getInstance(null).loadReports();
	}

	/**
	 * Calculates when the next alarm should occur based on shared prefs
	 * 
	 * @return a calendar object representing the next alarm to fire, or null if
	 *         no alarm should be scheduled
	 */
	private Calendar getNextAlarm() {
		Calendar nextAlarm = null;
		SharedPreferences prefs =getSharedPreferences();

		if (prefs.getBoolean(WakeMeSkiPreferences.ALARM_ENABLE_PREF_KEY, false)) {

			RepeatDaySharedPreference repeatDay = new RepeatDaySharedPreference();
			if (repeatDay.setFromPersistString(prefs.getString(
					WakeMeSkiPreferences.REPEAT_DAYS_PREF_KEY, null))) {
				TimeSettingsSharedPreference timeSettings = new TimeSettingsSharedPreference();
				if (timeSettings.setTimeFromPersistString(prefs.getString(
						WakeMeSkiPreferences.ALARM_WAKEUP_TIME_PREF_KEY, null))) {
					AlarmCalculator calculator = new AlarmCalculator(repeatDay,
							timeSettings);
					nextAlarm = calculator.getNextAlarm();
					if (nextAlarm == null) {
						Log
								.d(TAG,
										"Alarm caculator returned null, no alarm scheduled");
					}
				} else {
					Log.d(TAG, "No time set, no alarm scheduled");
				}
			} else {
				Log.d(TAG, "No repeat day setting, no alarm scheduled");
			}
		} else {
			Log.d(TAG, "Alarm is disabled, no alarm scheduled");
		}
		return nextAlarm;
	}

	private void scheduleNextAlarm() {
		Calendar nextAlarm = getNextAlarm();
		if (nextAlarm != null) {
			getAlarmController().setAlarm(nextAlarm);
		}
	}

	public void onHandleIntent(Intent intent, int startId) {
		boolean releaseWakeLock = true;
		boolean stopService = true;
		String 	currentAction;
		currentAction = intent.getAction();

		if (currentAction == null) {
			Log.e(TAG, "Error - null intent action passed");
		}
		else if (currentAction.equals(ACTION_WAKE_CHECK)) {
			checkAlarmAction();
			/*
			 * We will release wake lock when check completes
			 */
			releaseWakeLock = false;
			/*
			 * Don't stop the service, we will stop it after resorts are checked
			 */
			stopService = false;


		} else if (currentAction.equals(ACTION_ALARM_SCHEDULE)) {
			// alarm will be scheduled below
		} else if (currentAction.equals(OnAlarmReceiver.ACTION_SNOOZE)) {
			getAlarmController().fireAlarm();
			// we will release wake lock in the alarm controller
			releaseWakeLock = false;
		}
		
		/*
		 * We always need to schedule the next alarm after a check or snooze event
		 * Otherwise we won't get a new wakeup on the next configured check day
		 */
		scheduleNextAlarm();
		
		if (releaseWakeLock) {
			AlarmAlertWakeLock.release();
		}
		
		if(stopService) {
			stopSelfResult(startId);
		} else {
			/**
			 * Save the active start ID so we can use it to eventually stop the service
			 * when we are done
			 */
			mActiveStartId = startId;
		}
	}

	@Override
	public void onStart( final Intent intent, final int startId ) {
		h.post(new Runnable() {
			public void run() {
				onHandleIntent(intent,startId);
			}
		});
	}
	
}
