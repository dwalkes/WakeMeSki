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
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.wakemeski.R;
import com.wakemeski.pref.RepeatDaySharedPreference;

/**
 * A preference class to hold a list of days. In this case the list of days is
 * used to select when the user would like to be woken up for good ski
 * conditions.
 * 
 * @author dan
 * 
 */
public class RepeatDayPreference extends ListPreferenceMultiSelect {

	Preference.OnPreferenceChangeListener prefListener;
	RepeatDaySharedPreference sharedPreference;

	public RepeatDaySharedPreference getSharedPreference() {
		return sharedPreference;
	}

	/**
	 * Updates the RepeatDataPreference summary given the settings in the
	 * preference. RepeatData summary (bottom of dialog) will list the first
	 * three letters of day1,day2, etc. IE Mon,Tue,Wed
	 */
	private void updateSummary() {
		if (sharedPreference != null) {
			StringBuilder builder = new StringBuilder();
			String[] selectedDays = sharedPreference.getSelectedDays();
			if (selectedDays != null) {
				for (String day : selectedDays) {
					if (builder.length() > 0) {
						builder.append(",");
					}
					// Add the first 3 letters of this day of the week
					builder.append(day.substring(0, day.length() > 3 ? 3 : day
							.length()));
				}
			}
			if (builder.length() > 0) {
				setSummary(builder);
			} else {
				setSummary(R.string.none);
			}
		} else {
			setSummary(R.string.none);
		}
	}

	/**
	 * This is the first time the persistent value can be read
	 * 
	 * @param preferenceManager
	 *            - the manager attached to this hierarchy
	 */
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		sharedPreference.setFromPersistString(this.getValue());
		updateSummary();
	}

	/**
	 * public c'tor
	 * 
	 * @param context
	 * @param attrs
	 */
	public RepeatDayPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		prefListener = null;
		sharedPreference = new RepeatDaySharedPreference();

		/**
		 * Setup our own pref change listener in the constructor. This allows us
		 * to update the summary automatically when the preference is changed.
		 */
		super
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference pref,
							Object newValue) {
						if (newValue != null) {
							sharedPreference.setFromPersistString(newValue
									.toString());
							updateSummary();
						}
						/**
						 * Call the registered callback if applicable
						 */
						boolean returnValue = prefListenerCallback(pref,
								newValue);
						return returnValue;
					}
				});

	}

	/**
	 * Since we override setOnPreferenceChangeListener and define our own in the
	 * c'tor, we need a way for classes to register for an additional change
	 * listener as necessary This synchronized method calls a registered pref
	 * listener
	 * 
	 * @param pref
	 *            The value to pass to the registered pref listener
	 * @param newValue
	 *            The value to pass to the registered pref listener
	 */
	private synchronized boolean prefListenerCallback(Preference pref,
			Object newValue) {
		boolean returnValue = true;
		if (prefListener != null) {
			returnValue = prefListener.onPreferenceChange(pref, newValue);
		}
		return returnValue;
	}

	@Override
	/**
	 * Override setOnPreferenceChangeListener to save a reference to the class to call on preference change
	 */
	public synchronized void setOnPreferenceChangeListener(
			Preference.OnPreferenceChangeListener listener) {
		prefListener = listener;
	}

}
