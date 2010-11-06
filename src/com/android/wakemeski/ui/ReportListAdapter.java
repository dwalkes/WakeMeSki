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
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.wakemeski.R;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.ReportController;
import com.android.wakemeski.core.ReportListener;
import com.android.wakemeski.core.WakeMeSkiFactory;

public class ReportListAdapter implements ListAdapter {

	Handler mHandler = new Handler();

	private ArrayList<Report> mReports = new ArrayList<Report>();
	private boolean mLoading = false;

	private DataSetObservable mDataSetObservable = new DataSetObservable();

	private LayoutInflater mInflater;

	public ReportListAdapter(Context c) {
		mInflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		synchronized (mReports) {
			ReportController reportController = WakeMeSkiFactory.getInstance(c).getReportController();
			reportController.addListener(mListener);
			Report reports[] = reportController.getLoadedReports();
			for(Report r: reports)
				mReports.add(r);
		}
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

	@Override
	public synchronized View getView(int position, View convertView,
			ViewGroup parent) {
		Report r = null;
		
		//make sure its not the loading item
		if( position < mReports.size() )
			r = mReports.get(position);

		View v = mInflater.inflate(R.layout.snow_layout, parent, false);

		TextView tv = (TextView) v.findViewById(R.id.resort_name);
		if( r != null ) {
			tv.setText(r.getResort().getResortName());
			tv = (TextView) v.findViewById(R.id.snow_value);
			if( r.hasErrors() )
				tv.setText(R.string.fresh_not_available);
			else
				tv.setText(r.getFreshAsString());

			ImageView iv = (ImageView)v.findViewById(R.id.snow_layout_icon);
			iv.setImageResource(r.getWeatherIconResId());
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

	ReportListener mListener = new ReportListener() {
		
		@Override
		public void onRemoved(Report r) {
			synchronized (mReports) {
				mReports.remove(r);
			}
			mHandler.post(new Runnable() {
				public void run() {
					mDataSetObservable.notifyChanged();
				}
			});
		}

		@Override
		public void onLoading(boolean started) {
			synchronized (mReports) {
				mLoading = started;
				if( mLoading )
					mReports.clear();
			}			
			mHandler.post(new Runnable() {
				public void run() {
					mDataSetObservable.notifyChanged();
				}
			});
		}

		@Override
		public void onAdded(Report r) {
			synchronized (mReports) {
				mReports.add(r);
				if( !mLoading ) {
					//this report was just added, we need to make sure we keep
					//the list sorted alphabetically
					Collections.sort(mReports, new Comparator<Report>() {
						public int compare(Report r1, Report r2){
							String n1 = r1.getResort().getResortName();
							String n2 = r2.getResort().getResortName();
							return n1.compareTo(n2);
						}
					});
				}
			}
			mHandler.post(new Runnable() {
				public void run() {
					mDataSetObservable.notifyChanged();
				}
			});
		}
	};
}
