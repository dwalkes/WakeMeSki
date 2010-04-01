/*
 * Copyright (c) 2008 nombre.usario@gmail.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.wakemeski.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.wakemeski.R;
import com.android.wakemeski.ui.SnowSettingsSharedPreference;

public class Report implements Parcelable {
	private String _location = "";
	private String _date = "";
	private String _windAvg = "";
	private String _detailsURL = "";

	private int _trailsOpen = 0;
	private int _trailsTotal = 0;

	private int _liftsOpen = 0;
	private int _liftsTotal = 0;

	private String _weatherUrl = "";
	private String _weatherIcon = "";

	private String _latitude = "";
	private String _longitude = "";

	private String _snowConditions = "";

	private ArrayList<String> _snowTotals = new ArrayList<String>();
	private ArrayList<String> _dailySnow = new ArrayList<String>();
	private ArrayList<String> _tempReadings = new ArrayList<String>();
	private String _freshSnow = "";
	private String _snowUnits = "inches";

	private String _label;

	// Used to display an error if one occurred
	private String _errMsg = null;

	private static final String TAG = "com.android.wakemeski.core.Report";

	public static final Parcelable.Creator<Report> CREATOR = new Parcelable.Creator<Report>() {
		@Override
		public Report createFromParcel(Parcel source) {
			Report r = new Report();

			r._location = source.readString();
			r._date = source.readString();
			r._windAvg = source.readString();
			r._detailsURL = source.readString();

			r._trailsOpen = source.readInt();
			r._trailsTotal = source.readInt();

			r._liftsOpen = source.readInt();
			r._liftsTotal = source.readInt();

			r._label = source.readString();
			r._errMsg = source.readString();

			r._weatherUrl = source.readString();
			r._weatherIcon = source.readString();

			r._latitude = source.readString();
			r._longitude = source.readString();

			r._snowConditions = source.readString();

			r._freshSnow = source.readString();

			r._snowUnits = source.readString();

			source.readList(r._snowTotals, getClass().getClassLoader());
			source.readList(r._dailySnow, getClass().getClassLoader());
			source.readList(r._tempReadings, getClass().getClassLoader());

			return r;
		}

		@Override
		public Report[] newArray(int size) {
			return new Report[size];
		}
	};

	public static boolean meetsPreference(Report r,
			SnowSettingsSharedPreference s) {
		double depth = s.getSnowDepth();
		double reported = r.getFreshSnowTotal();

		if (r.getSnowUnits() != SnowUnits.CENTIMETERS)
			reported *= 2.54;

		if (s.getMeasurementUnits() == SnowUnits.CENTIMETERS)
			depth *= 2.54;

		return (reported >= depth);
	}

	/**
	 * Private constructor. loadReport should be used to construct an instance.
	 */
	private Report() {
	}

	/**
	 * Returns the name of the location the report is for
	 */
	public String getLabel() {
		return _label;
	}

	/**
	 * Returns the date the report data was generated.
	 */
	public String getDate() {
		return _date;
	}

	/**
	 * Returns the average wind speed recorded on the mountain
	 */
	public String getWindSpeed() {
		return _windAvg;
	}

	public int getTrailsOpen() {
		return _trailsOpen;
	}

	public int getTrailsTotal() {
		return _trailsTotal;
	}

	public int getLiftsOpen() {
		return _liftsOpen;
	}

	public int getLiftsTotal() {
		return _liftsTotal;
	}

	/**
	 * Returns an array of total snow depths read on the mountain
	 */
	public String[] getSnowDepths() {
		return _snowTotals.toArray(new String[_snowTotals.size()]);
	}

	/**
	 * Returns an array of all daily snow reports found
	 */
	public String[] getDailySnow() {
		return _dailySnow.toArray(new String[_dailySnow.size()]);
	}

	/**
	 * @return true if a fresh snow total was obtained for this resort
	 */
	public boolean hasFreshSnowTotal() {
		return getFreshSnowTotal() >= 0;
	}

	/**
	 * @return The units for all snow totals
	 */
	public SnowUnits getSnowUnits() {
		if (_snowUnits.equalsIgnoreCase("inches")) {
			return SnowUnits.INCHES;
		}
		return SnowUnits.CENTIMETERS;
	}

	/**
	 * @return The total fresh snowfall (rounded to nearest int) or -1 if !
	 *         hasFreshSnowTotal()
	 */
	public int getFreshSnowTotal() {
		int snowTotal;
		try {
			snowTotal = Math.round(Float.parseFloat(_freshSnow));
		} catch (NumberFormatException nfe) {
			snowTotal = -1;
		}
		return snowTotal;
	}

	public String getFreshAsString() {
		String unit = " \"";
		if (getSnowUnits() == SnowUnits.CENTIMETERS)
			unit = " cm";

		return getFreshSnowTotal() + unit;
	}

	public String getSnowConditions() {
		return _snowConditions;
	}

	/**
	 * Returns an array of the various temperature readings on the mountain.
	 */
	public String[] getTemperatureReadings() {
		return _tempReadings.toArray(new String[_tempReadings.size()]);
	}

	/**
	 * Used to display an error if one occurred
	 */
	public String getError() {
		return _errMsg;
	}

	public Uri getGeo() {
		return Uri.parse("geo:" + _latitude + "," + _longitude);
	}

	/**
	 * Returns a URL to information related to the given report
	 */
	public String getDetailsURL() {
		return _detailsURL;
	}

	public String getWeatherURL() {
		return _weatherUrl;
	}

	public String getWeatherIcon() {
		return _weatherIcon;
	}

	public int getWeatherIconResId() {
		String parts[] = _weatherIcon.split("/");
		String name = parts[parts.length - 1];
		parts = name.split("\\.");
		name = parts[0];

		Pattern p = Pattern.compile("(\\d+)");
		Matcher m = p.matcher(name);
		if (m.find()) {
			// TODO String percentage = m.group(0);
			name = name.substring(0, m.start());
		}

		Integer i = _icons.get(name);
		if (i != null) {
			return i.intValue();
		}

		return R.drawable.unknown;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(_location);
		dest.writeString(_date);
		dest.writeString(_windAvg);
		dest.writeString(_detailsURL);

		dest.writeInt(_trailsOpen);
		dest.writeInt(_trailsTotal);

		dest.writeInt(_liftsOpen);
		dest.writeInt(_liftsTotal);

		dest.writeString(_label);
		dest.writeString(_errMsg);

		dest.writeString(_weatherUrl);
		dest.writeString(_weatherIcon);

		dest.writeString(_latitude);
		dest.writeString(_longitude);

		dest.writeString(_snowConditions);

		dest.writeString(_freshSnow);
		dest.writeString(_snowUnits);

		dest.writeList(_snowTotals);
		dest.writeList(_dailySnow);
		dest.writeList(_tempReadings);
	}

	private static ArrayList<String> toList(String vals[]) {
		ArrayList<String> l = new ArrayList<String>();

		for (String v : vals) {
			l.add(v);
		}
		return l;
	}

	/**
	 * Simple wrapper to Integer.parseInt that will catch format errors
	 */
	private static int getInt(String val) {
		int v = 0;
		try {
			v = Integer.parseInt(val);
		} catch (Throwable t) {
			Log.e(TAG, "Unable to parse value to int: " + val);
		}

		return v;
	}

	/**
	 * Loads a report from the given location
	 */
	public static Report loadReport(Context c, ConnectivityManager cm,
			Location l) {
		// A report will be in the format:
		// location = OSOALP
		// date = 12-6-2008
		// lifts.open = 5
		// lifts.total = 10
		// trails.open = 0
		// trails.total = 10
		// snow.total = 6
		// snow.daily = Fresh(4.3) [48 hr(<amount>)]
		// snow.fresh = 4.3
		// snow.units = inches
		// temp.readings = 41/33 44/38 43/34
		// wind.avg = 20

		Report r = new Report();
		r._label = l.getLabel();

		String lines[] = new String[0];

		try {
			lines = HttpUtils.fetchUrl(l.getReportUrl());
		} catch (Exception e) {
			NetworkInfo n = cm.getActiveNetworkInfo();
			if (n == null || !n.isConnected()) {
				r._errMsg = c.getString(R.string.error_no_connection);
			} else {
				r._errMsg = e.getLocalizedMessage();
			}
		}

		for (String line : lines) {
			String parts[] = line.split("=", 2);
			if (parts.length == 2) {
				parts[0] = parts[0].trim();
				parts[1] = parts[1].trim();

				if (parts[0].equals("wind.avg")) {
					r._windAvg = parts[1];
				} else if (parts[0].equals("date")) {
					r._date = parts[1];
				} else if (parts[0].equals("details.url")) {
					r._detailsURL = parts[1];
				} else if (parts[0].equals("trails.open")) {
					r._trailsOpen = getInt(parts[1]);
				} else if (parts[0].equals("trails.total")) {
					r._trailsTotal = getInt(parts[1]);
				} else if (parts[0].equals("lifts.open")) {
					r._liftsOpen = getInt(parts[1]);
				} else if (parts[0].equals("lifts.total")) {
					r._liftsTotal = getInt(parts[1]);
				} else if (parts[0].equals("weather.url")) {
					r._weatherUrl = parts[1];
				} else if (parts[0].equals("weather.icon")) {
					r._weatherIcon = parts[1];
				} else if (parts[0].equals("location")) {
					r._location = parts[1];
				} else if (parts[0].equals("location.latitude")) {
					r._latitude = parts[1];
				} else if (parts[0].equals("location.longitude")) {
					r._longitude = parts[1];
				} else if (parts[0].equals("snow.conditions")) {
					r._snowConditions = parts[1];
				} else if (parts[0].equals("err.msg")) {
					r._errMsg = parts[1];
				} else if (parts[0].equals("snow.fresh")) {
					r._freshSnow = parts[1];
				} else if (parts[0].equals("snow.units")) {
					r._snowUnits = parts[1];
				} else {
					String values[] = parts[1].split("\\s+");
					ArrayList<String> vals = toList(values);

					if (parts[0].equals("snow.total")) {
						r._snowTotals = vals;
					} else if (parts[0].equals("snow.daily")) {
						r._dailySnow = vals;
					}

					else if (parts[0].equals("temp.readings")) {
						r._tempReadings = vals;
					} else {
						Log.i(TAG, "Unknown key-value from from report URL("
								+ l.getReportUrl() + " line: " + line);
					}
				}
			} else {
				Log.e(TAG, "Error invalid line from report URL("
						+ l.getReportUrl() + " line: " + line);
			}
		}

		return r;
	}

	public static Report createError(String msg) {
		Report r = new Report();
		r._errMsg = msg;
		return r;
	}

	private static HashMap<String, Integer> _icons;
	static {
		_icons = new HashMap<String, Integer>();

		// nfew = mostly clear, evening
		// nbkn = mostly cloudy, evening
		// nsct = partly cloudy, evening
		_icons.put("nfew", new Integer(R.drawable.moon_parly_cloudy));
		_icons.put("nbkn", new Integer(R.drawable.moon_mostly_cloudy));
		_icons.put("nsct", new Integer(R.drawable.moon_parly_cloudy));

		// few = sunny, day
		// skc = totally sunny
		_icons.put("few", new Integer(R.drawable.sunny));
		_icons.put("skc", new Integer(R.drawable.sunny));

		// sct = mostly sunny, day
		_icons.put("sct", new Integer(R.drawable.mostly_sunny));

		// bkn = partly sunny, day
		// sct = partly cloudy
		_icons.put("bkn", new Integer(R.drawable.partly_sunny));
		_icons.put("sct", new Integer(R.drawable.partly_sunny));

		// nskc = night clear
		_icons.put("nskc", new Integer(R.drawable.moon_clear));

		// nsn = chance of snow, evening
		// sn = chance of snow, day
		// nsn = night snow
		_icons.put("nsn", new Integer(R.drawable.snow));
		_icons.put("sn", new Integer(R.drawable.snow));
		_icons.put("nsn", new Integer(R.drawable.snow));

		// blizzard
		_icons.put("blizzard", new Integer(R.drawable.snow));

		// rasn = chance rain/snow, day (combine rain and snow icons)
		// nrasn = chance rain/snow, night
		// raip = rain / sleet
		_icons.put("rasn", new Integer(R.drawable.rain_snow));
		_icons.put("nrasn", new Integer(R.drawable.rain_snow));
		_icons.put("raip", new Integer(R.drawable.rain_snow));

		// nra = night rain
		// nshra = night showers
		// ra = rain
		_icons.put("ra", new Integer(R.drawable.rain));
		_icons.put("nra", new Integer(R.drawable.rain));
		_icons.put("nshra", new Integer(R.drawable.rain));

		// hi_nshwrs = high night showers
		// hi_shwrs = high day showers showers
		// shra = showers
		_icons.put("hi_nshwrs", new Integer(R.drawable.rain));
		_icons.put("hi_shwrs", new Integer(R.drawable.rain));
		_icons.put("shra", new Integer(R.drawable.rain));

		// ovc = overcast
		// novc = night overcast
		_icons.put("ovc", new Integer(R.drawable.cloudy));
		_icons.put("novc", new Integer(R.drawable.cloudy));

		// hi_ntsra = lighting night
		// hi_tsra = lighting and showers
		// ntsra = lighting night
		// tsra = lighting and rain
		_icons.put("hi_ntsra", new Integer(R.drawable.rain_lightning));
		_icons.put("hi_tsra", new Integer(R.drawable.rain_lightning));
		_icons.put("nscttsra", new Integer(R.drawable.rain_lightning));
		_icons.put("ntsra", new Integer(R.drawable.rain_lightning));
		_icons.put("tsra", new Integer(R.drawable.rain_lightning));

		// scttsra = sun and scattered rain
		_icons.put("scttsra", new Integer(R.drawable.sun_rain));

		// mix = snow and rain
		_icons.put("mix", new Integer(R.drawable.mix));

		// TODO fg = fog
		// fzra = freeze
		// hot = hot
		// hurr = hurricane
		// ntor = night tornado
		// wind
		// nwind = night wind
		// ovc = overcast
		// sctfg = scattered fog
		// cold = cold, day
	}
}
