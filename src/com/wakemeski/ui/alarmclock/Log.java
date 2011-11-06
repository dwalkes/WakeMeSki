/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * Modified by Dan Walkes for use with WakeMeSki
 */

/**
 * package-level logging flag
 */

package com.wakemeski.ui.alarmclock;

import android.util.Config;

import com.wakemeski.ui.WakeMeSkiPreferences;

class Log {
	public final static String LOGTAG = "WakeMeSki";

	static final boolean LOGV = WakeMeSkiPreferences.DEBUG ? Config.LOGD
			: Config.LOGV;

	static void v(String logMe) {
		com.wakemeski.Log.d(logMe);
	}

	static void e(String logMe) {
		com.wakemeski.Log.e(logMe);
	}

	static void e(String logMe, Exception ex) {
		com.wakemeski.Log.e(logMe, ex);
	}
}
