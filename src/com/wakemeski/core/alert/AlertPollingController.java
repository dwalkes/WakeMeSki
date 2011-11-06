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
package com.wakemeski.core.alert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.wakemeski.Log;
import com.wakemeski.ui.OnAlarmReceiver;

/**
 * A class to control the turn on/off of polling for snow alerts using
 * the android AlarmManager
 */
public class AlertPollingController {

	Context mContext;

	private static AlertPollingController mInstance = null;

	private AlertPollingController(Context c) {
		mContext = c;
	}

	public static synchronized AlertPollingController getInstance(Context c) {
		if( mInstance == null ) {
			mInstance = new AlertPollingController(c);
		}
		return mInstance;
	}

	/**
	 * @param intentString
	 * @return the pending intent used to start an alert wakeup
	 */
	protected PendingIntent getPendingIntent() {
		PendingIntent pi = null;
		Intent i = new Intent(OnAlarmReceiver.ACTION_ALERT_CHECK, null, mContext,
				OnAlarmReceiver.class);
		pi = PendingIntent.getBroadcast(mContext, 0, i, 0);
		return pi;
	}

	/**
	 * Enable the periodic wakeup check for alerts
	 */
	public void enableAlertPolling() {
		Log.d("Enabling alert polling");
		AlarmManager mgr = (AlarmManager) mContext
								.getSystemService(Context.ALARM_SERVICE);
		mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
								0, AlarmManager.INTERVAL_HOUR, getPendingIntent());
	}

	/**
	 * Disable wakeup checks for snow alerts
	 */
	public void disableAlertPolling() {
		Log.d("Disabling alert polling");
		AlarmManager mgr = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);

		mgr.cancel(getPendingIntent());
	}
}
