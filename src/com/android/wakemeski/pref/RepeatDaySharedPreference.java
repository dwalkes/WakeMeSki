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
package com.android.wakemeski.pref;

import java.util.Calendar;

import com.android.wakemeski.ui.ListPreferenceMultiSelect;

import android.util.Log;

/**
 * A POJO used to access repeat day information shared in an android preference.
 * This POJO is shared by the preference activity and the code reading directly
 * from the shared preference datastore.
 * 
 * @author dan
 * 
 */
public class RepeatDaySharedPreference {
	private String[] selectedDays;
	private String TAG = "RepeatDaySharedPreference";

	/**
	 * Initializes this shared preference object from a persistent string
	 * 
	 * @param persistentString
	 *            The persistent string read from the database.
	 * @return true if the value was set successfully, false if using defaults
	 */
	public boolean setFromPersistString(String persistentString) {
		if (persistentString != null) {
			selectedDays = ListPreferenceMultiSelect
					.parseStoredValue(persistentString);
		} else {
			selectedDays = null;
		}
		return (selectedDays != null);
	}

	public boolean isDaySelected(int dayOfWeek) {
		boolean isSelected = false;
		String selectedString = null;
		switch (dayOfWeek) {
		case Calendar.SUNDAY:
			selectedString = "Sunday";
			break;
		case Calendar.MONDAY:
			selectedString = "Monday";
			break;
		case Calendar.TUESDAY:
			selectedString = "Tuesday";
			break;
		case Calendar.WEDNESDAY:
			selectedString = "Wednesday";
			break;
		case Calendar.THURSDAY:
			selectedString = "Thursday";
			break;
		case Calendar.FRIDAY:
			selectedString = "Friday";
			break;
		case Calendar.SATURDAY:
			selectedString = "Saturday";
			break;
		default:
			Log.e(TAG, "Unknown day of week" + dayOfWeek);
			break;
		}
		if (selectedDays != null && selectedString != null) {
			for (String day : selectedDays) {
				if (selectedString.equalsIgnoreCase(day)) {
					isSelected = true;
				}
			}
		}
		return isSelected;
	}

	public String[] getSelectedDays() {
		return selectedDays;
	}

}
