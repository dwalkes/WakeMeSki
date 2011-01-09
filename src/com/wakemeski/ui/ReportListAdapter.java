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
package com.wakemeski.ui;

import java.util.ArrayList;

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

import com.wakemeski.R;
import com.wakemeski.core.Report;
import com.wakemeski.core.ReportController;
import com.wakemeski.core.ReportListener;
import com.wakemeski.core.WakeMeSkiFactory;

public class ReportListAdapter implements ListAdapter {

	Handler mHandler = new Handler();

	private ArrayList<Report> mReports = new ArrayList<Report>();

	private DataSetObservable mDataSetObservable = new DataSetObservable();

	private LayoutInflater mInflater;
	
	private ReportController mReportController;

	public ReportListAdapter(Context c) {
		mInflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mReportController = WakeMeSkiFactory.getInstance(c).getReportController();

		mReportController.addListenerAndUpdateReports(mListener,true);
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
		synchronized (mReports) {
			return mReports.size();
		}
	}

	/**
	 * @param position index into the summary list
	 * @return a Report at this position in mReportList or
	 * null if the report does not exist at this position.
	 */
	private Report getReportAtPosition(int position) {
		Report atPosition = null;
		synchronized (mReports) {
			if( position < mReports.size() )
				atPosition = mReports.get(position);
		}
		return atPosition;
	}
	
	@Override
	public synchronized Object getItem(int position) {
		return getReportAtPosition(position);
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
		Report r = getReportAtPosition(position);

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
		/**
		 * This method represents the content observer handler 
		 * we would use on notify changed
		 * from the report controller if it were a content provider.
		 */
		@Override
		public void onUpdated() {
			synchronized (mReports) {
				/*
				 *  This represents the query we would issue to the report controller
				 *  if it was implemented as a content provider, mReportList simulates
				 *  the cursor
				 */
				mReports = mReportController.getSortedReportList();
			}
			mHandler.post(new Runnable() {
				public void run() {
					mDataSetObservable.notifyChanged();
				}
			});
		}

		@Override
		public void onLoading(boolean started) {
		}

		@Override
		public void onAdded(Report r) {
		}
		
		@Override
		public void onBusy( boolean busy ) {
			
		}
	};
}
