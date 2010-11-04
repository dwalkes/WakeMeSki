package com.android.wakemeski.core;

import android.content.Context;

public class WakeMeSkiFactory {

	private static WakeMeSkiFactory mInstance = null;
	private ReportController mReportController;
	private ResortManager mResortManager;
	
	private WakeMeSkiFactory(Context c) {
		mResortManager = ResortManagerFile.getInstance(c);
		mReportController = ReportController.getInstance(c, mResortManager);
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
