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
import android.util.Log;

import com.wakemeski.core.WakeMeSkiWakeupService;
import com.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

/**
 * Catch an upgrade message for our ap... otherwise our alarm will be removed and never
 * rescheduled.
 * 
 * @author dan
 *
 */
public class OnUpgradeReceiver extends BroadcastReceiver {
	static final String TAG = "com.wakemeski.ui.OnUpgradeReceiver";
	@Override
	public void onReceive(Context context, Intent arg1) {
		// TODO Auto-generated method stub
		/*
		 * Note: I couldn't figure out a way to intent filter based on package name.
		 * I ran into the same issue described here:
		 *  http://groups.google.com/group/android-developers/browse_thread/thread/14589d5e0761c056?pli=1
		 * It appears it's not possible to filter based on this data field.
		 * I was not able to figure out how to do so (path/host filters do not work as 
		 * as arg1.getData().getPath() or getHost() both return NULL.)  Therefore this receiver
		 * fires on every install of every application.  Hopefully that won't be
		 * a problem since we just do a quick check for the application name and
		 * exit if no match.
		 */
		if( arg1.getDataString().contains("com.wakemeski")) {
			Log.d(TAG,"Rescheduling alarm after upgrade" );
			AlarmAlertWakeLock.acquireCpuWakeLock(context);
			context.startService(new Intent(WakeMeSkiWakeupService.ACTION_ALARM_SCHEDULE,
					null, context, WakeMeSkiWakeupService.class));
		}
	}

}
