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

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.wakemeski.Log;
import com.wakemeski.R;
import com.wakemeski.core.Location;
import com.wakemeski.core.Report;
import com.wakemeski.core.Resort;
import com.wakemeski.core.WakeMeSkiServer;
import com.wakemeski.core.Weather;
import com.wakemeski.pref.SnowSettingsSharedPreference;
import com.wakemeski.ui.AlertsActivity;
import com.wakemeski.ui.WakeMeSkiPreferences;

/**
 * Keeps track of snow alerts for a given resort. Each resort can have a list of
 * weather reports that describe up-coming snow conditions the user is
 * interested in.
 */
public class AlertManager {

	public static final int NOTIFICATION_ID = R.drawable.snow;

	private static final long SIX_HOURS = 6 * 60 * 60;

	private final Context mContext;
	private final SQLiteDatabase mDB;

	private SnowSettingsSharedPreference mNotifySnowSettings = null;

	private SharedPreferences mSharedPreferences = null;

	private SharedPreferences getSharedPreferences() {
		if( mSharedPreferences == null ) {
			mSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(mContext);
		}
		return mSharedPreferences;
	}

	/**
	 * @return the snow settings shared preference for alert notifications
	 */
	private SnowSettingsSharedPreference getNotifySnowSettings() {
		if( mNotifySnowSettings == null ) {
			mNotifySnowSettings = SnowSettingsSharedPreference.getNotifyPreference();
			mNotifySnowSettings.setFromPreferences(getSharedPreferences());
		}
		return mNotifySnowSettings;
	}

	/**
	 * @return true when user has enabled notification in preferences
	 */
	private boolean isNotificationEnabled() {
		return getSharedPreferences().getBoolean(WakeMeSkiPreferences.NOTIFY_ENABLE_PREF_KEY, false);
	}

	public AlertManager(Context c) {
		mContext = c;
		DBHelper h = new DBHelper(c);
		mDB = h.getWritableDatabase();

	}

	public void close() {
		mDB.close();
	}

	/**
	 * Returns the alert description column from a cursor coming from the
	 * alerts table.
	 */
	public static String getAlertDesc(Cursor alertCursor) {
		return alertCursor.getString(2);
	}

	/**
	 * Returns a string representation of an alert's time column.
	 */
	public static CharSequence getAlertTime(Cursor alertCursor) {
		long epochMS = alertCursor.getLong(1) * 1000;
		return DateFormat.format("EEE ha", epochMS);
	}

	/**
	 * Returns a string representation of the resort's label column.
	 */
	public static String getResortLabel(Cursor resortCursor) {
		return resortCursor.getString(1);
	}

	/**
	 * @param l
	 * @return the existing resort ID for this location, or NULL if it does not exist
	 */
	private Long getExistingResortId(Location l) {
		Long resortId=null;
		String query = "label = \"" + l.getLabel() +"\" AND url = \"" + l.getReportUrlPath() +"\"";
		Cursor c = mDB.query("resorts", new String[] {"_id"}, query, null, null, null, null);
		if( c.getCount() == 1 && c.moveToFirst() ) {
			resortId = c.getLong(0);
		}
		c.close();
		return resortId;
	}

	/**
	 * Returns the resort ID or creates a entry
	 */
	private long getResortID(Location l) {

		Long id = getExistingResortId(l);

		if( id == null ) {
			SQLiteStatement insertResort = mDB
						.compileStatement("INSERT INTO resorts (label,url) values (?, ?)");

			insertResort.bindString(1, l.getLabel());
			insertResort.bindString(2, l.getReportUrlPath());
			id = insertResort.executeInsert();
			insertResort.close();
		}
		return id;
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
		SQLiteStatement insertAlert = mDB
			.compileStatement("INSERT INTO alerts (time,desc,acked,resort) values (?, ?, ?, ?)");


		insertAlert.bindLong(1, w.getExact());
		insertAlert.bindString(2, w.getDesc());
		insertAlert.bindLong(3, 0);
		insertAlert.bindLong(4, resortId);
		insertAlert.executeInsert();
		insertAlert.close();
	}

	public void addAlerts(Report r, WakeMeSkiServer server) {
		SnowSettingsSharedPreference prefs = getNotifySnowSettings();
		for (Weather w : r.getWeather()) {
			if (w.hasSnowAlert(prefs, server)) {
				long rid = getResortID(r.getResort().getLocation());

				long wid = findAlert(w.getExact(), rid);
				if( wid == -1 )
					insertAlert(w, rid);
				else
					updateAlert(wid, w);
			}
		}
	}

	/**
	 * Returns all the resorts that have alerts
	 */
	public Cursor getAlertResorts() {
		return mDB.query("view_resorts", null, null, null, null, null, null);
	}

	public Cursor getAlerts(long resortId) {
		String query = "resort = \"" + resortId + "\"";
		Cursor c = mDB.query("alerts", null, query,
							null, null, null, "resort, time", null);
		c.moveToFirst();
		return c;
	}

