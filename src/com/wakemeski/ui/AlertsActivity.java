package com.wakemeski.ui;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.wakemeski.R;
import com.wakemeski.core.alert.AlertManager;

public class AlertsActivity extends ExpandableListActivity {

	private AlertManager mAlerts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getExpandableListView().setBackgroundColor(0xffC0C0C0);
		mAlerts = new AlertManager(getApplication());
		mAlerts.removeOld();
		Cursor c = mAlerts.getAlertResorts();
		setListAdapter(new AlertCursorAdapter(getApplicationContext(), c));

		//the alerts can be considered "viewed" so mark them acknowledged
		mAlerts.acknowledgeAlerts();
	}

	class AlertCursorAdapter extends ResourceCursorTreeAdapter {

		public AlertCursorAdapter(Context ctx, Cursor c) {
			super(ctx, c, R.layout.alerts_resort, R.layout.alert_entry);
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			TextView tv = (TextView)view.findViewById(R.id.alert_time);
			long epochMS = cursor.getLong(1) * 1000;
			CharSequence cs = DateFormat.format("EEE ha", epochMS);
			tv.setText(cs);

			tv = (TextView)view.findViewById(R.id.alert_desc);
			tv.setText(cursor.getString(2));
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			TextView tv = (TextView)view.findViewById(R.id.alert_group_label);
			tv.setText(cursor.getString(1));
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			long id = groupCursor.getLong(0);
			return mAlerts.getAlerts(id);
		}
	}
}
