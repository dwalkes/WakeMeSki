package com.wakemeski.ui;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.wakemeski.R;
import com.wakemeski.core.alert.AlertManager;

public class AlertsActivity extends ExpandableListActivity {

	private static final int SHARE_ID = Menu.FIRST;

	private AlertManager mAlerts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getExpandableListView().setBackgroundColor(0xffC0C0C0);
		mAlerts = new AlertManager(getApplication());
		mAlerts.removeOld();
		Cursor c = mAlerts.getAlertResorts();
		startManagingCursor(c);
		setListAdapter(new AlertCursorAdapter(getApplicationContext(), c));

		//the alerts can be considered "viewed" so mark them acknowledged
		mAlerts.acknowledgeAlerts();

		registerForContextMenu(getExpandableListView());
	}

	@Override
	protected void onResume() {
		super.onResume();
		ExpandableListView v = getExpandableListView();
		int i = getExpandableListAdapter().getGroupCount();
		while(i-- > 0)
			v.expandGroup(i);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mAlerts.close();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info =
			(ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);

		if( type == ExpandableListView.PACKED_POSITION_TYPE_GROUP )
			return;

		menu.add(Menu.NONE, SHARE_ID, Menu.NONE, R.string.alert_share);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListView.ExpandableListContextMenuInfo info =
			(ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
		int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

		CursorTreeAdapter ca = (CursorTreeAdapter)getExpandableListAdapter();
		Cursor g = ca.getGroup(groupPos);
		Cursor a = ca.getChild(groupPos, childPos);

		if( item.getItemId() == SHARE_ID ) {
			shareAlert(g, a);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void shareAlert(Cursor resort, Cursor alert) {
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.setType("text/plain");
		String label = AlertManager.getResortLabel(resort);
		String subject = getString(R.string.alert_share_subject, label);
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		String msg = getString(R.string.alert_share_heading, label) + "\n\n" +
						AlertManager.getAlertTime(alert) + "\n" +
						AlertManager.getAlertDesc(alert);
		i.putExtra(android.content.Intent.EXTRA_TEXT, msg);

		String chooserMsg = getString(R.string.alert_share_intent);
		startActivity(Intent.createChooser(i, chooserMsg));
	}

	class AlertCursorAdapter extends ResourceCursorTreeAdapter {

		public AlertCursorAdapter(Context ctx, Cursor c) {
			super(ctx, c, R.layout.alerts_resort, R.layout.alert_entry);
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			TextView tv = (TextView)view.findViewById(R.id.alert_time);
			tv.setText(AlertManager.getAlertTime(cursor));

			tv = (TextView)view.findViewById(R.id.alert_desc);
			tv.setText(AlertManager.getAlertDesc(cursor));
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
			Cursor c = mAlerts.getAlerts(id);
			startManagingCursor(c);
			return c;
		}
	}
}
