package com.wakemeski.core;

public interface ReportListener {

	/**
	 * Called when an action on the report controller completes (report
	 * for a resort is added or removed)
	 */
	void onUpdated();

	/**
	 * Called when a report is added to the list of reports held by the
	 * report controller.  Classes which do not need to handle lists of reports
	 * but rather handle each report individually can implement this method
	 * @param r
	 */
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
