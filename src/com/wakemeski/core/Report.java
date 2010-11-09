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
package com.wakemeski.core;

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

import com.wakemeski.R;
import com.wakemeski.pref.SnowSettingsSharedPreference;

public class Report implements Parcelable {

	private String _location = "";
	private String _date = "";
	private String _windAvg = "";
	private String _detailsURL = "";
	private String _freshSourceUrl = "";
	private String _locationURL = "";
	private String _locationComments = "";

	private int _trailsOpen = 0;
	private int _trailsTotal = 0;
	/*
	 * Some reports indicate trails open as percentage only (ie the Euro resorts)
	 */
	private String _trailsPercentOpen = "";

	private int _liftsOpen = 0;
	private int _liftsTotal = 0;

	private String _weatherUrl = "";
	private String _weatherIcon = "";
	private ArrayList<Weather> _weather = new ArrayList<Weather>();

	private String _latitude = "";
	private String _longitude = "";

	private String _snowConditions = "";

	private ArrayList<String> _snowTotals = new ArrayList<String>();
	private ArrayList<String> _dailySnow = new ArrayList<String>();
	private ArrayList<String> _tempReadings = new ArrayList<String>();
	private String _freshSnow = "";
	private String _snowUnits = "inches";
	private String _requestUrl = "";
	private WakeMeSkiServerInfo _serverInfo = new WakeMeSkiServerInfo();

	private Resort _resort;

	// Used to display an error if one occurred
	private String _errMsgLocalized = "";
	
	// The error message from the server, will not be localized
	private String _errMsgServer = "";
	
	private static final String TAG = "com.wakemeski.core.Report";

