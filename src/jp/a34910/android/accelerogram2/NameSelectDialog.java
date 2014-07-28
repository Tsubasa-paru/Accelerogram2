package jp.a34910.android.accelerogram2;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class NameSelectDialog extends ConfirmationDialog implements OnItemClickListener {
	private String mTitle;
	private ArrayList<String> mNameList;
	private NameSelectDialog.SelectedListener mSelectedListener;
	private ArrayAdapter<String> mFileNameAdapter;
	private AlertDialog mDialog;

	public NameSelectDialog(Context context, String title, ArrayList<String> namelist, NameSelectDialog.SelectedListener listener) {
		super(context);
		this.mTitle = title;
		this.mNameList = namelist;
		this.mSelectedListener = listener;
	}

	@Override
	public AlertDialog show() {
		mFileNameAdapter = new ArrayAdapter<String>(mParent, android.R.layout.simple_list_item_1, mNameList);
		ListView listview = new ListView(mParent);
		listview.setScrollingCacheEnabled(false);
		listview.setFastScrollEnabled(true);
		listview.setOnItemClickListener(this);
		listview.setAdapter(mFileNameAdapter);

		this.setTitle(mTitle);
		this.setOnDeniedListener("Cancel", null);
		this.setView(listview);
		mDialog = super.show();
		return mDialog;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
		if (mDialog != null) {
			mDialog.dismiss();
		}
		String selected = mFileNameAdapter.getItem(position);
		mSelectedListener.onSelected(selected);
	}

	public interface SelectedListener {
		public void onSelected(String selected);
	}
}
