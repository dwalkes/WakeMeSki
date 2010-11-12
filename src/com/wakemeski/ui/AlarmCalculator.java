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

import com.wakemeski.pref.RepeatDaySharedPreference;
import com.wakemeski.pref.TimeSettingsSharedPreference;

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
	 *         fire, given a timeNow calendar object representing the current time
	 */
	public Calendar getNextAlarm( Calendar timeNow ) {
		Calendar nextAlarm = timeNow;
		if (mDaySelect == null || mTimeSelect == null) {
			return null;
		}
		int setHour = mTimeSelect.getCurrentHour();
		int setMinute = mTimeSelect.getCurrentMinute();

		nextAlarm.set(Calendar.SECOND, 0);
		/*
		 *  never set less than 2 minutes ahead - always go to the next day in
		 *  this case.  Otherwise it would be possible for the second value to 
		 *  roll from 59 to 00 and increment the minute value immediately after
		 *  we obtained the time, meaning we would set an alarm for a time before
		 *  the current time. This gives us a in the worst case a full minute
		 *  to go from the .getInstance() call on calendar to the time we actually
		 *  set the alarm to ensure we haven't already passed the time for the new alarm.
		 */
		nextAlarm.add(Calendar.MINUTE, 2);
				/*
				 * If the alarm hour is greater than the current hour of day,
				 * we will consider setting the alarm today
				 */
		if (	nextAlarm.get(Calendar.HOUR_OF_DAY) < setHour ||
				/*
				 * If the alarm time specifies the same hour, compare minutes to
				 * decide if we should set today or another day
				 */
				(nextAlarm.get(Calendar.HOUR_OF_DAY) == setHour && 
						nextAlarm.get(Calendar.MINUTE) < setMinute)
					
					/*
					 * If today is one of the days selected for wakeup we might
					 * leave day of week at current value.
					 */
				&& mDaySelect
						.isDaySelected(nextAlarm.get(Calendar.DAY_OF_WEEK))) {
		} else {
			/*
			 * One of the tests above failed, we need to go to a new day.
			 * Find the next one in the day select preference.
			 */
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
	
	/**
	 * @return a calendar object representing the next time the alarm should
	 *         fire, or null if no day is selected for the next alarm
	 */
	public Calendar getNextAlarm() {

		return getNextAlarm(Calendar.getInstance());
	}

}