	public void acknowledgeAlerts() {
		SQLiteStatement ackAll = mDB
		.compileStatement("UPDATE alerts SET acked=1 WHERE acked=0");

		ackAll.execute();
		ackAll.close();

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager)mContext.getSystemService(ns);
		nm.cancel(NOTIFICATION_ID);
	}

	/**
	 * Removes any alerts that are over a certain age
	 */
	public void removeOld() {
		//the timestamps are in seconds since epoch
		long thresh = (System.currentTimeMillis()/1000) - SIX_HOURS;
		SQLiteStatement removeOld = mDB.compileStatement("DELETE FROM alerts WHERE time<?");

		removeOld.bindLong(1, thresh);
		removeOld.execute();
		removeOld.close();
	}

	/**
	 * Removes all alerts and resorts from the database
	 */
	public void removeAll() {
		Log.d("Removing all resorts & alerts");
		SQLiteStatement removeAllAlerts= mDB.compileStatement("DELETE FROM alerts");
		removeAllAlerts.execute();
		removeAllAlerts.close();

		SQLiteStatement removeAllResorts= mDB.compileStatement("DELETE FROM resorts");
		removeAllResorts.execute();
		removeAllResorts.close();

	}

	/**
	 * Removes all resorts for a particular resort from the database
	 * @param r
	 */
	public void removeResort(Resort r) {
		Long resId = getExistingResortId(r.getLocation());
		if( resId != null ) {
			Log.d("Removing resort " + r);
			SQLiteStatement removeResortAlerts = mDB.compileStatement("DELETE FROM alerts WHERE resort=?");
			removeResortAlerts.bindLong(1, resId);
			removeResortAlerts.execute();

			SQLiteStatement removeResorts = mDB.compileStatement("DELETE FROM resorts WHERE _id=?");
			removeResorts.bindLong(1, resId);
			removeResorts.execute();
		} else {
			Log.d("Attempt to remove not present resort " +r);
		}
	}
	/**
	 * Returns a list of resort IDs for each alert not yet acked
	 */
	private List<Long> getUnacknowledgedIds() {
		List<Long> l = new ArrayList<Long>();
		Cursor c = mDB.query("alerts", new String[]{"resort"}, "acked=0", null, null, null, null);
		c.moveToFirst();
		while( !c.isAfterLast() ) {
			l.add(c.getLong(0));
			c.moveToNext();
		}
		c.close();
		return l;
	}

	private String getResortLabel(long id) {
		String label = "";
		Cursor c = mDB.query("resorts", new String[]{"label"}, "_id="+id, null, null, null, null);
		if( c.getCount() == 1 && c.moveToFirst())
			label = c.getString(0);
		c.close();
		return label;
	}

	/**
	 * Creates an alert in the status bar. If there are more than one reports
	 * with snow, it gives a summary of the number otherwise it displays the
	 * name of the resort
	 */
	public void handleNotifications() {
		List<Long>ids = getUnacknowledgedIds();

		if( ids.size() <= 0 )
			return;

		/*
		 * Don't do anything if notifications are disabled
		 */
		if( !isNotificationEnabled() ) {
			return;
		}

		int icon = R.drawable.snow_alert;
		CharSequence tickerTitle = mContext.getString(R.string.ticker_title);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerTitle, when);

		Intent i;
		CharSequence tickerMsg;

		if( ids.size() == 1) {
			String resort = getResortLabel(ids.get(0));
			tickerMsg = mContext.getString(R.string.ticker_msg, resort);
		} else {
			tickerMsg = mContext.getString(R.string.ticker_msg_multi, ids.size());
		}
		i = new Intent(mContext, AlertsActivity.class);

		PendingIntent pi = PendingIntent.getActivity(mContext, 0, i, 0);
		notification.setLatestEventInfo(mContext, tickerTitle, tickerMsg, pi);

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager)mContext.getSystemService(ns);
		nm.notify(NOTIFICATION_ID, notification);
	}

	private class DBHelper extends SQLiteOpenHelper {
		private final static String DB_NAME = "alerts.db";
		private final static int DB_VERSION = 1;

		DBHelper(Context c) {
			super(c, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE resorts " + "(_id INTEGER PRIMARY KEY, "
					+ "label TEXT, url TEXT)");
			db.execSQL("CREATE TABLE alerts " + "(_id INTEGER PRIMARY KEY, "
					+ "time INTEGER, " + "desc TEXT, " + "acked INTEGER, " + "resort INTEGER,"
					+ "FOREIGN KEY(resort) REFERENCES resort(_id) )");

			db.execSQL("CREATE VIEW view_resorts AS " +
			   "SELECT resorts._id AS _id, resorts.label AS label FROM alerts INNER JOIN resorts ON alerts.resort=resorts._id GROUP BY resorts._id ORDER BY resorts.label");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS alerts");
			db.execSQL("DROP TABLE IF EXISTS resorts");
			db.execSQL("DROP VIEW  IF EXISTS view_resorts");
			onCreate(db);
		}
	}
}
