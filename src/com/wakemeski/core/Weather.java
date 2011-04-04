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
import com.wakemeski.Log;

import com.wakemeski.pref.SnowSettingsSharedPreference;

/**
 * A child of a Report object. It helps the report describe the upcoming weather
 * conditions
 */
public class Weather implements Parcelable {

	private String mWhen;
	private long mExact;
	private String mDesc;
	private SnowUnits mUnits;
	private static String TAG = "Weather";

	public Weather(String when, long exact, String desc, SnowUnits units) {
		mWhen = when;
		mExact = exact;
		mDesc = desc;
		mUnits = units;
	}

	private boolean meetsThreshold(SnowSettingsSharedPreference pref, Matcher m) {
		/*
		 * Look for the upper accumulation value
		 */
		String snowTotalString = m.group(m.groupCount());
		int snowTotal =0;
		if( snowTotalString != null ) {
			try {
				snowTotal = Integer.parseInt(snowTotalString);
			} catch (Throwable t) {
				Log.e(TAG, "Unable to parse snow total to int: " + snowTotalString);
			}
		}
		return pref.meetsPreference(snowTotal, mUnits);
	}

	/**
	 * @param pref the users preference about when to notify on a new
	 * expected snowfall predicted for a particular resort
	 * @param server contains regular expressions used to detect snow alerts
	 * @return true if
	 */
	public boolean hasSnowAlert(SnowSettingsSharedPreference pref, WakeMeSkiServer server) {
		String expressions[] = server.getServerInfo().getAlertExpressions();

		for( String expression: expressions ) {
			Pattern p = Pattern.compile(expression);
			Matcher m = p.matcher(mDesc);
			if( m.find() && meetsThreshold(pref, m) )
				return true;
		}

		return false;

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
			int inches = source.readInt();
			SnowUnits units = SnowUnits.CENTIMETERS;
			if( inches != 0 ) {
				units = SnowUnits.INCHES;
			}

			return new Weather(when, exact, desc, units);
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
		dest.writeInt(mUnits.equals(SnowUnits.INCHES) ? 1 : 0);
	}
}
