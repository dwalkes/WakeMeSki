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

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Defines a simple interface that provides the data from which a Report will
 * built from.
 */
public class Location implements Parcelable, Serializable {
	private final String _label;
	private final String _urlPath;

	private static final long serialVersionUID = 0;

	public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
		@Override
		public Location createFromParcel(Parcel source) {
			String label = source.readString();
			String url = source.readString();

			return new Location(label, url);
		}

		@Override
		public Location[] newArray(int size) {
			return new Location[size];
		}
	};

	/**
	 *
	 * @param label A human readable label for this location
	 * @param urlPath Path+file portion of the URL (does NOT include the server)
	 */
	public Location(String label, String urlPath) {
		_label = label;
		_urlPath = urlPath;
	}

	/**
	 * The human-readable label for this location.
	 */
	public String getLabel() {
		return _label;
	}

	/**
	 * The path portion of the URL where the report from the location can be found
	 */
	public String getReportUrlPath() {
		return _urlPath;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(_label);
		dest.writeString(_urlPath);
	}

	@Override
	public String toString() {
		return _label;
	}

	/**
	 * Determine whether the location objects refer to the same location.
	 *
	 * @param loc
	 *            The location object
	 * @return True if location refers to the same location
	 */
	public boolean equals(Location loc) {
		return loc.getLabel().equals(this.getLabel());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Location) {
			return equals((Location) obj);
		}
		return false;
	}
}
