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
 */
package com.android.wakemeski.core;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A child of a Report object. It helps the report describe the upcoming weather
 * conditions
 */
public class Weather implements Parcelable {
	
	private String mWhen;
	private String mDesc;

	public Weather(String when, String desc) {
		mWhen = when;
		mDesc = desc;
	}
	
	public String getWhen() {
		return mWhen;
	}
	
	public String getDesc() {
		return mDesc;
	}
	
	public static final Parcelable.Creator<Weather> CREATOR = new Parcelable.Creator<Weather>() {
		@Override
		public Weather createFromParcel(Parcel source) {
			String when = source.readString();
			String desc = source.readString();

			return new Weather(when, desc);
		}
		
		@Override
		public Weather[] newArray(int size) {
			return new Weather[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mWhen);
		dest.writeString(mDesc);
	}
}
