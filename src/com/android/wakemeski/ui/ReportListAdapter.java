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
package com.android.wakemeski.ui;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.wakemeski.R;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.pref.SnowSettingsSharedPreference;

public class ReportListAdapter implements ListAdapter {
	private final static String TAG = "ReportListAdapter";

	private ArrayList<Report> mReports = new ArrayList<Report>();
	private boolean mLoading = true;

	private DataSetObservable mDataSetObservable = new DataSetObservable();

	private LayoutInflater mInflater;

	private Context mCtx;

	public ReportListAdapter(Context c) {
		mCtx = c;
		mInflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public synchronized void addReport(Report r) {
		if (r == null)
			mLoading = false;			
		else
			mReports.add(r);

		mDataSetObservable.notifyChanged();
	}

	public synchronized void clearReports() {
		mReports.clear();
		mDataSetObservable.notifyChanged();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	@Override
	public synchronized int getCount() {
		int size = mReports.size();
		// If size = 0 and we aren't loading, then no resorts have been
		// configured by the user. We want to return a single item telling
		// them to add a resort.
		if( mLoading || (size ==0 && !mLoading) )
			size++;
		return size;
	}

	@Override
	public synchronized Object getItem(int position) {
		if( position >= mReports.size() )
			return null; //This would be the "loading" or "not configured" item

		return mReports.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	private SnowSettingsSharedPreference getPrefs() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mCtx);
		SnowSettingsSharedPreference ssp = new SnowSettingsSharedPreference();
		if (!ssp.setFromPreferences(prefs)) {
			Log.e(TAG, "Error obtaining snow settings preferences");
		}
		return ssp;
	}

	@Override
	public synchronized View getView(int position, View convertView,
			ViewGroup parent) {
		Report r = null;
		
		//make sure its not the loading item
		if( position < mReports.size() )
			r = mReports.get(position);

		View v = mInflater.inflate(R.layout.snow_layout, parent, false);

		Resources rs = mCtx.getResources();

		Drawable d;
		if (r != null && Report.meetsPreference(r, getPrefs())) {
			d = rs.getDrawable(R.drawable.exceeds_threshold_background);
		} else {
			d = rs.getDrawable(R.drawable.below_threshold_background);
		}
		v.setBackgroundDrawable(d);

		TextView tv = (TextView) v.findViewById(R.id.resort_name);
		if( r != null ) {
			tv.setText(r.getLabel());
			tv = (TextView) v.findViewById(R.id.snow_value);
			tv.setText(r.getFreshAsString());
		}
		else {
			if( mLoading )
				tv.setText(R.string.loading);
			else
				tv.setText(R.string.no_resorts_configured);
		}

		return v;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return (getCount() == 0);
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.unregisterObserver(observer);
	}
}
