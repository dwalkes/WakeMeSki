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

import java.util.Calendar;

public class AlarmCalculator {
	RepeatDaySharedPreference mDaySelect;
	TimeSettingsSharedPreference mTimeSelect;

	public AlarmCalculator(RepeatDaySharedPreference dayPreference,
			TimeSettingsSharedPreference timeSelect) {
		mDaySelect = dayPreference;
		mTimeSelect = timeSelect;
	}

	/**
	 * @return a calendar object representing the next time the alarm should
	 *         fire, or null if no day is selected for the next alarm
	 */
	public Calendar getNextAlarm() {
		if (mDaySelect == null || mTimeSelect == null) {
			return null;
		}
		int setHour = mTimeSelect.getCurrentHour();
		int setMinute = mTimeSelect.getCurrentMinute();

		// start with the time now
		Calendar nextAlarm = Calendar.getInstance();
		nextAlarm.set(Calendar.SECOND, 0);
		// never set less than 1 minutes ahead - always go to the next day in
		// this case
		nextAlarm.add(Calendar.MINUTE, 1);
		if (nextAlarm.get(Calendar.HOUR_OF_DAY) <= setHour
				&& nextAlarm.get(Calendar.MINUTE) < setMinute
				&& mDaySelect
						.isDaySelected(nextAlarm.get(Calendar.DAY_OF_WEEK))) {
			// leave day of week at current value
		} else {
			// go to the next selected day of week
			int i = 0;
			for (; i < 7; i++) {
				nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
				if (mDaySelect.isDaySelected(nextAlarm
						.get(Calendar.DAY_OF_WEEK))) {
					break;
				}
			}
			if (i == 7) {
				// no day was selected
				nextAlarm = null;
			}
		}
		if (nextAlarm != null) {
			if (setHour >= 12) {
				nextAlarm.set(Calendar.AM_PM, Calendar.PM);
			} else {
				nextAlarm.set(Calendar.AM_PM, Calendar.AM);
			}
			// setting the value on this day
			nextAlarm.set(Calendar.HOUR_OF_DAY, setHour);

			nextAlarm.set(Calendar.MINUTE, setMinute);
		}
		return nextAlarm;
	}

}
