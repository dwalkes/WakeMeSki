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

import android.app.PendingIntent;
import android.content.Context;

/**
 * Alarm controller for snooze actions
 * 
 * @author dan
 * 
 */
public class AlarmSnoozeController extends AlarmController {

	public AlarmSnoozeController(Context context) {
		super(context);
	}

	protected PendingIntent getPendingIntent() {
		return super.getPendingIntent(OnAlarmReceiver.ACTION_SNOOZE);
	}

}
