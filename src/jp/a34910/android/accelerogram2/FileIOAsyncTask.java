package jp.a34910.android.accelerogram2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public abstract class FileIOAsyncTask extends AsyncTask<Void, Void, Boolean> {

	private Activity mParent = null;
	private Handler mPostHandler = null;
	private ProgressDialog mDialog = null;
	private String mErrMessage = null;

	public FileIOAsyncTask(Activity activity, Handler handler) {
		this.mParent = activity;
		this.mPostHandler = handler;
	}

	@Override
	protected void onPreExecute() {
		mDialog = new ProgressDialog(mParent);
		mDialog.setMessage("処理中...");
		mDialog.setCancelable(false);
		mDialog.show();
	}

	@Override
	protected Boolean doInBackground(Void... unused) {
		boolean completed = false;
		try {
			completed = backgroundTask();
		} catch (Exception e) {
			mErrMessage = e.getMessage();
			e.printStackTrace();
		}
		return completed;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mDialog.dismiss();
		Message msg = new Message();
		Bundle bundle = new Bundle();

		bundle.putBoolean("Result",	result);
		if (mErrMessage != null) {
			bundle.putString("ErrorMessage", mErrMessage);
		}
		msg.setData(bundle);
		mPostHandler.sendMessage(msg);
	}

	abstract boolean backgroundTask() throws Exception;
}
