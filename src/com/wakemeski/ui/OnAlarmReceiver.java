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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wakemeski.Log;
import com.wakemeski.core.WakeMeSkiAlertService;
import com.wakemeski.core.WakeMeSkiWakeupService;
import com.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

/**
 * This class is invoked when an alarm check needs to occur based on a
 * configured wakeup.
 *
 * @author dan
 *
 */
public class OnAlarmReceiver extends BroadcastReceiver {
	public static final String ACTION_WAKE_CHECK = WakeMeSkiWakeupService.ACTION_WAKE_CHECK;
	public static final String ACTION_ALERT_CHECK = WakeMeSkiWakeupService.ACTION_ALERT_CHECK;
	public static final String ACTION_SNOOZE = AlarmController.ACTION_FIRE_ALARM;
	private static final String TAG = "OnAlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_SNOOZE) ||
						intent.getAction().equals(ACTION_WAKE_CHECK) ||
						intent.getAction().equals(ACTION_ALERT_CHECK)) {
				AlarmAlertWakeLock.acquireCpuWakeLock(context);
				/*
				 * Create an intent for this action.  If the action is alert check use
				 * the alert service, otherwise use the wakeup service
				 */
				Intent i = new Intent(
						intent.getAction(), null, context,
						intent.getAction().equals(ACTION_ALERT_CHECK) ?
									WakeMeSkiAlertService.class :
									WakeMeSkiWakeupService.class
								);
				i.putExtra(WakeMeSkiWakeupService.EXTRA_ALARM_INTENT_BROADCAST_RECEIVER_TIME,
						System.currentTimeMillis());
				context.startService(i);
			} else {
				Log.w(TAG, "Unknown wake action " + intent.getAction());
			}
		} else {
			Log.w(TAG, "Null wake action");
		}
	}

}
