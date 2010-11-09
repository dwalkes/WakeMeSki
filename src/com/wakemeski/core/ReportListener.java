package com.wakemeski.core;

public interface ReportListener {

	void onRemoved(Report r);
	
	void onAdded(Report r);

	/**
	 * Called with true when loading of reports is started. Called with false
	 * when the loading is completed
	 */
	void onLoading(boolean started);
}
