 /* 
 * Copyright (c) 2010 Dan Walkes, Andy Doan
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

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * Wrapper around HttpUtils.getLocationServer() that allows caching a 
 * server location
 */
public class WakeMeSkiServer {
	private static final String TAG = "WakeMeSkiServer";
	
	private String mID = null;
	
	private String mServerUrl=null;
	
	private WakeMeSkiServerInfo mServerInfo=null;
	
	/**
	 * list of supported servers
	 */
	public final static String SERVER_LIST[] = {
		"http://bettykrocks.com/skireport",
		"http://wakemeski.com/skireport"
	};
	
	public WakeMeSkiServer(Context c) {
		mServerInfo = null;
		initID(c.getContentResolver());	
	}
	
	protected WakeMeSkiServer(Context c, String serverUrl) {
		initID(c.getContentResolver());	
		mServerUrl = serverUrl;
	}

	private void initID(ContentResolver cr) {
		mID = Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
		if( mID == null )
			mID = "unknown";
	}

	/**
	 * @return The server URL as obtained from HttpUtils as necessary or 
	 * cached version from previous request
	 */
	private synchronized String getServerUrl() {
		if ( mServerUrl == null ) {
			mServerUrl = findServerUrl();
		}
		return mServerUrl;
	}
	
	public String toString() {
		return getServerUrl();
	}

	/**
	 * Fets the full url used to query the server
	 * @param url URL to append to server default URL
	 * @return the full URL used to query the server with fetchUrl()
	 */
	public String getFetchUrl( String url ) {
		return getServerUrl() + url;
	}
	
	/**
	 * Fetch the URL from the server by prefixing url with
	 * the server URL
	 * @param url request for server
	 * @return String[] result of fetch URL
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String[] fetchUrl( String url )
		throws ClientProtocolException, IOException {
		return HttpUtils.fetchUrl(getFetchUrl(url));
	}

	public String getId() {
		return mID;
	}
	
	/**
	 * Same as the fetchURL but adds the phone's unique ID to the query
	 * string so the server can track usage patterns
	 */
	public String[] fetchUrlWithID(String url)
		throws ClientProtocolException, IOException {
		return fetchUrl(url + "&id=" + getId());
	}
	
	/**
	 * Simple wrapper to Integer.parseInt that will catch format errors
	 */
	private static int getInt(String val) {
		int v = -1;
		try {
			v = Integer.parseInt(val);
		} catch (Throwable t) {
			Log.e(TAG, "Unable to parse value to int: " + val);
		}

		return v;
	}
	
	/**
	 * Get the server info for the given server URL
	 * @param serverUrl Server to query for server info
	 * @return a WakeMeSkiServerInfo object with either default values or values
	 * for this server.
	 */
	private WakeMeSkiServerInfo getServerInfo(String serverUrl) {
		String url = serverUrl + "/server_info.php";
		WakeMeSkiServerInfo serverInfo = new WakeMeSkiServerInfo();
		try
		{
			String rows[] = HttpUtils.fetchUrl(url);
			for (String row : rows) {
				String vals[] = row.split("=", 2);
				if(vals.length == 2) {
					vals[0] = vals[0].trim();
					vals[1] = vals[1].trim();
					if(vals[0].equals("server.version")) {
						serverInfo.setServerVersion(getInt(vals[1]));
					} else if(vals[0].equals("ap.min.supported.version")) {
						serverInfo.setApMinSupportedVersion(getInt(vals[1]));
					} else if(vals[0].equals("ap.latest.version")) {
						serverInfo.setApLatestVersion(getInt(vals[1]));
					}
				}
			}
		}
		catch ( ClientProtocolException ce ) {
			Log.i(TAG,"Exception getting server info for server " + serverUrl + ce.getMessage());
		} 
		catch ( IOException ioe ) {
			Log.i(TAG,"Exception getting server info for server " + serverUrl + ioe.getMessage());
		}
		return serverInfo;

	}


	/**
	 * @return The server info for this server instance (defaults if server info could not be obtained)
	 */
	public synchronized WakeMeSkiServerInfo getServerInfo() {
		if( mServerInfo == null ) {
			mServerInfo = getServerInfo(getServerUrl());
		}
		return mServerInfo;
	}
	
	/**
	 * Looks for the latest of the two servers in the server list, or defaults to 
	 * using the first server in the list if both are at the same version or both
	 * are unreachable.
	 * @return A string with the URL which should be used with this server
	 */
	private synchronized String findServerUrl() {
		WakeMeSkiServerInfo latestServerInfo = new WakeMeSkiServerInfo();
		String latestServerUrl = SERVER_LIST[0] ;
		for (String serverUrl: SERVER_LIST) {
			WakeMeSkiServerInfo serverInfo = getServerInfo(serverUrl);
			try {
				if (serverInfo.getServerVersion() != -1 &&
						serverInfo.getServerVersion() > latestServerInfo.getServerVersion()) {
					latestServerUrl = serverUrl;
					latestServerInfo = serverInfo;
				}
			}
			catch (Exception e) {
				Log.i(TAG,"Exception getting server info for server " + serverUrl + e.getMessage());
			}
		}
		mServerInfo = latestServerInfo;
		return latestServerUrl;
	}
	
	
}
