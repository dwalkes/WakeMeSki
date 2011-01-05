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

/**
 * A class to hold server info for this wakemeski server instance,
 * as found in server_info.php
 */
public class WakeMeSkiServerInfo {
	private int mApMinSupportedVersion = -1;
	private int mApLatestVersion = -1;
	private int mServerVersion = -1;

	private String[] mRegEx;


	public int getApMinSupportedVersion() {
		return mApMinSupportedVersion;
	}

	public int getApLatestVersion() {
		return mApLatestVersion;
	}

	public int getServerVersion() {
		return mServerVersion;
	}

	public String[] getAlertExpressions() {
		return mRegEx;
	}

	public void setApMinSupportedVersion( int version ) {
		mApMinSupportedVersion = version;
	}

	public void setApLatestVersion( int version ) {
		mApLatestVersion = version;
	}

	public void setServerVersion( int version ) {
		mServerVersion = version;
	}

	public void setAlertExpressions(String[] regex) {
		mRegEx = regex;
	}
}
