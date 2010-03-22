/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * Modified by Dan Walkes for use with WakeMeSki
 */

package com.dwalkes.android.wakemeski.alarmclock;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

import com.dwalkes.android.wakemeski.AlarmToneSharedPreference;

public class AlarmPreference extends RingtonePreference {
    AlarmToneSharedPreference 	mPreference;
    private IRingtoneChangedListener mRingtoneChangedListener;
    
    public interface IRingtoneChangedListener {
        public void onRingtoneChanged(Uri ringtoneUri);
    };
    
    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPreference = new AlarmToneSharedPreference();
        mRingtoneChangedListener = null;
    }

    public void setRingtoneChangedListener(IRingtoneChangedListener listener) {
        mRingtoneChangedListener = listener;
    }
    
    public void updateSummary() {
//    	String summaryString = mPreference.getSummaryString(mRingtoneUriToTitle);
//    	this.setSummary(summaryString); 
    }
    
    protected void onSaveRingtone(Uri ringtoneUri ) {
    	mPreference.setFromUri(ringtoneUri);
    	updateSummary();
        if (mRingtoneChangedListener != null) {
            mRingtoneChangedListener.onRingtoneChanged(ringtoneUri);
        }
    	super.onSaveRingtone(ringtoneUri);
    }
    
	/**
	 * This is the first time the persistent value can be read
	 * @param preferenceManager - the manager attached to this hierarchy
	 */
    @Override
	protected void onAttachedToHierarchy( PreferenceManager preferenceManager ) {
		super.onAttachedToHierarchy(preferenceManager);
		mPreference.setFromPersistString(this.getPersistedString(null));
		updateSummary();
	}
	
	public Uri getTone() {
		return mPreference.getUri();
	}
	
	
}
