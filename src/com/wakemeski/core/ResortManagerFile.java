/*
 * Copyright (C) 2010 Andy Doan, Dan Walkes
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
package com.wakemeski.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import android.content.Context;
import com.wakemeski.Log;

/**
 * Manages a list of resorts to/from persistent storage
 *
 * @author dan
 *
 */
public class ResortManagerFile implements ResortManager {

	private static final String TAG = ResortManagerFile.class.getName();
	private static final String resortFile = "resorts.txt";
	private Context context;
	private static ResortManagerFile instance = null;

	private Resort mResorts[];

	public static synchronized ResortManagerFile getInstance(Context c) {
		if (instance == null) {
			instance = new ResortManagerFile(c);
		}
		return instance;
	}

	private ResortManagerFile(Context c) {
		context = c;
		read();
	}

	/**
	 * Updates the resort list with the passed list, updates persistent storage
	 *
	 * @param resort
	 */
	public void update(Resort[] resortList) {
		mResorts = resortList;
		Arrays.sort(mResorts);
		update();
	}

	private void update() {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
		context.deleteFile(resortFile);
		try {
			fos = context.openFileOutput(resortFile, 0);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(mResorts);
		} catch (Exception e) {
			Log.e(TAG, "Exception " + e + " writing " + resortFile);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException ioe) {
					Log.w(TAG, "IO exception " + ioe + " closing output file "
							+ resortFile);
				}
			}
		}
	}

	private void read() {

		FileInputStream fis;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput(resortFile);
			ois = new ObjectInputStream(fis);
			mResorts = (Resort[]) ois.readObject();
			Arrays.sort(mResorts);
		} catch (FileNotFoundException fnf) {
			Log.w(TAG, "File " + resortFile
					+ " not found setting selected resorts");
			// leave selected resorts list empty
		} catch (Exception e) {
			Log.e(TAG, "Exception " + e + " reading " + resortFile);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException ioe) {
					Log.w(TAG, "IO exception closing input file " + resortFile);
				}
			}
		}
	}

	/**
	 * @return The current resort list from persistent storage
	 */
	public Resort[] getResorts() {
		if( mResorts == null ) {
			mResorts = new Resort[0];
		}
		return mResorts;
	}

}
