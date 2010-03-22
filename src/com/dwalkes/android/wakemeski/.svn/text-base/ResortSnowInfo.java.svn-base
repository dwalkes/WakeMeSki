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
package com.dwalkes.android.wakemeski;

/**
 * A class to hold information about snow totals for a specific resort.
 * Used to compare 24hr snow totals to a configured setting.
 * 
 * @author dan
 *
 */
public class ResortSnowInfo implements Comparable<ResortSnowInfo> {
	private SnowUnits resortReportUnits = SnowUnits.INCHES;
	private int	resortSnowDepth24Hours = 0;
	Resort resort;
	
	ResortSnowInfo(Resort resort ) {
		this.resort = resort;
	}
	
	/**
	 * Provides for sorting of resort snow info by snow depth
	 */
	public int compareTo(ResortSnowInfo compareObject) {
		return  compareObject.resortSnowDepth24Hours - this.resortSnowDepth24Hours ;
	}
	
	boolean exceedsOrMatchesPreference( SnowSettingsSharedPreference snowSettings ) {
		if ( resortReportUnits == snowSettings.getMeasurementUnits() ) {
			if( resortSnowDepth24Hours >= snowSettings.getSnowDepth() ) {
				return true;
			}
		} else {
			// units do not agree - change both to centimeters for more accuracy
			double resortSnowDepth24Hours_cm = ( resortReportUnits == SnowUnits.CENTIMETERS ?
					resortSnowDepth24Hours : resortSnowDepth24Hours * 2.54 );
			double prefSnowDepth24Hours_cm = (  snowSettings.getMeasurementUnits() == SnowUnits.CENTIMETERS ?
					snowSettings.getSnowDepth() : snowSettings.getSnowDepth() * 2.54);
			
			if( resortSnowDepth24Hours_cm >= prefSnowDepth24Hours_cm ) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return resort.toString() + " (" + resortSnowDepth24Hours + " " + resortReportUnits.getAbbreviation() + " in 24 hrs)";
	}

	/**
	 * @return Suitable user visible log message
	 */
	public String toLog() {
		return toString();
	}
	public void setResortSnowDepth24Hours( int resortSnowDepth ) {
		resortSnowDepth24Hours = resortSnowDepth;
	}
	

	public String getSnowDepth() {
		return resortSnowDepth24Hours + " " + resortReportUnits.getAbbreviation();
	}
	
    public Resort getResort() {
    	return resort;
    }

}
