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
package com.wakemeski;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.wakemeski.core.WakeMeSkiFactory;
import com.wakemeski.core.alert.AlertPollingController;
import com.wakemeski.ui.WakeMeSkiPreferences;

public class WakeMeSki extends Application {

	/**
	 * Global control of all debug applications including logging to logcat
	 */
	public static final boolean DEBUG = false;

	@Override
	public void onCreate() {
		super.onCreate();

		WakeMeSkiFactory.getInstance(this);
		Context c = getApplicationContext();
		/**
		 * see http://groups.google.com/group/android-developers/browse_thread/thread/697e7e7cc6dd3d1f/f1bb447b4d38ee57?#f1bb447b4d38ee57
		 * By default alert notification is enabled in the preferences area but
		 * I need a way to actually start alert polling through the AlarmManager.
		 * If I had a way to run this code on install only I would, however I can't figure it
		 * out (see thread above.)  Instead this will run every time the ap is launched which
		 * is overkill but the best way I can figure out to make sure alert notification is scheduled.
		 */
		if( WakeMeSkiPreferences.isAlertNotificationEnabled(PreferenceManager
				.getDefaultSharedPreferences(c)) ) {
			AlertPollingController.getInstance(c).enableAlertPolling();
		}
	}
}
