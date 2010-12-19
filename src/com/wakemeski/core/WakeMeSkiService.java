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
package com.wakemeski.core;

import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.wakemeski.WakeMeSki;
import com.wakemeski.pref.RepeatDaySharedPreference;
import com.wakemeski.pref.SnowSettingsSharedPreference;
import com.wakemeski.pref.TimeSettingsSharedPreference;
import com.wakemeski.ui.AlarmCalculator;
import com.wakemeski.ui.AlarmController;
import com.wakemeski.ui.OnAlarmReceiver;
import com.wakemeski.ui.WakeMeSkiPreferences;
import com.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

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
	public static final String ACTION_WAKE_CHECK = "com.wakemeski.core.ACTION_WAKE_CHECK";
	public static final String ACTION_ALARM_SCHEDULE = "com.wakemeski.core.ACTION_ALARM_SCHEDULE";
	public static final String ACTION_SHUTDOWN = "com.wakemeski.core.ACTION_SHUTDOWN";
	public static final String EXTRA_ALARM_INTENT_BROADCAST_RECEIVER_TIME ="com.wakemeski.core.BCAST_RECEIVER_TIME_EXTRA";

	private SnowSettingsSharedPreference	mSnowSettings = null;
	private SharedPreferences			 	mSharedPreferences = null;
	private AlarmController				 	mAlarmController=null;
	private ReportController				mReportController;
	private boolean							mAlarmFired=false;
	private ForegroundServiceCompat			mForegroundService;
	private static final int				NOTIFY_CHECK_STATUS_ID=1;

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
		
		public void onBusy(boolean busy) {
			
		}
	};
	
	/**
	 * Called in the service thread when the ReportListener onAdded() callback occurs
	 * and mDoAlarmActionCheck is true
	 * @param r The report added to the list of reports
	 */
	private void onReportAdded(Report r) {
		Log.d(TAG, "Report added " + r);
		if( r.getResort().isWakeupEnabled() ) {
			if (r.meetsPreference(getSnowSettings())) {
				Log.i(TAG, "Resort " + r.getResort() + " met preference "
						+ getSnowSettings());
				/**
				 * Done listening now that we've fired the alarm
				 */
				if( mAlarmFired == false ) {
					/**
					 * Only fire the alarm once
					 * Don't release the wake lock, we will do that when the alarm activity starts
					 */
					getAlarmController().fireAlarm();
					mAlarmFired = true;
					
					/*
					 * Not safe to stop the service here because the 
					 * alarm activity has not yet started. Will be stopped by the alarm
					 * activity.
					 */
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
			Log.d(TAG, "Report load started");
			/**
			 * Force a reload of snow settings when the report loading starts
			 */
			mSnowSettings = null;	
			mAlarmFired = false;
		} else {
			Log.d(TAG, "Report load completed");
			/**
			 * Done listening now that report load has completed
			 * Note: missing logic to determine whether the listener was active for at least
			 * one start + one stop.  This means we could potentially just catch the end of
			 * a loadReport() started by another thread.  This seems unlikely enough that we should
			 * be safe to ignore it.
			 */
			mReportController.removeListener(mReportListener);
			
			if( !mAlarmFired ) {
				Log.d(TAG, "Alarm did not fire, stopping service");
				stopSelf();
			}
			/**
			 * Else if the alarm fired the Alarm class will shutdown the service when it
			 * has completed running.  Shutting down here could create a race condition between
			 * lock acquire on the alarm application and shutdown of the service.
			 */

			/*
			 * Remove foreground notification if present
			 */
			mForegroundService.stopForegroundCompat(NOTIFY_CHECK_STATUS_ID);
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
			mSnowSettings = SnowSettingsSharedPreference.newWakeupPreference();
			// update snow settings based on current preferences
			if( !mSnowSettings.setFromPreferences(getSharedPreferences()) ) {
				Log.e(TAG, "snow settings not found");
			}
		}

		return mSnowSettings;
	}
		
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		Context c = this.getApplicationContext();
		// Maintain a lock during the checking of the alarm. This lock may have
		// already been acquired in AlarmReceiver. If the process was killed,
		// the global wake lock is gone. Acquire again just to be sure.
		AlarmAlertWakeLock.acquireCpuWakeLock(c);
		mReportController = WakeMeSkiFactory.getInstance(c).getReportController();
		mForegroundService = new ForegroundServiceCompat(this);

		super.onCreate();
	}
	

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		/**
		 * Make sure our listener is removed before destroy.  Should be safe to call
		 * even if the listener is not currently in the list
		 */
		mReportController.removeListener(mReportListener);
		AlarmAlertWakeLock.release();
		/*
		 * Remove foreground notification
		 */
		mForegroundService.stopForegroundCompat(NOTIFY_CHECK_STATUS_ID);
		super.onDestroy();
	}
	/**
	 * Checks whether an alarm action needs to occur based on stored preferences.
	 * Kicks off report load using ReportController.  onReportAdded() and onReportLoading() will
	 * take care of handling the cases where an alarm action is or is not needed
	 */
	private void checkAlarmAction() {
		
		/**
		 * I noticed problems shortly after the 2.0 release running in the background.
		 * In an attempt to resolve I added setForeground() to make it less likely that the
		 * process will be killed.  I figured out ultimately that the problem was 
		 * the process priority in ReportController (THREAD_PRIORITY_BAKGROUND.)  Setting this
		 * priority on the process when the service was the only thing running in the process
		 * caused the process to be killed immediately in low memory conditions regardless of
		 * foreground notifications.
		 * I had this code implemented by the time I figured it out, so I figured it didn't
		 * hurt to keep it in.  However if we ever suspect trouble with the foreground 
		 * setting or associated notification we should be able to kill this code below.
		 */
		Notification notification = new Notification(android.R.drawable.stat_notify_sync_noanim,
												getString(com.wakemeski.R.string.wakemeski_update),
												System.currentTimeMillis());
		
		Intent notificationIntent = new Intent(this,WakeMeSki.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(getApplicationContext(), 
									getString(com.wakemeski.R.string.wakemeski_update),
									getString(com.wakemeski.R.string.checking_notify_text), 
									contentIntent);
		
		mForegroundService.startForegroundCompat(NOTIFY_CHECK_STATUS_ID, notification);
		
												
		/**
		 * Add our listener here - note in the odd case where we've already added a listener
		 * and have not removed it (two checks in a row occurring before first load completed) it should
		 * still be safe to call addListener - the second add will be a no-op and the listener will
		 * be removed when the first loadReports() completes
		 */
		mReportController.addListener(mReportListener);
		/**
		 * Must load reports with default (non background) priority.  See <a href="https://github.com/dwalkes/WakeMeSki/issues/#issue/20">
		 * issue 20</a>
		 */
		mReportController.loadReports(false);
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
				TimeSettingsSharedPreference timeSettings = new TimeSettingsSharedPreference(this.getApplicationContext());
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

	/**
	 * With API 5 and higher onStartCommand() is used to start the service.
	 * We have the option to re-deliver an intent (with START_REDELIVER_INTENT) if
	 * our service is killed when processing.  The problem with doing this indefinitely
	 * would be if we can't run for 2 hours, then finally can run and this means we fire
	 * the alarm 2 hours after we were supposed to (during a movie, church, etc.)
	 * This function checks to make sure the intent we are looking at was created by the
	 * broadcast receiver within a reasonable time that it's still worth trying to do something
	 * about.  For now this threshold is hard coded to 5 minutes.
	 * @param intent to check
	 * @return true if this intent is stale
	 */
	boolean isStaleIntent( Intent intent ) {
		boolean isStale = false;
		
		if( intent != null && intent.hasExtra(EXTRA_ALARM_INTENT_BROADCAST_RECEIVER_TIME) ) {
			long bcReceiverTime = intent.getExtras().getLong(EXTRA_ALARM_INTENT_BROADCAST_RECEIVER_TIME);
			if( bcReceiverTime != 0 ) {
				Calendar bcReceiverCalPlus5Minutes = Calendar.getInstance();
				bcReceiverCalPlus5Minutes.setTimeInMillis(bcReceiverTime);
				/**
				 * Consider the intent stale if it's currently 
				 * greater than 5 minutes after the intent was created
				 */
				bcReceiverCalPlus5Minutes.add(Calendar.MINUTE, 5);
				Calendar now = Calendar.getInstance();
				isStale = now.after(bcReceiverCalPlus5Minutes);
				Log.d(TAG, "isStaleIntent " + isStale + " now= " + new Date(now.getTimeInMillis()) +
						" bcReceiverCalPlus5Minutes= " + new Date(bcReceiverCalPlus5Minutes.getTimeInMillis()));
			}
		}
		return isStale;
	}
	
	/**
	 * Handles the intent from onStart or onStartCommand originally sent by a broadcast
	 * reciever to do some background action.
	 * @param intent to handle
	 * @param startId ID of this started service instance
	 * @return Value to return from onStartCommand for API level 5 and
	 * above, determining whether a kill of this process requests a re-start
	 * with the same intent or not to bother anymore.
	 * 
	 */
	private int onHandleIntent(Intent intent, int startId) {
		boolean stopService = true;
		String 	currentAction = null;
		boolean staleIntent = false;
		/*
		 * By default don't try to fire this intent again if we are killed after
		 * onStart()
		 */
		int startedState = START_NOT_STICKY;

			
		if( intent != null ) {
			currentAction = intent.getAction();
		}
		
		if (currentAction == null) {
			Log.w(TAG, "onHandleIntent with null intent or action");
		}
		else {
			
			staleIntent = isStaleIntent(intent);
			
			if (currentAction.equals(ACTION_WAKE_CHECK)) {
				if( !staleIntent ) {
					Log.d(TAG,"Starting new wake check");
					checkAlarmAction();
					/*
					 * Don't stop the service, we will stop it after resorts are checked
					 */
					stopService = false;
					/*
					 * If for some reason we are killed before we complete processing,
					 * re-start with the same intent.  We will figure out if it's
					 * stale above.
					 */
					startedState = START_REDELIVER_INTENT;
				} else {
					Log.w(TAG,"Stale intent received with ACTION_WAKE_CHECK, ignoring");
				}
			} else if (currentAction.equals(ACTION_ALARM_SCHEDULE)) {
				// alarm will be scheduled below, even on stale intents
			} else if (currentAction.equals(OnAlarmReceiver.ACTION_SNOOZE)) {
				if( !staleIntent ) {
					getAlarmController().fireAlarm();
					// we will release wake lock in the alarm controller
					stopService = false;
					/*
					 * Don't bother with redeliver intent in this case.  Since
					 * there's no background processing happening it shouldn't be an issue.
					 */
				} else {
					Log.w(TAG,"Stale intent received with ACTION_SNOOZE, ignoring");
				}
			} else {
				Log.d(TAG,"Unhandled action ");
			}
		
		}
		/*
		 * We always need to schedule the next alarm after a check or snooze event
		 * Otherwise we won't get a new wakeup on the next configured check day
		 */
		scheduleNextAlarm();
		
		if (stopService) {
			stopSelf(startId);
		}
		
		return startedState;
	}

	/**
	 * Start method for 2.0 and above
	 */
	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		Log.d(TAG, "onStartCommand");
		int startedState;
		startedState = onHandleIntent(intent,startId);
		return startedState;

	}
	
	/**
	 * Start method for 1.6 and below
	 */
	@Override
	public void onStart( final Intent intent, final int startId ) {
		Log.d(TAG, "onStart");
		onHandleIntent(intent,startId);
	}
	


}
