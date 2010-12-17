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
package com.wakemeski.core.alert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.wakemeski.core.Location;
import com.wakemeski.core.Report;
import com.wakemeski.core.Weather;

//TODO's:
//add an aknowledged function that will mark all current alerts
//add a function to see if there's any alerts not acknowedged(for the notification bar)
//fix the query command when a resort no longer has alerts

/**
 * Keeps track of snow alerts for a given resort. Each resort can have a list of
 * weather reports that describe up-coming snow conditions the user is
 * interested in.
 */
public class AlertManager {

	private static final long SIX_HOURS = 6 * 60 * 60 * 1000;

	private SQLiteDatabase mDB;

	private SQLiteStatement mInsertResort;
	private SQLiteStatement mInsertAlert;
	private SQLiteStatement mRemoveOld;

	public AlertManager(Context c) {
		DBHelper h = new DBHelper(c);
		mDB = h.getWritableDatabase();

		mInsertResort = mDB
				.compileStatement("INSERT INTO resorts (label,url) values (?, ?)");
		mInsertAlert = mDB
				.compileStatement("INSERT INTO alerts (time,desc,acked,resort) values (?, ?, ?, ?)");
		mRemoveOld = mDB.compileStatement("DELETE FROM alerts WHERE time<?");
	}

	/**
	 * Returns the resort ID or creates a entry
	 */
	private long getResortID(Location l) {

		String query = "label = \"" + l.getLabel() +"\" AND url = \"" + l.getReportUrlPath() +"\"";
		Cursor c = mDB.query("resorts", new String[] {"_id"}, query, null, null, null, null);
		if( c.getCount() == 1 && c.moveToFirst() ) {
			long id = c.getLong(0);
			c.close();
			return id;
		}
		c.close();

		mInsertResort.bindString(1, l.getLabel());
		mInsertResort.bindString(2, l.getReportUrlPath());
		return mInsertResort.executeInsert();
	}

	private long findAlert(long time, long resortId) {
		long id = -1;
		String query = "time = \"" + time +"\" AND resort = \"" + resortId +"\"";
		Cursor c = mDB.query("alerts", new String[] {"_id"}, query, null, null, null, null);
		if( c.getCount() == 1 && c.moveToFirst() )
			id = c.getLong(0);
		c.close();

		return id;
	}

	public void updateAlert(long id, Weather w) {
		ContentValues alert = new ContentValues();
	    alert.put("desc", w.getDesc());

		mDB.update("alerts", alert, "_id=?", new String[] {Long.toString(id)});
	}

	private void insertAlert(Weather w, long resortId) {
		mInsertAlert.bindLong(1, w.getExact());
		mInsertAlert.bindString(2, w.getDesc());
		mInsertAlert.bindLong(3, 0);
		mInsertAlert.bindLong(4, resortId);
		mInsertAlert.executeInsert();
	}

	public void addAlerts(Report r) {
		for (Weather w : r.getWeather()) {
//			if (w.hasSnowAlert()) {
				long rid = getResortID(r.getResort().getLocation());

				long wid = findAlert(w.getExact(), rid);
				if( wid == -1 )
					insertAlert(w, rid);
				else
					updateAlert(wid, w);
			}
//		}
	}

	/**
	 * Returns all the resorts that have alerts
	 */
	public Cursor getAlertResorts() {
		//TODO this should only return resorts that have alerts
		//Cursor c = mDB.query("view_resorts", null, null, null, null, null, null);
		return mDB.query("resorts", null, null, null, null, null, "label");
	}

	public Cursor getAlerts(long resortId) {
		String query = "resort = \"" + resortId + "\"";
		Cursor c = mDB.query("alerts", null, query,
							null, null, null, "resort, time", null);
		c.moveToFirst();
		return c;
	}
	/**
	 * Removes any alerts that are over a certain age
	 */
	public void removeOld() {
		mRemoveOld.bindLong(1, System.currentTimeMillis() - SIX_HOURS);
		mRemoveOld.execute();
	}

	private class DBHelper extends SQLiteOpenHelper {
		private final static String DB_NAME = "alerts.db";
		private final static int DB_VERSION = 1;

		DBHelper(Context c) {
			super(c, DB_NAME, null, DB_VERSION);

//			onUpgrade(getWritableDatabase(), 1, 2);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE resorts " + "(_id INTEGER PRIMARY KEY, "
					+ "label TEXT, url TEXT)");
			db.execSQL("CREATE TABLE alerts " + "(_id INTEGER PRIMARY KEY, "
					+ "time INTEGER, " + "desc TEXT, " + "acked INTEGER, " + "resort INTEGER,"
					+ "FOREIGN KEY(resort) REFERENCES resort(_id) )");

//			db.execSQL("CREATE VIEW view_resorts AS " +
//			   "SELECT _id, label, url FROM resorts INNER JOIN alerts ON resorts._id = alerts.resort GROUP BY resorts._id ORDER BY resorts.label;");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS alerts");
			db.execSQL("DROP TABLE IF EXISTS resorts");
			onCreate(db);
		}
	}
}
