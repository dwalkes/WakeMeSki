package com.wakemeski.core;

import android.content.Context;

import com.wakemeski.Log;


public class WakeMeSkiFactory {

	private static WakeMeSkiFactory mInstance = null;
	private final ReportController mReportController;
	private final ResortManager mResortManager;

	private WakeMeSkiFactory(Context c) {
		mResortManager = ResortManagerFile.getInstance(c);
		mReportController = ReportController.getInstance(c, mResortManager);
		Log.getInstance().setContext(c);
	}

	public ReportController getReportController() {
		return mReportController;
	}

	public ResortManager getRestortManager() {
		return mResortManager;
	}

	public static synchronized WakeMeSkiFactory getInstance( Context c ) {
		if( mInstance == null ) {
			mInstance = new WakeMeSkiFactory(c);
		}
		return mInstance;
	}


}
