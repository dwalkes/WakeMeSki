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

package com.android.wakemeski.ui.alarmclock;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

/**
 * Hold a wakelock that can be acquired in the AlarmReceiver and released in the
 * AlarmAlert activity
 */
public class AlarmAlertWakeLock {

	private static PowerManager.WakeLock sScreenWakeLock = null;
	private static PowerManager.WakeLock sCpuWakeLock = null;
	private static KeyguardManager.KeyguardLock mKeyguardLock = null;

	public static void acquireCpuWakeLock(Context context) {
		Log.v("Acquiring cpu wake lock");
		if (sCpuWakeLock != null) {
			return;
		}

		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);

		sCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, Log.LOGTAG);
		sCpuWakeLock.acquire();
	}

	public static void acquireScreenWakeLock(Context context) {
		Log.v("Acquiring screen wake lock");
		if (sScreenWakeLock != null) {
			return;
		}

		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);

		sScreenWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, Log.LOGTAG);
		sScreenWakeLock.acquire();

		/*
		 * Disable the keyguard if activeFor 2.0 or later this is easier, just
		 * use addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)See
		 * http://gitorious.com/rowboat/packages-apps-alarmclock/commit/01d
		 * ee6eedd37dd50961584a7c717c85f00131401
		 */
		KeyguardManager keyMgr = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (keyMgr == null) {
			Log.e("Cannot access keyguard");
		} else {
			mKeyguardLock = keyMgr.newKeyguardLock("com.dwalkes.wakemeski");
			if (mKeyguardLock != null) {
				if (Log.LOGV)
					Log.v("keyguardLock Created");
			}
		}
		if (mKeyguardLock != null) {
			if (Log.LOGV)
				Log.v("disabling keyguard");
			mKeyguardLock.disableKeyguard();
		}
	}

	public static void release() {
		Log.v("Releasing wake lock");
		if (sCpuWakeLock != null) {
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
		if (sScreenWakeLock != null) {
			sScreenWakeLock.release();
			sScreenWakeLock = null;
		}
		if (mKeyguardLock != null) {
			if (Log.LOGV)
				Log.v("re-enabling keyguard");
			mKeyguardLock.reenableKeyguard();
			mKeyguardLock = null;
		}
	}
}
