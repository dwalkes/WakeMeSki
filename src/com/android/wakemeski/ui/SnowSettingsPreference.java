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

import com.android.wakemeski.R;

import android.content.Context;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

/**
 * A preference class that shows a dialog for selecting how much snow a 
 * configured resort must receive before enabling a wakeup call.
 * 
 * @author dan
 *
 */
public class SnowSettingsPreference extends DialogPreference {

	Spinner	mDepthSpinner = null;
	Spinner mUnitsSpinner = null;
	
	final static int SNOW_DEPTH_1_START_INDEX = 0;
	final static int SNOW_DEPTH_INCHES_INDEX = 0;
	final static int SNOW_DEPTH_CENTIMETERS_INDEX = 1;
	SnowSettingsSharedPreference mPreference;
	
	public SnowSettingsPreference( Context context, AttributeSet attrs ) {
		super(context,attrs);
		mPreference = new SnowSettingsSharedPreference();
	}
	
	SnowUnits getMeasurementUnits() {
		return mPreference.getMeasurementUnits();
	}
	
	int getSnowDepth() {
		return mPreference.getSnowDepth();
	}
	
	/**
	 * Sets custom properties on the dialog
	 */
	protected void onBindDialogView( View view ) {
		super.onBindDialogView(view);
		if( view != null ) {
			mDepthSpinner = (Spinner)view.findViewById(R.id.snow_settings_amount);
			mUnitsSpinner = (Spinner)view.findViewById(R.id.snow_settings_units);
			
			if( mUnitsSpinner != null ) {
				if( getMeasurementUnits() == SnowUnits.CENTIMETERS ) {
					mUnitsSpinner.setSelection(SNOW_DEPTH_CENTIMETERS_INDEX);
				} else {
					mUnitsSpinner.setSelection(SNOW_DEPTH_INCHES_INDEX);
				}
			}
			if( mDepthSpinner != null ) {
				mDepthSpinner.setSelection(getSnowDepth() + SNOW_DEPTH_1_START_INDEX - 1);
			}
		}
	}
	
	/**
	 * Updates persistent storage based on current values of variables
	 */
	private void updatePersist() {
		StringBuilder builder = new StringBuilder();
		builder.append(Integer.toString(getSnowDepth()));
		builder.append(',');
		builder.append(getMeasurementUnits().toString());
		persistString(builder.toString());
		updateSummary();
	}
	
	/**
	 * Called when the dialog is closed through OK or cancel buttons.  Saves settings back to 
	 * persistent storage. 
	 */
	public void onDialogClosed( boolean positiveResult ) {
		if( positiveResult ) {
			if( mDepthSpinner != null ) {
				int position = mDepthSpinner.getSelectedItemPosition();
				if( position != Spinner.INVALID_POSITION ) {
					mPreference.setSnowDepth(position + 1 - SNOW_DEPTH_1_START_INDEX);
				}
			}
			if( mUnitsSpinner != null ) { 
				int position = mUnitsSpinner.getSelectedItemPosition();
				if( position == SNOW_DEPTH_CENTIMETERS_INDEX) {
					mPreference.setMeasurementUnits(SnowUnits.CENTIMETERS);
				} else {
					mPreference.setMeasurementUnits(SnowUnits.INCHES);
				}
			}
			updatePersist();
		}
	}


	
	/**
	 * Updates the summary text of this preference with the status stored in this class
	 */
	private void updateSummary() {
		StringBuilder builder = new StringBuilder();
		builder.append(getContext().getString(R.string.wake_on));
		builder.append(' ');
		builder.append(Integer.toString(getSnowDepth()));
		builder.append(' ');
		String[] measurementUnits = getContext().getResources().getStringArray(R.array.inches_cm);
		if( getMeasurementUnits() == SnowUnits.CENTIMETERS ) {
			builder.append(measurementUnits[SNOW_DEPTH_CENTIMETERS_INDEX]);
		}
		else {
			builder.append(measurementUnits[SNOW_DEPTH_INCHES_INDEX]);
		}
		builder.append(' ');
		builder.append(getContext().getString(R.string.wake_overnight));
		setSummary(builder.toString());
	}
	
	/**
	 * This is our first chance to read persistent data
	 */
	protected void onAttachedToHierarchy( PreferenceManager preferenceManager ) {
		super.onAttachedToHierarchy(preferenceManager);
		if( !mPreference.setFromPersistedString(this.getPersistedString(null)) ) {
			// set the defaults into persistent storage if not valid at start
			updatePersist();
		}
		updateSummary();

	}
	
}
