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

import android.content.Context;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.wakemeski.pref.TimeSettingsSharedPreference;

/**
 * Configure a time (in this case the time for wakeup resort checks) and store
 * in persistent storage.
 *
 * @author dan
 *
 */
public class TimePickerPreference extends DialogPreference {
	private TimePicker mTimePicker;
	private final Context mContext;
	private final AttributeSet mAttributes;
	private final TimeSettingsSharedPreference mPreference;

	public TimePickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mAttributes = attrs;
		mTimePicker = null;
		mPreference = new TimeSettingsSharedPreference(context);
	}

	@Override
	protected View onCreateDialogView() {
		mTimePicker = new TimePicker(mContext, mAttributes);
		if (mTimePicker != null) {
			mTimePicker.setCurrentHour(mPreference.getCurrentHour());
			mTimePicker.setCurrentMinute(mPreference.getCurrentMinute());
			mTimePicker.setIs24HourView(mPreference.is24Hour());
		}
		return mTimePicker;
	}

	private void updateSummary() {
		setSummary(mPreference.getSummaryString());
	}

	private void updatePersist() {

		persistString(mPreference.getPersistString());
		updateSummary();
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			if (mTimePicker != null) {
				mPreference.setCurrentHour(mTimePicker.getCurrentHour());
				mPreference.setCurrentMinute(mTimePicker.getCurrentMinute());
				updatePersist();
			}
		}
	}

	/**
	 * This is our first chance to read persistant data
	 */
	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		if (mPreference.setTimeFromPersistString(this.getPersistedString(null))) {
			if (mTimePicker != null) {
				mTimePicker.setCurrentHour(mPreference.getCurrentHour());
				mTimePicker.setCurrentMinute(mPreference.getCurrentMinute());
			}
		}
		updateSummary();
	}

	/**
	 * @return The current hour selected 0-23
	 */
	public int getCurrentHour() {
		return mPreference.getCurrentHour();
	}

	/**
	 *
	 * @return The current minute selected 0-59
	 */
	public int getCurrentMinute() {
		return mPreference.getCurrentMinute();
	}

	public TimeSettingsSharedPreference getSharedPreference() {
		return mPreference;
	}

	public void setDefaultValue(int hour, int minute) {
		mPreference.setCurrentHour(hour);
		mPreference.setCurrentMinute(minute);
		setDefaultValue(mPreference.getPersistString());
	}
}
