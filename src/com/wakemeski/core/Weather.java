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
package com.wakemeski.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A child of a Report object. It helps the report describe the upcoming weather
 * conditions
 */
public class Weather implements Parcelable {
	
	private String mWhen;
	private long mExact;
	private String mDesc;

	public Weather(String when, long exact, String desc) {
		mWhen = when;
		mExact = exact;
		mDesc = desc;
	}

	//TODO: make this a configurable threshold
	public boolean hasSnowAlert() {
		Pattern p = Pattern.compile("snow accumulation of (\\d+) to (\\d+)");
		Matcher m = p.matcher(mDesc);
		
		return m.find();
	}

	public String getWhen() {
		return mWhen;
	}

	public long getExact() {
		return mExact;
	}
	
	public String getDesc() {
		return mDesc;
	}
	
	public static final Parcelable.Creator<Weather> CREATOR = new Parcelable.Creator<Weather>() {
		@Override
		public Weather createFromParcel(Parcel source) {
			String when = source.readString();
			long   exact = source.readLong();
			String desc = source.readString();

			return new Weather(when, exact, desc);
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
		dest.writeLong(mExact);
		dest.writeString(mDesc);
	}
}
