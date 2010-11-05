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
package com.android.wakemeski.core;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Process;
import android.util.Log;

/**
 * Starts a long running thread that will check for updates to resorts and get
 * their report data on a periodic basis.
 * 
 * This is similar to the MessagingController class from the Android Email
 * application
 */
public class ReportController implements Runnable {
	private final static String TAG = "ReportController";

	private static ReportController inst = null;
	private Thread mThread;

	private BlockingQueue<Action> mActions = new LinkedBlockingQueue<Action>();

	private Hashtable<Resort, Report> mReports = new Hashtable<Resort, Report>();

	/**
	 * All access to mListeners *must* be synchronized
	 * Using a HashSet will ensure only one instance of a listener will be present
	 * in mListeners.  Therefore add and remove may be called multiple times for
	 * a given listener
	 */
	private HashSet<ReportListener> mListeners = new HashSet<ReportListener>();
	private boolean mBusy;
	private Context mContext;
	private ResortManager mResortManager;

	private ReportController(Context c, ResortManager rm) {
		mContext = c;
		mThread = new Thread(this);
		mThread.start();
		mResortManager = rm;		
	}
	
	
	/**
	 * @param c context
	 * @param rm resort manager
	 * @return the static synchronized instance of this class
	 */
	public static synchronized ReportController getInstance(Context c, ResortManager rm) {
		if( inst == null ) {
			inst = new ReportController(c, rm);
		}
		return inst;
	}

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		while (true) {
			try {
				Action a = mActions.take();
				mBusy = true;
				a.run();
			} catch (Exception e) {
				Log.e(TAG, "error performing action", e);
			}
			mBusy = false;
		}
	}

	public boolean isBusy() {
        return mBusy;
    }

	public void loadReports() {
		Action a = new LoadResortsAction(mResortManager.getResorts());
		mActions.add(a);
	}

	/**
	 * Returns the reports we have loaded (cached)
	 * @return
	 */
	public Report[] getLoadedReports() {
		synchronized (mListeners) {
			Report r[] = mReports.values().toArray(new Report[mReports.size()]);
			//check if there are no cached reports and reload
			if( r.length == 0 )
				loadReports();
			return r;
		}
	}

	/**
	 * Returns a mapping of Resort->Report
	 */
	public Hashtable<Resort, Report> getResortReport() {
		return mReports;
	}

	/**
	 * Adds a Resort to monitor reports for
	 */
	public void addResort(Resort r) {
		Action a = new AddResortAction(r);
		mActions.add(a);
	}

	/**
	 * Removes a resort we were monitoring
	 */
	public void removeResort(Resort r) {
		Action a = new RemoveResortAction(r);
		mActions.add(a);
	}

	/**
	 * Adds a listener if not already present
	 * @param listener
	 */
	public void addListener(ReportListener listener) {
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}

	/**
	 * Removes a listener if present
	 * @param listener
	 * @return true if the list contained this listener, false if the listener had already
	 * been removed or was never in the list
	 */
	public boolean removeListener(ReportListener listener) {
		boolean containedElement;
		synchronized (mListeners) {
			containedElement = mListeners.remove(listener);
		}
		return containedElement;
	}

	abstract class Action {
		abstract void run();
	}

	class AddResortAction extends Action {
		Resort resort;
		AddResortAction(Resort r) {
			resort = r;
		}

		@Override
		public void run() {
			Context c = ReportController.this.mContext;
			ConnectivityManager cm = 
				(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

			WakeMeSkiServer srv = new WakeMeSkiServer(mContext);
			Report r = Report.loadReport(c, cm, resort, srv);			
			synchronized (mListeners) {
				mReports.put(resort, r);
				for(ReportListener rl: mListeners) {					
					rl.onAdded(r);
				}
			}
		}
	}

	class RemoveResortAction extends Action {
		Resort r;
		RemoveResortAction(Resort r) {
			this.r = r;
		}

		@Override
		public void run() {			
			synchronized (mListeners) {
				Report removed = mReports.remove(r);
				for(ReportListener l: mListeners) {
					l.onRemoved(removed);
				}
			}
		}
	}

	class LoadResortsAction extends Action {
		Resort resorts[];

		LoadResortsAction(Resort r[]) {
			resorts = r;
		}
		@Override
		void run() {
			Context c = ReportController.this.mContext;
			ConnectivityManager cm = 
				(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

			synchronized (mListeners) {
				for(ReportListener l: mListeners)
					l.onLoading(true);
			}

			WakeMeSkiServer server = new WakeMeSkiServer(mContext);
			for( Resort res: resorts ) {
				Report r = Report.loadReport(c, cm, res, server);				
				synchronized (mListeners) {
					mReports.put(res, r);
					for(ReportListener l: mListeners)
						l.onAdded(r);
				}				
			}

			synchronized (mListeners) {
				for(ReportListener l: mListeners)
					l.onLoading(false);
			}
		}
	}
}
