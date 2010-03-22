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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.SharedPreferences;

public class HttpUtils
{
	public final static String LOC_SERVER = "http://bettykrocks.com/skireport";

	static HttpClient getHttpClient()
	{
		return new DefaultHttpClient();
	}
	
	/**
	 * Returns the contents of the given URL as an array of strings
	 */
	public static String[] fetchUrl(String url) 
		throws ClientProtocolException, IOException
	{
		ArrayList<String> lines = new ArrayList<String>();
		
		url = url.replace(" ", "%20");
		
		HttpResponse resp = getHttpClient().execute(new HttpGet(url));
		if( resp.getStatusLine().getStatusCode() == 200 )
		{
			InputStreamReader ir = 
				new InputStreamReader(resp.getEntity().getContent());
			BufferedReader r = new BufferedReader(ir);
			
			String line;
			while( (line = r.readLine()) != null )
				lines.add(line);
		}
		else
		{
			throw new IOException("LocationFinder: unable to get URL[" + url +"]");
		}
		
		return lines.toArray(new String[lines.size()]);
	}
	
	/**
	 * Returns the server to retrieve location data from.
	 * @return
	 */
	public static String getLocationServer(SharedPreferences pref)
	{
		return pref.getString("location_server", LOC_SERVER);
	}
}
