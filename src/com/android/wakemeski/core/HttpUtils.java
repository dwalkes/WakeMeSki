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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class HttpUtils {
	private static final String TAG = HttpUtils.class.getName();

	/**
	 * list of supported servers
	 */
	public final static String SERVER_LIST[] = {
		"http://bettykrocks.com/skireport",
		"http://ddubtech.com/wakemeski/skireport"
	};

	static HttpClient getHttpClient() {
		return new DefaultHttpClient();
	}

	/**
	 * Returns the contents of the given URL as an array of strings
	 */
	public static String[] fetchUrl(String url) 
		throws ClientProtocolException, IOException {
		ArrayList<String> lines = new ArrayList<String>();

		url = url.replace(" ", "%20");

		HttpResponse resp = getHttpClient().execute(new HttpGet(url));
		if (resp.getStatusLine().getStatusCode() == 200) {
			InputStreamReader ir = new InputStreamReader(resp.getEntity()
					.getContent());
			BufferedReader r = new BufferedReader(ir);

			String line;
			while ((line = r.readLine()) != null)
				lines.add(line);
		} else {
			throw new IOException("LocationFinder: unable to get URL[" + url
					+ "]");
		}

		return lines.toArray(new String[lines.size()]);
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
	 * Get the server version from the wakemeski supported server.  Use this method
	 * to figure out which of the supported wakemeski servers to use in order to 
	 * fulfill a url request.
	 * @param serverUrl The server URL to use when obtaining server version
	 * @return an int value describing the server version, or -1 if the server does not
	 * exist.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static int getServerVersion(String serverUrl)
		throws ClientProtocolException, IOException {
		String url = serverUrl + "/server_info.php";
		int version = -1;

		String rows[] = fetchUrl(url);
		for (String row : rows) {
			String vals[] = row.split("=", 2);
			if(vals.length == 2) {
				vals[0] = vals[0].trim();
				vals[1] = vals[1].trim();
				if(vals[0].equals("server.version")) {
					version = getInt(vals[1]);
				}
			}
		}
		return version;

	}
	
	/**
	 * Returns the server to retrieve location data from.
	 */
	public static String getLocationServer() {
		int maxVersion = -1;
		String latestServer = SERVER_LIST[0] ;
		for (String server: SERVER_LIST) {
			try {
				int version = getServerVersion(server);
				if (version != -1 &&
						version > maxVersion) {
					latestServer = server;
					maxVersion = version;
				}
			} 
			catch (Exception e) {
				Log.i(TAG,"Exception accessing server"+ e.getMessage());
			}
		}
		Log.d(TAG, "Using server " + latestServer);
		return latestServer;
	}
}