	public static final Parcelable.Creator<Report> CREATOR = new Parcelable.Creator<Report>() {
		@Override
		public Report createFromParcel(Parcel source) {
			Report r = new Report();

			r._location = source.readString();
			r._date = source.readString();
			r._windAvg = source.readString();
			r._detailsURL = source.readString();
			r._locationURL = source.readString();
			r._locationComments = source.readString();

			r._trailsOpen = source.readInt();
			r._trailsTotal = source.readInt();
			r._trailsPercentOpen = source.readString();

			r._liftsOpen = source.readInt();
			r._liftsTotal = source.readInt();

			r._resort = (Resort)source.readSerializable();
			r._errMsgLocalized = source.readString();
			r._errMsgServer = source.readString();

			r._weatherUrl = source.readString();
			r._weatherIcon = source.readString();
			source.readTypedList(r._weather, Weather.CREATOR);

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

	public boolean meetsPreference(SnowSettingsSharedPreference s) {
		double depth = s.getSnowDepth();
		double reported = getFreshSnowTotal();

		if (getSnowUnits() == SnowUnits.CENTIMETERS)
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
	 * Returns the resort the report is for
	 */
	public Resort getResort() {
		return _resort;
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

	public String getTrailsAsString() {
		String s = "n/a";
		if( _trailsTotal > 0 )
			s = _trailsOpen + "/" + _trailsTotal;
		else if( _trailsOpen > 0 )
			s = String.valueOf(_trailsOpen);
		else if( _trailsPercentOpen.length() != 0 ) 
			s = _trailsPercentOpen;

		return s;
	}

	public int getLiftsOpen() {
		return _liftsOpen;
	}

	public int getLiftsTotal() {
		return _liftsTotal;
	}

	public String getLiftsAsString() {
		String s = "n/a";
		if( _liftsTotal > 0 )
			s = _liftsOpen + "/" + _liftsTotal;
		else if( _liftsOpen > 0 )
			s = String.valueOf(_liftsOpen);

		return s;
	}

	/**
	 * Returns an array of total snow depths read on the mountain
	 */
	public String[] getSnowDepths() {
		return _snowTotals.toArray(new String[_snowTotals.size()]);
	}

	public String getSnowDepthsAsString() {
		StringBuffer sb = new StringBuffer();
		int len = _snowTotals.size();

		for(int i = 0; i < len; i++) {
			sb.append(_snowTotals.get(i));
			if( i+2< len)
				sb.append(',');
			sb.append(' ');
		}

		return sb.toString();
	}

	/**
	 * Returns an array of all daily snow reports found
	 */
	public String[] getDailySnow() {
		return _dailySnow.toArray(new String[_dailySnow.size()]);
	}

	/**
	 * Returns the list of daily snow fall plus snow conditions if available
	 */
	public String getDailyDetails() {
		StringBuffer sb = new StringBuffer();
		int len = _dailySnow.size();

		for(int i = 0; i < len; i++) {
			sb.append(_dailySnow.get(i));
			if( i+2< len)
				sb.append(',');
			sb.append(' ');
		}

		if(_snowConditions != null && _snowConditions.length() > 0 )
			sb.append(_snowConditions);

		return sb.toString();
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

	/**
	 * @return A string representing the snow total with units, or N/A if not available
	 */
	public String getFreshAsString() {
		String unit = " \"";
		String snowTotal;
		if (getSnowUnits() == SnowUnits.CENTIMETERS)
			unit = " cm";

		if( hasFreshSnowTotal() ) {
			snowTotal = getFreshSnowTotal() + unit;
		} else {
			snowTotal = "N/A";
		}
		return snowTotal;
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
	 * @return the localized error if one occurred. Note that hasErrors() does
	 * not mean this string will be length > 0.  See hasServerError()
	 */
	public String getLocalizedError() {
		return _errMsgLocalized;
	}
	
	/**
	 * @return any localized error message + any message obtained from the server
	 */
	public String getNonLocalizedError() {
		return _errMsgLocalized + _errMsgServer;
	}
	
	/**
	 * @return true if an error condition was found on the server.  In this
	 * case the error message will not be localized.  It basically means we
	 * know about the error and we are working on it.  Our test scripts will catch
	 * it and print the non-localized version with getNonLocalizedError()
	 */
	public boolean hasServerError() {
		return _errMsgServer.length() != 0;
	}
	
	/**
	 * @return true if hasServerError() or has a localized error available
	 * with getLocalizedError()
	 */
	public boolean hasErrors() {
		return ( (_errMsgLocalized.length() != 0 ) ||
				 (_errMsgServer.length() != 0 	 ) 
				 );
	}

	public WakeMeSkiServerInfo getServerInfo() {
		return _serverInfo;
	}
	
	/**
	 * Returns true if the report include latitude and longitude coordinates
	 */
	public boolean hasGeo() {
		if( _latitude != null && _latitude.length() > 0 &&
			_longitude != null && _longitude.length() > 0 )
			return true;
		return false;
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

	/**
	 * Similar to getDetailsURL. This is more for the resport web site, whereas
	 * the getDetailsURL is more for drilling down into snow information
	 */
	public String getLocationURL() {
		return _locationURL;
	}

	/**
	 * @return true if this report contains location comments available in
	 * getLocationComments()
	 */
	public boolean hasLocationComments() {
		return _locationComments.length() != 0 ;
	}
	/**
	 * An optional field some feeds provide that give information on the resort.
	 * Its usually something like "Come Saturday for Elvis Presley Day!"
	 */
	public String getLocationComments() {
		return _locationComments;
	}

	/**
	 * @return The url where "fresh" snow information is obtained by the PHP parsing script
	 */
	public String getFreshSourceURL() {
		return _freshSourceUrl;
	}
	
	/**
	 * @return The string URL requested of a wakemeski server to build this report
	 */
	public String getRequestURL() {
		return _requestUrl;
	}
	
	public String getWeatherURL() {
		return _weatherUrl;
	}

	public Weather[] getWeather() {
		return _weather.toArray(new Weather[_weather.size()]);
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
		dest.writeString(_locationURL);
		dest.writeString(_locationComments);

		dest.writeInt(_trailsOpen);
		dest.writeInt(_trailsTotal);
		dest.writeString(_trailsPercentOpen);

		dest.writeInt(_liftsOpen);
		dest.writeInt(_liftsTotal);

		dest.writeSerializable(_resort);
		dest.writeString(_errMsgLocalized);
		dest.writeString(_errMsgServer);

		dest.writeString(_weatherUrl);
		dest.writeString(_weatherIcon);
		dest.writeTypedList(_weather);

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
		/*
		 * Don't bother to try parsing if no value is returned or if
		 * n/a is returned.
		 */
		if (val.length() != 0 &&
				!val.equals("n/a") ) {
			try {
				v = Integer.parseInt(val);
			} catch (Throwable t) {
				Log.e(TAG, "Unable to parse value to int: " + val);
			}
		}

		return v;
	}

	/**
	 * Loads a report from the given location with default URL
	 */
	public static Report loadReport(Context c, ConnectivityManager cm,
		Resort resort, WakeMeSkiServer server)
	{
		return loadReportWithAppendUrl(c,cm,resort,server,"");
	}
	
	/**
	 * Loads a report from the given location without caching
	 */
	public static Report loadReportNoCache(Context c, ConnectivityManager cm,
			Resort resort, WakeMeSkiServer server)
	{
		return loadReportWithAppendUrl(c,cm,resort,server,"&nocache=1");
	}
	

	/**
	 * Loads a report.  Allows specifying custom append values to the URL request
	 * (such as nocache=1)
	 * @return
	 */
	private static Report loadReportWithAppendUrl(Context c, ConnectivityManager cm,
			Resort resort, WakeMeSkiServer server, String appendUrl) {
		
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
		r._resort = resort;

		Location l = resort.getLocation();

		String lines[] = new String[0];

		String url = "/" + l.getReportUrlPath() 
						+ appendUrl;
		r._requestUrl = server.getFetchUrl(url);
		try {
			lines = server.fetchUrlWithID(url);
		} catch (Exception e) {
			NetworkInfo n = cm.getActiveNetworkInfo();
			if (n == null || !n.isConnected()) {
				r._errMsgLocalized = c.getString(R.string.error_no_connection);
			} else {
				r._errMsgLocalized = e.getLocalizedMessage();
			}
			return r;
		}

		String when[] = new String[3];
		String desc[] = new String[3];
		for (String line : lines) {
			String parts[] = line.split("=", 2);
			if (parts.length == 2) {
				parts[0] = parts[0].trim();
				parts[1] = parts[1].trim();
				if( parts[0].equals("fresh.source.url")) {
					r._freshSourceUrl = parts[1];
				} else if (parts[0].equals("wind.avg")) {
					r._windAvg = parts[1];
				} else if (parts[0].equals("date")) {
					r._date = parts[1];
				} else if (parts[0].equals("details.url")) {
					r._detailsURL = parts[1];
				} else if (parts[0].equals("location.info")) {
					r._locationURL = parts[1];
				} else if (parts[0].equals("location.comments")) {
					r._locationComments = parts[1];
				} else if (parts[0].equals("trails.open")) {
					r._trailsOpen = getInt(parts[1]);
				} else if (parts[0].equals("trails.total")) {
					r._trailsTotal = getInt(parts[1]);
				} else if (parts[0].equals("trails.percent.open")) {
					r._trailsPercentOpen = parts[1];
				} else if (parts[0].equals("lifts.open")) {
					r._liftsOpen = getInt(parts[1]);
				} else if (parts[0].equals("lifts.total")) {
					r._liftsTotal = getInt(parts[1]);
				} else if (parts[0].equals("weather.url")) {
					r._weatherUrl = parts[1];
				} else if (parts[0].equals("weather.icon")) {
					r._weatherIcon = parts[1];
				} else if( parts[0].startsWith("weather.forecast.when.")) {
					int idx = Integer.parseInt(parts[0].substring(parts[0].length()-1));
					if(idx < when.length) {
						when[idx] = parts[1];
					}
				} else if( parts[0].startsWith("weather.forecast.desc.")) {
					int idx = Integer.parseInt(parts[0].substring(parts[0].length()-1));
					if(idx < desc.length) {
						desc[idx] = parts[1];
					}
				}
				else if (parts[0].equals("location")) {
					r._location = parts[1];
				} else if (parts[0].equals("location.latitude")) {
					r._latitude = parts[1];
				} else if (parts[0].equals("location.longitude")) {
					r._longitude = parts[1];
				} else if (parts[0].equals("snow.conditions")) {
					r._snowConditions = parts[1];
				} else if (parts[0].equals("err.msg")) {
					r._errMsgServer = parts[1];
				} else if (parts[0].equals("err.msg.localized")) {
					/*
					 * Include a method to set a localized error message from the
					 * server, doubtful that we'd want to do this but never say never
					 */
					r._errMsgLocalized = parts[1];
				}
				else if (parts[0].equals("snow.fresh")) {
					r._freshSnow = parts[1];
				} else if (parts[0].equals("snow.units")) {
					r._snowUnits = parts[1];
				} else if (parts[0].equals("cache.found")) {
					// do nothing, but avoid warning about unknown key-value pair.
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
								+ l.getReportUrlPath() + " line: " + line);
					}
				}
			} else {
				Log.e(TAG, "Error invalid line from report URL("
						+ l.getReportUrlPath() + " line: " + line);
			}
		}

		for( int i = 0; i < when.length && i < desc.length ; i++ ) {
			if( when[i] != null && when[i].length() > 0 &&
					desc[i] != null && desc[i].length() > 0 )
				r._weather.add(new Weather(when[i], desc[i]));
		}

		r._serverInfo = server.getServerInfo();
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
		
		_icons.put("nwind", new Integer(R.drawable.night_wind));

		// TODO fg = fog
		// fzra = freeze
		// hot = hot
		// hurr = hurricane
		// ntor = night tornado
		// wind
		// ovc = overcast
		// sctfg = scattered fog
		// cold = cold, day
	}
}
