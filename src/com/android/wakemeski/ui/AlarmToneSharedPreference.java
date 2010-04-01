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

import android.net.Uri;

/**
 * POJO to access the alarm tone saved in a shared preference
 * @author dan
 *
 */
public class AlarmToneSharedPreference {


	private String mAlertMediaString = null;
	
	
	public AlarmToneSharedPreference(String sharedPrefString) {
		mAlertMediaString = sharedPrefString;
	}

	public AlarmToneSharedPreference() {
	}
	
	public void setFromPersistString( String persistString ) {
		mAlertMediaString = persistString;
	}
	
	public String getAlertMediaString() {
		return mAlertMediaString;
	}
	
	public void setFromUri( Uri uri ) {
		if( uri != null ) {
			mAlertMediaString = uri.toString();
		}
		else {
			mAlertMediaString = null;
		}
	}
	
	public Uri getUri() {
		Uri returnUri = null;
		if( mAlertMediaString != null ) {
			Uri.Builder builder = new Uri.Builder();
			builder.path(mAlertMediaString);
			returnUri = builder.build();
		}
		return returnUri; 
	}
	
}
