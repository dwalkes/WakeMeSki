package com.wakemeski.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.wakemeski.R;
import com.wakemeski.pref.SnowSettingsSharedPreference;

/**
 * Extends SnowSettingsPreference to obtain a preference for wake-up purposes
 */
public class WakeupSnowSettingsPreference extends SnowSettingsPreference {

	public WakeupSnowSettingsPreference(Context context, AttributeSet attrs) {
		super(context,attrs,SnowSettingsSharedPreference.getWakeupPreference());
	}
	
	@Override
	protected String getUpdateSummaryPrefix() {
		// TODO Auto-generated method stub
		return getContext().getString(R.string.wake_on);
	}

	@Override
	protected String getUpdateSummarySuffix() {
		// TODO Auto-generated method stub
		return getContext().getString(R.string.wake_overnight);
	}

}
