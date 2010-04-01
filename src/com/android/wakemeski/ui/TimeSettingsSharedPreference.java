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

/**
 * A POJO used to read/write the time value from persistent storage. Used by
 * both the dialog preference and by classes accessing the shared preferences
 * datastore.
 * 
 * @author dan
 * 
 */
public class TimeSettingsSharedPreference {
	int currentHour = 0;
	int currentMinute = 0;
	boolean is24Hour = false;

	public int getCurrentHour() {
		return currentHour;
	}

	public void setCurrentHour(int currentHour) {
		this.currentHour = currentHour;
	}

	public int getCurrentMinute() {
		return currentMinute;
	}

	public void setCurrentMinute(int mCurrentMinute) {
		this.currentMinute = mCurrentMinute;
	}

	public boolean is24Hour() {
		return is24Hour;
	}

	public void setIs24Hour(boolean is24Hour) {
		this.is24Hour = is24Hour;
	}

	public String getSummaryString() {
		StringBuilder builder = new StringBuilder();
		if (!is24Hour()) {
			if ((getCurrentHour() % 12) == 0) {
				builder.append(12);
			} else {
				builder.append(getCurrentHour() % 12);
			}
		} else {
			builder.append(getCurrentHour());
		}
		builder.append(':');
		builder.append(getCurrentMinute() / 10);
		builder.append(getCurrentMinute() % 10);
		if (!is24Hour()) {
			if (getCurrentHour() >= 12) {
				builder.append(" PM");
			} else {
				builder.append(" AM");
			}
		}
		return builder.toString();
	}

	public String getPersistString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getCurrentHour());
		builder.append(',');
		builder.append(getCurrentMinute());
		return builder.toString();
	}

	/**
	 * Sets the time in this object based on the persisted string
	 * 
	 * @param persistedString
	 *            The string from persistance store
	 * @return true if the store was parseable and time was changed
	 */
	public boolean setTimeFromPersistString(String persistedString) {
		boolean timeSet = false;
		if (persistedString != null) {
			String[] timePersist = persistedString.split(",");
			if (timePersist.length >= 2) {
				int hour = 0;
				int minute = 0;
				timeSet = true;
				try {
					hour = Integer.parseInt(timePersist[0]);
				} catch (NumberFormatException ne) {
					timeSet = false;
				}
				try {
					minute = Integer.parseInt(timePersist[1]);
				} catch (NumberFormatException ne) {
					timeSet = false;
				}
				if (timeSet) {
					currentHour = hour;
					currentMinute = minute;
				}

			}
		}
		return timeSet;
	}

}
