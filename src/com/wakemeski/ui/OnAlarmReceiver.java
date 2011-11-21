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

import android.content.Context;
import android.content.Intent;

import com.wakemeski.Log;
import com.wakemeski.core.WakeMeSkiAlertService;
import com.wakemeski.core.WakeMeSkiWakeupService;
import com.wakemeski.generic_deskclock.AlarmAlertWakeLock;
import com.wakemeski.generic_deskclock.AlarmReceiver;
import com.wakemeski.generic_deskclock.GenericDeskClockCustomization;

/**
 * This class is invoked when an alarm check needs to occur based on a
 * configured wakeup or when we need to check for new snow alerts.
 * 
 * @author dan
 *
 */

public class OnAlarmReceiver extends AlarmReceiver {
	public static final String ACTION_WAKE_CHECK = WakeMeSkiWakeupService.ACTION_WAKE_CHECK;
	public static final String ACTION_ALERT_CHECK = WakeMeSkiWakeupService.ACTION_ALERT_CHECK;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			if ( intent.getAction().equals(ACTION_WAKE_CHECK) ||
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
				if( intent.getAction().equals(ACTION_WAKE_CHECK) ) {
					/*
					 * Add an extra with the intent that will actually fire the alarm, by removing the class and
					 * setting alarm alert action as the action
					 */
					Intent alarmFireIntent = GenericDeskClockCustomization.getInstance().getShowAlertIntent(context);
					alarmFireIntent.putExtras(intent);
					i.putExtra(WakeMeSkiWakeupService.EXTRA_PENDING_ALARM_INTENT,
							alarmFireIntent);
				}
				context.startService(i);
			} else {
				Log.w("Unknown wake action " + intent.getAction());
			}
		} else {
			Log.w("Null wake action");
		}
	}

}
