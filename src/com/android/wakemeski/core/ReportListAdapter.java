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
package com.android.wakemeski.core;

import java.util.ArrayList;

import com.android.wakemeski.R;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ReportListAdapter implements ListAdapter
{

	private ArrayList<Report> _reports = new ArrayList<Report>();
	
	private DataSetObservable _dataSetObservable = new DataSetObservable();
	
	private LayoutInflater _inflater;
	
	private Context _ctx;
	
	public ReportListAdapter(Context c)
	{
		_ctx = c;
		_inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public synchronized void addReport(Report r)
	{
		_reports.add(r);
		_dataSetObservable.notifyChanged();
	}
	
	public synchronized void removeReport(int pos)
	{
		_reports.remove(pos);
		_dataSetObservable.notifyChanged();
	}
	
	/**
	 * Removes all reports starting pos to the end.
	 */
	public synchronized void removeReports(int pos)
	{
		while(_reports.size()-1 != pos)
			_reports.remove(pos);
		_dataSetObservable.notifyChanged();
	}
	
	public synchronized void moveReport(int pos)
	{
		//swap report at pos with report at pos+1
		Report r1 = _reports.get(pos);
		Report r2 = _reports.get(pos+1);
		
		_reports.set(pos, r2);
		_reports.set(pos+1, r1);

		_dataSetObservable.notifyChanged();
	}
	
	public synchronized void clearReports()
	{
		_reports.clear();
		_dataSetObservable.notifyChanged();
	}
	
	public synchronized Report[] getReports()
	{
		return _reports.toArray(new Report[_reports.size()]);
	}
	
	public synchronized void setReports(Report reports[])
	{
		_reports.clear();
		for( Report r: reports)
		{
			_reports.add(r);
		}
			
		_dataSetObservable.notifyChanged();
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}

	@Override
	public synchronized int getCount()
	{
		return _reports.size();
	}

	@Override
	public synchronized Object getItem(int position)
	{
		return _reports.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getItemViewType(int position)
	{
		return 0;
	}
	
	private String getSummary(Report r)
	{
		StringBuffer b = new StringBuffer();

		//First see if we have an error
		String err = r.getError();
		if( err != null && err.length() > 0 )
		{
			b.append(_ctx.getString(R.string.report_error, err));
			return b.toString();
		}
		
		b.append(_ctx.getString(R.string.snow_total));
		for(String s: r.getSnowDepths())
		{
			b.append(' ').append(s);
		}
		
		b.append('\n');
		b.append(_ctx.getString(R.string.snow_daily));
		for(String s: r.getDailySnow())
		{
			b.append(' ').append(s);
		}
		
		String val = r.getSnowConditions();
		if( val.length() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.snow_condition, val));
		}
		
		if( r.getTrailsTotal() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.trails, r.getTrailsOpen(), r.getTrailsTotal()));
		}
		else if( r.getTrailsOpen() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.trails2, r.getTrailsOpen()));
		}
		
		if( r.getLiftsTotal() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.lifts, r.getLiftsOpen(), r.getLiftsTotal()));
		}
		else if( r.getLiftsOpen() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.lifts2, r.getLiftsOpen()));
		}
		
		String temps[] = r.getTemperatureReadings();
		if( temps.length > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.temps));
			for(String s: r.getTemperatureReadings())
			{
				b.append(' ').append(s);
			}
		}
		
		val = r.getWindSpeed();
		if( val.length() > 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.wind, val));
		}
		
		val = r.getDate();
		if( val.length() >= 0 )
		{
			b.append('\n');
			b.append(_ctx.getString(R.string.report_data, val));
		}
		
		return b.toString();
	}

	@Override
	public synchronized View getView(int position, View convertView, ViewGroup parent)
	{
		Report r = _reports.get(position);
		View v = _inflater.inflate(R.layout.report_view, parent, false);
		
		TextView tv = (TextView)v.findViewById(R.id.report_label);
		
		if( r.getLabel() == null )
		{
			tv.setText(r.getError());
		}
		else
		{
			tv.setText(r.getLabel());
		
			tv = (TextView)v.findViewById(R.id.report_txt);
			tv.setText(getSummary(r));
			
			ImageView iv = (ImageView)v.findViewById(R.id.report_image);
			iv.setImageResource(r.getWeatherIconResId());
		}

		return v;
	}

	@Override
	public int getViewTypeCount()
	{
		return 1;
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return (getCount() == 0);
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
		_dataSetObservable.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
		_dataSetObservable.unregisterObserver(observer);
	}
	
	protected Report getReportForPosition( int position ) 
	{
		return _reports.get(position);
	}
}
