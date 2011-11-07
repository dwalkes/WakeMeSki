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
 */
package com.wakemeski.deskclock_custom;

import android.content.Context;
import android.content.Intent;

import com.wakemeski.R;
import com.wakemeski.core.WakeMeSkiWakeupService;
import com.wakemeski.generic_deskclock.AlarmClockBase;
import com.wakemeski.generic_deskclock.GenericDeskClockCustomization;
import com.wakemeski.ui.OnAlarmReceiver;

public class WakeMeSkiAlarmCustomization extends GenericDeskClockCustomization {
	
	@Override
	public Intent getAlarmFireIntent(Context context) {
		return new Intent(WakeMeSkiWakeupService.ACTION_WAKE_CHECK,null,context,OnAlarmReceiver.class);
	}
	
	@Override
	public Intent getAlarmSnoozeIntent(Context context) {
		return super.getAlarmFireIntent(context);
	}
	
	@Override
	public Intent getShowAlertIntent(Context context) {
		return super.getAlarmFireIntent(context);
	}
	
	/**
	 * Content URI authority.  Must match the value specified in the application manifest
	 * @return
	 */
	@Override
	public String getContentURIAuthority() {
		return new String("com.wakemeski");
	}
	
	/**
	 * @return the class type used to implement the AlarmClock dialog where a user can pick/enable specific
	 * alarms
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class getAlarmClock() {
		return AlarmClockBase.class;
	}
	
	@Override
	public boolean isSaveAlarmInSystemSettings() {
		return false;
	}
	
	@Override
	public String getAlarmAlertDefaultLabel(Context context) {
		return context.getString(R.string.ski_wake_up);
	}
	
}
