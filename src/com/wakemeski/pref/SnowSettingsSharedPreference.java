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
package com.wakemeski.pref;

import android.content.SharedPreferences;

import com.wakemeski.core.SnowUnits;
import com.wakemeski.ui.WakeMeSkiPreferences;

/**
 * A POJO used to access snow settings from a shared preference. Shared by the
 * preference dialog class and any class reading snow settings directly out of
 * the shared preferences datastore.
 *
 * @author dan
 *
 */
public class SnowSettingsSharedPreference {
	private int snowDepth = 1; // the default
	private SnowUnits measurementUnits = SnowUnits.INCHES;
	private final String mPrefKey;

	/**
	 * A singleton reference to the snow settings shared preference
	 * for wakeup snow threshold
	 */
	private static SnowSettingsSharedPreference mWakeupPreference = null;

	/**
	 * A singleton reference to the snow settings shared preference for
	 * notify snow threshold
	 */
	private static SnowSettingsSharedPreference mNotifyPreference = null;

	private SnowSettingsSharedPreference(String prefKey) {
		mPrefKey = prefKey;
	}

	public int getSnowDepth() {
		return snowDepth;
	}

	public void setSnowDepth(int snowDepth) {
		this.snowDepth = snowDepth;
	}

	public SnowUnits getMeasurementUnits() {
		return measurementUnits;
	}

	public void setMeasurementUnits(SnowUnits measurementUnits) {
		this.measurementUnits = measurementUnits;
	}

	/**
	 * @return The key used to store this shared preference in persistent storage
	 */
	protected String getPreferenceKey() {
		return mPrefKey;
	}

	public boolean setFromPreferences(SharedPreferences prefs) {
		return setFromPersistedString(prefs.getString(
				getPreferenceKey(), null));
	}

	/**
	 * Converts from a persisted string to
	 *
	 * @param persistedString
	 * @return true if set, false if using defaults
	 */
	public boolean setFromPersistedString(String persistedString) {
		boolean snowSet = false;
		if (persistedString != null) {
			String[] snowPersist = persistedString.split(",");
			if (snowPersist.length >= 2) {
				int depth = 1;
				SnowUnits units = SnowUnits.INCHES;
				snowSet = true;
				try {
					depth = Integer.parseInt(snowPersist[0]);
				} catch (NumberFormatException ne) {
					snowSet = false;
				}
				if (snowPersist[1].equals(SnowUnits.CENTIMETERS.toString())) {
					units = SnowUnits.CENTIMETERS;
				} else {
					units = SnowUnits.INCHES;
				}

				if (snowSet) {
					snowDepth = depth;
					measurementUnits = units;
				}
			}
		}
		return snowSet;
	}

	@Override
	public String toString() {
		return snowDepth + " " + measurementUnits.getAbbreviation();
	}

	/**
	 * @return reference to the snow settings preference holding the value of the snow wakeup
	 * preference key
	 */
	public static synchronized SnowSettingsSharedPreference getWakeupPreference() {
		if( mWakeupPreference == null ) {
			mWakeupPreference = new SnowSettingsSharedPreference(WakeMeSkiPreferences.SNOW_WAKEUP_SETTINGS_KEY);
		}
		return mWakeupPreference;
	}

	/**
	 * @return a reference to the snow settings preference holding the value of the snow notify
	 * preference key
	 */
	public static synchronized SnowSettingsSharedPreference getNotifyPreference() {
		if( mNotifyPreference == null ) {
			mNotifyPreference = new SnowSettingsSharedPreference(WakeMeSkiPreferences.SNOW_ALERT_SETTINGS_KEY);
		}
		return mNotifyPreference;
	}

	/**
	 * Checks to see if the passed snow depth meets or exceeds the value
	 * stored in the preference
	 * @param snowDepth value to check
	 * @param units the units of the passed snowfall value
	 * @return true if this snow total meets or exceeds the value stored in the preference
	 */
	public boolean meetsPreference( int snowCheck, SnowUnits checkUnits ) {
		double depth = getSnowDepth();
		double reported = snowCheck;

		if (checkUnits == SnowUnits.CENTIMETERS)
			reported *= 2.54;

		if (getMeasurementUnits() == SnowUnits.CENTIMETERS)
			depth *= 2.54;

		return (reported >= depth);
	}
}
