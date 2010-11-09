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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.client.ClientProtocolException;

import android.util.Log;

/**
 * This class helps find a location and its corresponding URL from which it can
 * get the ski report.
 */
public class LocationFinder {
	private static final String TAG = "LocationFinder";
	private WakeMeSkiServer mServer;

	public LocationFinder(WakeMeSkiServer server) {
		mServer = server;
	}
	
	/**
	 * Gets the top-level regions to start the search for a location from.
	 */
	public String[] getRegions()
			throws ClientProtocolException, IOException {
		String [] regions = mServer.fetchUrl("/location_finder.php");
		Arrays.sort(regions);
		return regions;
	}

	/**
	 * Returns the location objects associated with a given region.
	 */
	public Location[] getLocations(String region)
			throws ClientProtocolException, IOException {
		ArrayList<Location> locations = new ArrayList<Location>();

		String rows[] = mServer.fetchUrlWithID("/location_finder.php?region=" + region);
		Arrays.sort(rows);
		for (String row : rows) {
			String vals[] = row.split("=", 2);
			if (vals.length == 2) {
				Location l = new Location(vals[0].trim(), vals[1].trim());
				locations.add(l);
			} else {
				Log.e(TAG, "Bad location line for region [" + region + "]: ["
						+ row + "]");
			}
		}
		return locations.toArray(new Location[locations.size()]);
	}
}
