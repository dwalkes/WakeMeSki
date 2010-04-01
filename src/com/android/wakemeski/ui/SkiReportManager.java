/*
 * Copyright (C) 2010 Dan Walkes
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
 * 
 */

package com.android.wakemeski.ui;

import java.util.HashSet;
import java.util.Set;

import com.android.wakemeski.core.Report;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Wrapper around com.android.wakemeski.core.Report.loadReport to convert report information
 * into ResortSnowInfo
 * @author dan
 *
 */
public class SkiReportManager {

	Context mContext;
	ConnectivityManager mConnectivityMgr;
	
	SkiReportManager( Context c, ConnectivityManager cm ) {
		mContext = c;
		mConnectivityMgr = cm;
	}
	
	ResortSnowInfo [] getSnowInfo( Resort [] resortList ) {
		Set<ResortSnowInfo> snowInfoSet = new HashSet<ResortSnowInfo>(); 
		for( Resort resort : resortList ) {
			Report report = Report.loadReport(mContext, mConnectivityMgr, resort.getLocation());
			if( report.hasFreshSnowTotal() ) {
				ResortSnowInfo snowInfo = new ResortSnowInfo(resort);
				snowInfo.setResortSnowDepth24Hours(report.getFreshSnowTotal());
				snowInfo.setResortReportUnits(report.getSnowUnits());
				snowInfoSet.add(snowInfo);
			}
		}
		return snowInfoSet.toArray(new ResortSnowInfo[snowInfoSet.size()]);
	}
}
