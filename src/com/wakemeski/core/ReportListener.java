package com.wakemeski.core;

public interface ReportListener {

	void onRemoved(Report r);
	
	void onAdded(Report r);

	/**
	 * Called with true when loading of reports is started. Called with false
	 * when the loading is completed
	 */
	void onLoading(boolean started);
	
	/**
	 * Called when the report controller is busy with an action
	 * or when the busy state clears for the controller
	 * @param isBusy true when the controller is busy with an action
	 */
	void onBusy(boolean isBusy);
}
