/*
 * Copyright (C) 2010 Dan Walkes, Andy Doan
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
 */
package com.wakemeski;

import java.io.File;

import android.content.Context;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.appender.LogCatAppender;
import com.google.code.microlog4android.format.PatternFormatter;

/**
 * Logger class for WakeMeSki.  Wrapper around microlog4android
 * which provides logging both to file on sdcard and
 */
public class Log {

	public final static String TAG = "WakeMeSki";

	static private Log mLogger = null;
	private final Logger mMicrologLogger;
	private FileAppender mFileAppender = null;

	private Log() {
		mMicrologLogger = LoggerFactory.getLogger();

		/*
		 * Only write prints to logcat if this is a
		 * debug build
		 */
		if( WakeMeSki.DEBUG ) {
			mMicrologLogger.addAppender(new LogCatAppender());
		}
	}

	/**
	 * Sets the context for the logger which will add support for
	 * logging to file
	 * @param c context of the logger
	 */
	public synchronized void setContext(Context c) {
		if( mFileAppender == null ) {
			PatternFormatter formatter = new PatternFormatter();
			/*
			 * Format date message throwable
			 */
			formatter.setPattern("%d{ISO8601} %m %T");
			/*
			 * Always write logs to file
			 */
			mFileAppender= new FileAppender(c);
			mFileAppender.setFormatter(formatter);
			File logFile = mFileAppender.getLogFile();
			boolean append = true;
			if( logFile != null ) {
				if( logFile.length() > 1024*1024) {
					/*
					 * The log file is > 1MB, start a new one
					 */
					append = false;
				}
			}
			mFileAppender.setAppend(append);
			mMicrologLogger.addAppender(mFileAppender);
		}
	}

	public File getLogFile() {
		File logFile=null;
		if( mFileAppender != null ) {
			logFile = mFileAppender.getLogFile();
			if( logFile != null ) {
				if( !logFile.exists() ) {
					logFile=null;
				}
			}
		}
		return logFile;
	}

	static public synchronized Log getInstance() {
		if( mLogger == null ) {
			mLogger = new Log();
		}
		return mLogger;
	}

	static private Logger getMicrologLogger() {
		return getInstance().mMicrologLogger;
	}

	/**
	 * Mimics android.util.Log.d but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void d(String tag, String message) {
		getMicrologLogger().debug(TAG+ ": " + message);
	}

	/**
	 * Mimics android.util.Log.e but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void e(String tag, String message) {
		getMicrologLogger().error(TAG+ ": " + message);
	}

	/**
	 * Mimics android.util.Log.e but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void e(String tag, String message, Throwable t) {
		getMicrologLogger().error(TAG+ ": " + message);
	}

	/**
	 * Mimics android.util.Log.i but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void i(String tag, String message) {
		getMicrologLogger().info(TAG+ ": " + message);
	}

	/**
	 * Mimics android.util.Log.w but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void w(String tag, String message) {
		getMicrologLogger().warn(TAG+ ": " + message);
	}

	/**
	 * Mimics android.util.Log.w but uses microlog
	 * @param tag
	 * @param message
	 */
	public static void w(String tag, String message, Throwable t) {
		getMicrologLogger().warn(TAG+ ": " + message,t);
	}
}
