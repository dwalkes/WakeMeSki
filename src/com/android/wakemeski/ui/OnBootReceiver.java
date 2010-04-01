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

import com.android.wakemeski.ui.alarmclock.AlarmAlertWakeLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class catches a boot message.  We need to reschedule our alarms on 
 * boot up, since these don't persist 
 * @author dan
 *
 */
public class OnBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		AlarmAlertWakeLock.acquireCpuWakeLock(context);
		context.startService(new Intent(WakeMeSkiService.ACTION_ALARM_SCHEDULE,null,context,WakeMeSkiService.class));
	}

}
