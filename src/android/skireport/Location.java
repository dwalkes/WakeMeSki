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
package android.skireport;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Defines a simple interface that provides the data
 * from which a Report will built from.
 */
public class Location implements Parcelable, Serializable
{
	private String _label;
	private String _url;
	
	private static final long serialVersionUID = 0;
	
	public static final Parcelable.Creator<Location> CREATOR
		= new Parcelable.Creator<Location>()
	{
		@Override
		public Location createFromParcel(Parcel source)
		{
			String label = source.readString();
			String url = source.readString();
			
			return new Location(label, url);
		}

		@Override
		public Location[] newArray(int size)
		{
			return new Location[size];
		}
	};
	
	public Location(String label, String url)
	{
		_label = label;
		_url = url;
	}

	/**
	 * The human-readable label for this location.
	 */
	public String getLabel()
	{
		return _label;
	}
	
	/**
	 * The URL where the report from the location can be found
	 */
	public String getReportUrl()
	{
		return _url;
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(_label);
		dest.writeString(_url);
	}
	
	@Override
	public String toString()
	{
		return _label;
	}
	
	/**
	 * Determine whether the location objects refer to the same location.
	 * @param loc The location object
	 * @return True if location refers to the same location
	 */
	public boolean equals( Location loc ) 
	{
		return loc.getLabel() == this.getLabel();
	}

	public boolean equals (Object obj ) 
	{
		if( obj instanceof Location ) {
			return equals((Location)obj);
		}
		return false;
	}
}
