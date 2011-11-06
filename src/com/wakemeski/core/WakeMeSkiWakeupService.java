/*
 * Copyright (C) 2010 Dan Walkes, Andy Doan
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
import android.content.Intent;
import android.content.SharedPreferences;

import com.wakemeski.Log;
import com.wakemeski.WakeMeSki;
import com.wakemeski.pref.RepeatDaySharedPreference;
import com.wakemeski.pref.SnowSettingsSharedPreference;
import com.wakemeski.pref.TimeSettingsSharedPreference;
import com.wakemeski.ui.AlarmCalculator;
import com.wakemeski.ui.AlarmController;
import com.wakemeski.ui.OnAlarmReceiver;
import com.wakemeski.ui.WakeMeSkiPreferences;

/**
 * Extends WakeMeSkiAlertService to provide wakeup related service functionality
 * including scheduling wakeup alarms and firing an alarm when snowfall exceeds
 * wakeup preference
 * @author dan
 */
public class WakeMeSkiWakeupService extends WakeMeSkiAlertService {

	private	SnowSettingsSharedPreference mSnowSettings;
	private boolean mAlarmFired;
	private AlarmController				 	mAlarmController=null;
	private ForegroundServiceCompat			mForegroundService;
	private static final int				NOTIFY_CHECK_STATUS_ID=1;
	public static final String ACTION_WAKE_CHECK = "com.wakemeski.core.ACTION_WAKE_CHECK";
	public static final String ACTION_SHUTDOWN = "com.wakemeski.core.ACTION_SHUTDOWN";
	public static final String EXTRA_ALARM_INTENT_BROADCAST_RECEIVER_TIME ="com.wakemeski.core.BCAST_RECEIVER_TIME_EXTRA";


	/**
	 * Clear mAlarmFired when the report load starts
	 */
	@Override
	protected void reportLoadStarted() {
		mAlarmFired = false;
	}

	/**
	 * The service should only stop on report load complete if the alarm did not
	 * fire.
	 */
	@Override
	protected boolean shouldStopOnReportLoadComplete() {
		if( !mAlarmFired ) {
			Log.d("Alarm did not fire, stopping service");
			return true;
		}
		/**
		 * Else if the alarm fired the Alarm class will shutdown the service when it
		 * has completed running.  Shutting down here could create a race condition between
		 * lock acquire on the alarm application and shutdown of the service.
		 */
		return super.shouldStopOnReportLoadComplete();
	}

	/**
	 * Gets the wakeup snow settings preference instead of the notification snow
	 * settings preference for use with the wakeup service
	 */
	@Override
	protected SnowSettingsSharedPreference getSnowSettings() {

		if( mSnowSettings == null ) {
			mSnowSettings = SnowSettingsSharedPreference.getWakeupPreference();
			// update snow settings based on current preferences
			if( !mSnowSettings.setFromPreferences(getSharedPreferences()) )
				Log.e("snow settings not found");
		}

		return mSnowSettings;
	}

	/**
	 * Overrides base implementation to check for exceeded preference and fire alarms
	 * as/if necessary
	 */
	@Override
	protected void onReportAdded(Report r)
	{
		super.onReportAdded(r);
		if( r.getResort().isWakeupEnabled() ) {
			if (r.meetsPreference(getSnowSettings())) {
				Log.i("Resort " + r.getResort() + " met preference " + getSnowSettings());
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
				Log.d("Resort " + r.getResort() + " did not meet preference "
						+ getSnowSettings());
			}
		} else {
			Log.d("Resort " + r.getResort() + " is not wakeup enabled");
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
					if (nextAlarm == null)
						Log.d("Alarm caculator returned null, no alarm scheduled");
				} else {
					Log.d("No time set, no alarm scheduled");
				}
			} else {
				Log.d("No repeat day setting, no alarm scheduled");
			}
		} else {
			Log.d("Alarm is disabled, no alarm scheduled");
		}
		return nextAlarm;
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

		startReportLoad();

	}



	/**
	 * The next wakup alarm must always be scheduled since this is a RTC wakeup
	 */
	private void scheduleNextAlarm() {
		Calendar nextAlarm = getNextAlarm();
		if (nextAlarm != null) {
			getAlarmController().setAlarm(nextAlarm);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mForegroundService = new ForegroundServiceCompat(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		/*
		 * Remove foreground notification
		 */
		mForegroundService.stopForegroundCompat(NOTIFY_CHECK_STATUS_ID);
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
	private boolean isStaleIntent( Intent intent ) {
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
				Log.d("isStaleIntent " + isStale + " now= " + new Date(now.getTimeInMillis()) +
						" bcReceiverCalPlus5Minutes= " + new Date(bcReceiverCalPlus5Minutes.getTimeInMillis()));
			}
		}
		return isStale;
	}

	/**
	 * Handle intents issued for the WakeMeSkiWakeupService
	 * @param intent
	 * @param startId
	 * @return
	 */
	@Override
	protected IntentHandlerResult onHandleIntent(Intent intent) {
		String 	currentAction = null;
		boolean staleIntent = false;
		/*
		 * By default don't try to fire this intent again if we are killed after
		 * onStart()
		 */
		IntentHandlerResult result = super.onHandleIntent(intent);

		currentAction = intent.getAction();

		if (currentAction == null) {
			Log.w("onHandleIntent with null intent or action");
		}
		else {
			staleIntent = isStaleIntent(intent);
			if (currentAction.equals(ACTION_WAKE_CHECK)) {
				if( !staleIntent ) {
					Log.d("Starting new wake check");
					checkAlarmAction();
					/*
					 * Don't stop the service, we will stop it after resorts are checked
					 */
					result.setStopService(false);
					/*
					 * If for some reason we are killed before we complete processing,
					 * re-start with the same intent.  We will figure out if it's
					 * stale above.
					 */
					result.setStartState(START_REDELIVER_INTENT);
				} else {
					Log.w("Stale intent received with ACTION_WAKE_CHECK, ignoring");
				}
			} else if (currentAction.equals(ACTION_ALARM_SCHEDULE)) {
				// alarm will be scheduled below, even on stale intents
			} else if (currentAction.equals(OnAlarmReceiver.ACTION_SNOOZE)) {
				if( !staleIntent ) {
					getAlarmController().fireAlarm();
					// we will release wake lock in the alarm controller
					result.setStopService(false);
					/*
					 * Don't bother with redeliver intent in this case.  Since
					 * there's no background processing happening it shouldn't be an issue.
					 */
				} else {
					Log.w("Stale intent received with ACTION_SNOOZE, ignoring");
				}
			} else {
				Log.d("Unhandled action " + currentAction);
			}

		}
		/*
		 * We always need to schedule the next alarm after a check or snooze event
		 * Otherwise we won't get a new wakeup on the next configured check day
		 */
		scheduleNextAlarm();

		return result;
	}
}
