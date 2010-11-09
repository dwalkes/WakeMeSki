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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.wakemeski.R;
import com.wakemeski.core.Report;
import com.wakemeski.core.Weather;

public class ReportActivity extends Activity {

	private final static String TAG = "ReportActivity";

	private static final int WEATHER_ID  = Menu.FIRST;
	private static final int DETAILS_ID  = WEATHER_ID + 1;
	private static final int LOCATION_ID = DETAILS_ID + 1;
	private static final int MAPIT_ID    = LOCATION_ID + 1;

	private Report mReport;

	@Override
	protected void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.report);

		Report r = (Report) getIntent().getParcelableExtra("report");
		mReport = r;

		// Title
		setTitle(getTitle() + " - " + r.getResort().getResortName());

		// Weather Icon
		ImageView iv = (ImageView) findViewById(R.id.report_weather);
		iv.setImageResource(r.getWeatherIconResId());

		// Title
		TextView t = (TextView) findViewById(R.id.report_label);
		t.setText(r.getResort().getResortName());
		
		if( r.hasErrors() ) {
			t = (TextView) findViewById(R.id.report_fresh);
			if( r.hasServerError() ) {
				/*
				 * The server can't find the info for this report, we've got a bug
				 * to fix on the server (and since we're running our unit tests we are
				 * probably already working on it :)
				 */
				t.setText(getString(R.string.report_error_working_on_it));
			} else {
				/*
				 * This was an error that happened client side (missing data connection,timeout, etc)
				 * so we have a localized error to display
				 */
				t.setText(getString(R.string.report_error_localized_detail, r.getLocalizedError()));
			}
			return;
		}
		
		t = (TextView) findViewById(R.id.report_snow_text);
		t.setText(getString(R.string.report_snow));
		
		t = (TextView) findViewById(R.id.report_weather_text);
		t.setText(getString(R.string.report_weather));

		t = (TextView) findViewById(R.id.report_date);
		t.setText(getString(R.string.report_date, r.getDate()));

		// Fresh
		t = (TextView) findViewById(R.id.report_fresh);
		t.setText(r.getDailyDetails());

		// Base Depths
		t = (TextView) findViewById(R.id.report_base);
		t.setText(getString(R.string.report_base, r.getSnowDepthsAsString()));

		// Lifts and Trails
		t = (TextView) findViewById(R.id.report_trails_lifts);
		t.setText(getString(R.string.report_trails_lifts,
					r.getTrailsAsString(), r.getLiftsAsString()));

		// Weather Forecast
		ViewGroup vg = (ViewGroup)findViewById(R.id.weather_list);
		for(Weather w: r.getWeather()) {
			addForecast(vg, w);
		}

		// Report Comments (optional)
		if( r.hasLocationComments() ) {
			t = (TextView) findViewById(R.id.report_comments_text);
			t.setText(getString(R.string.report_comments_header));
			t = (TextView) findViewById(R.id.report_comments);
			t.setText(r.getLocationComments());
		}
	}

	private void addForecast(ViewGroup vg, Weather w) {
		LayoutInflater li = getLayoutInflater();
		View v = li.inflate(R.layout.weather_item, null);

		String when = w.getWhen();
		String desc = w.getDesc();

		EditText tv = (EditText)v.findViewById(R.id.weather_item_txt);
		tv.setText(when + ": " + desc, BufferType.SPANNABLE);

		Spannable s = tv.getText();
		s.setSpan(new StyleSpan(Typeface.BOLD), 0, when.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		vg.addView(v);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(0, WEATHER_ID, 0, R.string.report_weather_link);
		item.setIcon(android.R.drawable.ic_menu_view);

		String s = mReport.getDetailsURL();
		if( s != null && s.length() > 0 ) {
			item = menu.add(0, DETAILS_ID, 0, R.string.report_details_link);
			item.setIcon(android.R.drawable.ic_menu_info_details);
		}

		s = mReport.getLocationURL();
		if( s != null && s.length() > 0 ) {
			item = menu.add(0, LOCATION_ID, 0, R.string.report_info_link);
			item.setIcon(android.R.drawable.ic_menu_info_details);
		}

		if( mReport.hasGeo() ) {
			item = menu.add(0, MAPIT_ID, 0, R.string.report_mapit_link);
			item.setIcon(android.R.drawable.ic_menu_directions);
		}

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == WEATHER_ID) {
			showUrl(mReport.getWeatherURL());
			return true;
		}
		else if (item.getItemId() == DETAILS_ID) {
			showUrl(mReport.getDetailsURL());
			return true;
		}
		else if (item.getItemId() == LOCATION_ID) {
			showUrl(mReport.getLocationURL());
			return true;
		}
		else if( item.getItemId() == MAPIT_ID ) {
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, mReport.getGeo()));
			}
			catch(Exception e){
				Log.e(TAG, "Error launching map for: " + mReport.getResort().getResortName(), e);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void showUrl(String url) {
		if (url != null && url.length() > 0) {
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			} catch (Exception e) {
				Log.e(TAG, "Error launching url: " + url, e);
			}
		}
	}
}
