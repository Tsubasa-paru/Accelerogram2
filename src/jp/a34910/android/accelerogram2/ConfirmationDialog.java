package jp.a34910.android.accelerogram2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

public class ConfirmationDialog {
	protected Context mParent;
	private AlertDialog.Builder mBuilder;
	private String mTitle = "確認";
	private View mView = null;
	private ConfirmationDialog.ConfirmedListener mConfirmedlistener;
	private ConfirmationDialog.DeniedListener mDeniedListener;

	public ConfirmationDialog(Context context) {
		this.buildDialog(context, null);
	}

	public ConfirmationDialog(Context context, String msg) {
		this.buildDialog(context, msg);
	}

	public ConfirmationDialog(Context context, String msg,
			ConfirmationDialog.ConfirmedListener listener) {
		this.buildDialog(context, msg);
		this.setOnConfirmListener("OK", listener);
	}

	public ConfirmationDialog(Context context, String msg,
			ConfirmationDialog.ConfirmedListener confirmlistener,
			ConfirmationDialog.DeniedListener deniedlistener) {
		this.buildDialog(context, msg);
		this.setOnConfirmListener("OK", confirmlistener);
		this.setOnDeniedListener("NG", deniedlistener);
	}

	private void buildDialog(Context context, String msg){
		this.mParent = context;
		this.mBuilder = new AlertDialog.Builder(this.mParent);
		this.mBuilder.setTitle(mTitle);
		if (msg != null) {
			this.mBuilder.setMessage(msg);
		}
	}

	protected void setTitle(String title) {
		if (this.mBuilder != null) {
			this.mBuilder.setTitle(title);
		}
	}

	protected void setMessage(String message) {
		if (this.mBuilder != null) {
			this.mBuilder.setMessage(message);
		}
	}

	protected void setView(View view) {
		if (this.mBuilder != null) {
			if (view != null) {
				this.mView = view;
				this.mBuilder.setView(view);
			}
		}
	}

	protected void setOnConfirmListener(String positive, ConfirmationDialog.ConfirmedListener listener) {
		this.mConfirmedlistener = listener;
		this.mBuilder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mConfirmedlistener != null) {
					mConfirmedlistener.onConfirmed(mView);
				}
			}
		});
	}

	protected void setOnDeniedListener(String negative, ConfirmationDialog.DeniedListener listener) {
		this.mDeniedListener = listener;
		this.mBuilder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mDeniedListener != null) {
					mDeniedListener.onDenied(mView);
				}
			}
		});
	}

	protected AlertDialog show() {
		if (this.mBuilder != null) {
			return mBuilder.show();
		} else {
			return null;
		}
	}

	public interface ConfirmedListener {
		public void onConfirmed(View view);
	}

	public interface DeniedListener {
		public void onDenied(View view);
	}
}
