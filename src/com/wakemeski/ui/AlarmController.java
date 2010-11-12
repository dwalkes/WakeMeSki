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

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.wakemeski.pref.AlarmToneSharedPreference;
import com.wakemeski.ui.alarmclock.AlarmAlert;
import com.wakemeski.ui.alarmclock.AlarmAlertFullScreen;

/**
 * POJO used for alarm related actions such as setting, clearing or firing the
 * alarm
 * 
 * @author dan
 * 
 */
public class AlarmController {
	public static final String ACTION_FIRE_ALARM = "com.wakemeski.ui.firealarm";

	private Context context;
	private static final String TAG = "AlarmController";

	public AlarmController(Context theContext) {
		context = theContext;
	}

	protected PendingIntent getPendingIntent(String intentString) {
		PendingIntent pi = null;
		Intent i = new Intent(intentString, null, context,
				OnAlarmReceiver.class);
		pi = PendingIntent.getBroadcast(context, 0, i, 0);
		return pi;
	}

	protected PendingIntent getPendingIntent() {
		return getPendingIntent(OnAlarmReceiver.ACTION_WAKE_CHECK);
	}

	/**
	 * Sets the next alarm based on the passed value
	 * 
	 * @param timeInMilliseconds
	 *            The next time the alarm should fire
	 * @return true if the alarm was set successfully
	 */
	public boolean setAlarm(long timeInMilliseconds) {
		boolean success = false;
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		if (mgr != null) {
			Log.d(TAG,"Setting alarm for time " + new Date(timeInMilliseconds));
			mgr.set(AlarmManager.RTC_WAKEUP, timeInMilliseconds,
					getPendingIntent());
			success = true;
		} else {
			Log.w(TAG, "Alarm manager could not be obtained");
		}
		return success;
	}

	/**
	 * Sets the next alarm based on the passed value
	 * 
	 * @param nextAlarm
	 *            An object representing the next time the alarm should fire
	 * @return true if the alarm was set successfully
	 */
	public boolean setAlarm(Calendar nextAlarm) {
		return setAlarm(nextAlarm.getTimeInMillis());
	}

	public void clearAlarm() {
		Log.i(TAG, "Clearing alarm");
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		if (mgr != null) {
			mgr.cancel(getPendingIntent());
		} else {
			Log.w(TAG, "Alarm manager could not be obtained");
		}
	}

	public void fireAlarm() {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		AlarmToneSharedPreference alarmSettings = new AlarmToneSharedPreference(
				prefs.getString(WakeMeSkiPreferences.ALARM_TONE_PREF_KEY,
						Settings.System.DEFAULT_RINGTONE_URI.toString()));

		Log.i(TAG, "Firing alarm");
		/* Close dialogs and window shade */
		Intent i = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(i);

		// Decide which activity to start based on the state of the keyguard.
		Class<?> c = AlarmAlert.class;
		KeyguardManager km = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (km.inKeyguardRestrictedInputMode()) {
			// Use the full screen activity for security.
			c = AlarmAlertFullScreen.class;
		}

		/*
		 * launch UI, explicitly stating that this is not due to user action so
		 * that the current app's notification management is not disturbed
		 */
		Intent fireAlarm = new Intent(context, c);
		// DW: old stuff from default android
		// fireAlarm.putExtra(Alarms.ID, id);
		// fireAlarm.putExtra(Alarms.LABEL,
		// intent.getStringExtra(Alarms.LABEL));
		fireAlarm.putExtra(AlarmAlert.MEDIA_ALERT_SOURCE_STRING_EXTRA,
				alarmSettings.getAlertMediaString());
		fireAlarm.putExtra(AlarmAlert.VIBRATE_BOOLEAN_EXTRA, true);
		fireAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		context.startActivity(fireAlarm);
	}
}
