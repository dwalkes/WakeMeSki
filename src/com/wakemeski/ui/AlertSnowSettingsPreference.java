/*
 * Copyright (C) 2010 Dan Walkes, Andy Doan
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
import android.util.AttributeSet;

import com.wakemeski.R;
import com.wakemeski.pref.SnowSettingsSharedPreference;

/**
 * Modifies SnowSettingsPreference to select a preference for the purpose
 * of notifications (ie forecasted snow)
 *
 */
public class AlertSnowSettingsPreference extends SnowSettingsPreference {

	public AlertSnowSettingsPreference(Context context, AttributeSet attrs) {
		super(context,attrs,SnowSettingsSharedPreference.getNotifyPreference());
	}

	@Override
	protected String getUpdateSummaryPrefix() {
		return getContext().getString(R.string.alert_when);
	}

	@Override
	protected String getUpdateSummarySuffix() {
		return getContext().getString(R.string.is_forecasted_for_resort);
	}

}
