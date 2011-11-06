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
package com.wakemeski.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.wakemeski.R;
import com.wakemeski.core.Location;
import com.wakemeski.core.LocationFinder;
import com.wakemeski.core.WakeMeSkiServer;

public class LocationFinderActivity extends ListActivity {
	// Spawned activity id's
	private final static int SELECT_LOC = 2;

	// Dialog ID's
	private static final int ERROR_DLG = 1;

	// This is tied to the ERROR_DLG
	private String _errMsg;

	private String _region = null;

	private final Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();

			String regions[] = b.getStringArray("regions");
			if (regions != null) {
				setListAdapter(new ArrayAdapter<String>(
						LocationFinderActivity.this,
						android.R.layout.simple_list_item_1, regions));
			}

			Location l[] = (Location[]) b.getParcelableArray("locations");
			if (l != null) {
				setListAdapter(new ArrayAdapter<Location>(
						LocationFinderActivity.this,
						android.R.layout.simple_list_item_1, l));

			}

			setLoading(false);

			String err = b.getString("error");
			if (err != null) {
				_errMsg = err;
				showDialog(ERROR_DLG);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		if (!isConnected()) {
			_errMsg = getApplicationContext().getString(
					R.string.error_no_connection);
			showDialog(ERROR_DLG);
			return;
		}
		setLoading(true);

		Intent i = getIntent();
		_region = i.getStringExtra("region");
		if (_region != null) {
			showLocations(_region);
		} else {
			showRegions();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (_region == null) {
			String selection = (String) getListView().getItemAtPosition(
					position);
			Intent i = new Intent(this, LocationFinderActivity.class);

			i.putExtra("region", selection);
			startActivityForResult(i, SELECT_LOC);
		} else {
			Location loc = (Location) getListView().getItemAtPosition(position);
			Intent i = new Intent();

			i.putExtra("region", _region);
			i.putExtra("location", loc.getLabel());
			i.putExtra("url", loc.getReportUrlPath());
			setResult(RESULT_OK, i);
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SELECT_LOC && resultCode == RESULT_OK) {
			setResult(resultCode, data);
			finish();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ERROR_DLG) {
			return new AlertDialog.Builder(this).setTitle(R.string.error_title)
					.setMessage(_errMsg).create();
		}
		return null;
	}

	private void setLoading(boolean loading) {
		if (loading)
			setTitle(R.string.selection_activity_loading);
		else
			setTitle(R.string.selection_activity);

		setProgressBarIndeterminateVisibility(loading);
	}

	private void showRegions() {
		Thread t = new Thread(new Runner());
		t.start();
	}

	private void showLocations(String region) {
		Thread t = new Thread(new Runner(region));
		t.start();
	}

	private boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null)
			return cm.getActiveNetworkInfo().isConnected();

		return false;
	}

	class Runner implements Runnable {
		private String _region = null;

		Runner() {
		}

		Runner(String region) {
			_region = region;
		}

		@Override
		public void run() {
			Bundle b = new Bundle();
			try {

				WakeMeSkiServer srv = new WakeMeSkiServer(getApplicationContext());
				LocationFinder finder = new LocationFinder(srv);

				if (_region == null) {
					String regions[] = finder.getRegions();
					b.putStringArray("regions", regions);
				} else {
					Location locations[] = finder.getLocations(_region);

					b.putParcelableArray("locations", locations);
				}
			} catch (Exception e) {
				if (!isConnected()) {
					_errMsg = getApplicationContext().getString(
							R.string.error_no_connection);
					showDialog(ERROR_DLG);
				} else {
					b.putString("error", e.getLocalizedMessage());
				}
			}

			Message msg = Message.obtain(_handler);
			msg.setData(b);
			msg.sendToTarget();
		}
	}
}
