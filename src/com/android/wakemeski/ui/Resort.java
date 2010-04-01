/*
 * Copyright (C) 2010 Dan Walkes
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
package com.android.wakemeski.ui;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.android.wakemeski.core.Location;

/**
 * A class containing resort information destined for persistent storage Uses
 * com.android.wakemeski.core.location as the base location detail.
 * 
 * @author dan
 * 
 */
public class Resort implements Serializable {

	boolean wakeupEnabled = false;
	private Location mLocation;

	/**
	 * @return True when the user has configured a wakeup check on this resort
	 */
	public boolean isWakeupEnabled() {
		return wakeupEnabled;
	}

	/**
	 * @param wakeupEnabled
	 *            true to configure wakeup check for this resort
	 */
	public void setWakeupEnabled(boolean wakeupEnabled) {
		this.wakeupEnabled = wakeupEnabled;
	}

	private static final long serialVersionUID = 0;

	public Resort(String label, String url) {
		mLocation = new Location(label, url);
	}

	public Resort(Location loc) {
		mLocation = loc;
		wakeupEnabled = false;
	}

	public String getResortName() {
		return mLocation.toString();
	}

	public Location getLocation() {
		return mLocation;
	}

	public String toString() {
		return mLocation.toString();
	}

	/**
	 * Finds a resort in the list with matching location
	 * 
	 * @param location
	 *            The location to search for
	 * @param resortList
	 *            A set of resorts to search in
	 * @return null if not found, or a resort with location matching this
	 *         location
	 */
	public static Resort getResortWithLocation(Location location,
			Set<Resort> resortList) {
		Resort matchingResort = null;
		for (Resort resort : resortList) {
			if (location.equals(resort.getLocation())) {
				matchingResort = resort;
				break;
			}
		}
		return matchingResort;
	}

	public static Location[] toLocationList(Resort[] resortList) {
		Set<Location> locationSet = new HashSet<Location>();
		for (Resort resort : resortList) {
			locationSet.add(resort.getLocation());
		}
		return locationSet.toArray(new Location[locationSet.size()]);
	}
}
