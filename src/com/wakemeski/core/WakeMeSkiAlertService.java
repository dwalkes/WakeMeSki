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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.wakemeski.core.alert.AlertPollingController;
import com.wakemeski.pref.SnowSettingsSharedPreference;
import com.wakemeski.ui.WakeMeSkiPreferences;
import com.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

/**
 * A service to manage alert notifications.  This service checks snow
 * alerts only and does not set or handle wakeup conditions.  To handle both
 * wakeup and alert notifications, see WakeMeSkiWakeupService
 * @author dan
 * 
 */
public class WakeMeSkiAlertService extends Service {

	private static final String TAG = "WakeMeSkiAlertService";

	private SnowSettingsSharedPreference	mSnowSettings = null;
	private SharedPreferences			 	mSharedPreferences = null;
	private ReportController				mReportController;

	/*
	 * Use this action to schedule alarms for both wakeup and alerts
	 */
	public static final String ACTION_ALARM_SCHEDULE = "com.wakemeski.core.ACTION_ALARM_SCHEDULE";
	public static final String ACTION_ALERT_CHECK = "com.wakemeski.core.ACTION_ALERT_CHECK";

	/**
	 * A class to hold results from the intent handler, used to determine whether to
	 * stop the service or whether to request a re-start if the process is killed before
	 * completing
	 */
	protected class IntentHandlerResult {
		private boolean 	mStopService;
		/*
		 * Value to return from onStartCommand for API level 5 and
		 * above, determining whether a kill of this process requests a re-start
	 	 * with the same intent or not to bother anymore.
		 */
		private int			mStartState; 
		public IntentHandlerResult( boolean stopService, int startState ) {
			mStopService = stopService;
			mStartState = startState;
		}
		public boolean isStopService() {
			return mStopService;
		}
		public void setStopService(boolean stopService) {
			mStopService = stopService;
		}
		public int getStartState() {
			return mStartState;
		}
		public void setStartState(int startState) {
			mStartState = startState;
		}

	}
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
	 * @param r The report added to the list of reports
	 */
	protected void onReportAdded(Report r) {
	}

	/**
	 * Called when report loading starts
	 */
	protected void reportLoadStarted()
	{
		Log.d(TAG, "Report load started");
		/**
		 * Force a reload of snow settings when the report loading starts
		 */
		mSnowSettings = null;	
	}
	
	/**
	 * Called when report load is complete to determine whether the service should stop
	 * or if it should remain running and wait for something else to stop it.
	 * @return true when this class has nothing more to do after report loading completes
	 */
	protected boolean shouldStopOnReportLoadComplete() {
		return true;
	}
	
	/**
	 * Called when report loading completes
	 */
	private void reportLoadComplete()
	{
		Log.d(TAG, "Report load completed");
		/**
		 * Done listening now that report load has completed
		 * Note: missing logic to determine whether the listener was active for at least
		 * one start + one stop.  This means we could potentially just catch the end of
		 * a loadReport() started by another thread.  This seems unlikely enough that we should
		 * be safe to ignore it.
		 */
		mReportController.removeListener(mReportListener);
		if( shouldStopOnReportLoadComplete() ) {
			stopSelf();
		}
	}
	
	/**
	 * Called in the service thread when ReportListener onAdded() callback occurs and
	 * mDoAlarmActionCheck is true
	 * @param started true when the report loading has started, false when completed
	 */
	private void onReportLoading(final boolean started) {
		if( started == true ) {
			reportLoadStarted();
		} else {
			reportLoadComplete();
		}
	}
	
	protected SharedPreferences getSharedPreferences() {
		if( mSharedPreferences == null ) {
			Context ctx = getApplicationContext();
			mSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences;
	}
	
	protected SnowSettingsSharedPreference getSnowSettings() {

		if( mSnowSettings == null ) {
			mSnowSettings = SnowSettingsSharedPreference.getNotifyPreference();
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

		super.onDestroy();
	}
	
	/**
	 * Start loading reports - will start a background process to complete the load,
	 * calling reportLoadComplete() when completed
	 */
	protected void startReportLoad() {
		Log.d(TAG, "Start report load");
		/**
		 * Add our listener here - note in the odd case where we've already added a listener
		 * and have not removed it (two checks in a row occurring before first load completed) it should
		 * still be safe to call addListener - the second add will be a no-op and the listener will
		 * be removed when the first loadReports() completes
		 *
		 * Must load reports with default (non background) priority.  See <a href="https://github.com/dwalkes/WakeMeSki/issues/#issue/20">
		 * issue 20</a>
		 *
		 */
		mReportController.addListenerAndUpdateReports(mReportListener,false);
	}
	


	
	/**
	 * Handles the intent from onStart or onStartCommand originally sent by a broadcast
	 * reciever to do some background action.  Override this in a derived class
	 * but call this method from your overrided method to handle any common intent actions
	 * @param intent to handle
	 */
	protected IntentHandlerResult onHandleIntent(Intent intent) {

		IntentHandlerResult result = new IntentHandlerResult(true,START_NOT_STICKY);
		String 	currentAction = null;

		currentAction = intent.getAction();
		
		if( currentAction != null ) {
			Log.d(TAG,"onHandleIntent currentAction " + currentAction );
			if( currentAction.equals(ACTION_ALARM_SCHEDULE) ) {
				Log.d(TAG, "Checking notify enable to schedule wakeup checks");
				if( WakeMeSkiPreferences.isAlertNotificationEnabled(getSharedPreferences()) ) {
					AlertPollingController.getInstance(getApplicationContext()).enableAlertPolling();
				}
			} else if( currentAction.equals(ACTION_ALERT_CHECK) ) {
				startReportLoad();
				/*
				 * We will stop the service when report load completes
				 */
				result.setStopService(false);
			}
		} else {
			Log.w(TAG, "onHandleIntent with null intent or action");
		}
		
		return result;
	}
	
	/**
	 * Calls onHandleIntent() to handle this intent and determine result.
	 * @param intent to handle
	 * @param startId ID of this started service instance
	 * @return Value to return from onStartCommand for API level 5 and
	 * above, determining whether a kill of this process requests a re-start
	 * with the same intent or not to bother anymore.
	 */
	private int onHandleIntentReturnStartState(Intent intent, int startId) {
		if( intent == null ) {
			return START_NOT_STICKY;
		}
		IntentHandlerResult result = onHandleIntent(intent);
		if (result.isStopService()) {
			stopSelf(startId);
		}
		return result.getStartState();
	}
	/**
	 * Start method for 2.0 and above
	 */
	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		Log.d(TAG, "onStartCommand");
		int startedState;
		startedState = onHandleIntentReturnStartState(intent,startId);
		return startedState;

	}
	
	/**
	 * Start method for 1.6 and below
	 */
	@Override
	public void onStart( final Intent intent, final int startId ) {
		Log.d(TAG, "onStart");
		onHandleIntentReturnStartState(intent,startId);
	}
	


}
