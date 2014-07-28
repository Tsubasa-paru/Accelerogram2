package jp.a34910.android.accelerogram2;

import android.os.Handler;
import android.os.Message;

public abstract class FileIOPostHandler extends Handler {
	private String mErrorMessage = null;
	@Override
	public void handleMessage(Message msg) {
		boolean isSuccess = msg.getData().getBoolean("Result");

		if (isSuccess) {
			onSuccess();
		} else {
			mErrorMessage = msg.getData().getString("ErrorMessage");
			onFailure(mErrorMessage);
		}
	}

	public abstract void onSuccess();

	public abstract void onFailure(String errMessage);

}
