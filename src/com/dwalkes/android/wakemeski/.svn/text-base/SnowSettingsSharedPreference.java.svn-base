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
package com.dwalkes.android.wakemeski;

import android.content.SharedPreferences;


/**
 * A POJO used to access snow settings from a shared preference.  Shared
 * by the preference dialog class and any class reading snow settings directly
 * out of the shared preferences datastore.
 * 
 * @author dan
 *
 */
public class SnowSettingsSharedPreference {
	private int 	snowDepth = 1;	// the default
	private SnowUnits	measurementUnits = SnowUnits.INCHES;
	
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

	public boolean setFromPreferences(SharedPreferences prefs) {
		return setFromPersistedString(
				prefs.getString(WakeMeSkiPreferences.SNOW_SETTINGS_PREF_KEY,null));
	}
	/**
	 * Converts from a persisted string to 
	 * @param persistedString
	 * @return true if set, false if using defaults
	 */
	public boolean setFromPersistedString( String persistedString ) {
		boolean snowSet = false;
		if( persistedString != null ) {
			String[] snowPersist = persistedString.split(",");
			if( snowPersist.length >= 2 ) {
				int depth=1;
				SnowUnits units = SnowUnits.INCHES;
				snowSet = true;
				try {
					depth = Integer.parseInt(snowPersist[0]);
				}
				catch( NumberFormatException ne ) {
					snowSet = false;
				}
				if( snowPersist[1].equals(SnowUnits.CENTIMETERS.toString()) ) {
					units = SnowUnits.CENTIMETERS;
				} else {
					units = SnowUnits.INCHES;
				}

				if( snowSet ) {
					snowDepth = depth;
					measurementUnits = units;
				}
			}
		}
		return snowSet;
	}
	
	public String toString() {
		return snowDepth + " " + measurementUnits.getAbbreviation();
	}
	
	public String toLog() {
		return toString() + " in 24 hours";
	}
}
