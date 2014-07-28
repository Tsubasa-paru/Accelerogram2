package jp.a34910.android.accelerogram2;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

@SuppressLint("ClickableViewAccessibility")
public class SurfaceViewOnTouchListener implements OnTouchListener {
	static private final String TAG = MainActivity.APP_NAME + "onTouchListener";
	private View mTargetView;
	private OnTouchAction mOntouchAction;

	/**
	 * onTouchのステートを定義
	 * NONEー＞スクリーンにタッチ：MOVEー＞移動：MOVEー＞離す：NONE
	 * NONEー＞スクリーンにタッチ：MOVEー＞２つ目をタッチ：ZOOMー＞移動：ZOOMー＞離す：NONE
	 */
	private enum TouchState {
		NONE,
		MOVE,
		ZOOM,
	}
	private TouchState touchState = TouchState.NONE;

	private double originDistance = 0.0f;
	private double zoomDistance = 0.0f;
	private double moveDistance = 0.0f;
	private double originX = 0.0f;
	private double originY = 0.0f;
	private double moveX = 0.0f;
	private double moveY = 0.0f;

	public SurfaceViewOnTouchListener(View view, OnTouchAction action) {
		mTargetView = view;
		mOntouchAction = action;
	}

	private double eventDistance(MotionEvent e) {
		float x = e.getX(0) - e.getX(1);
		float y = e.getY(0) - e.getY(1);
		return Math.sqrt(x * x + y * y);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == mTargetView) {
			Log.d(TAG, "onTouchEvent");
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				touchState = TouchState.MOVE;
				originX = event.getX(0);
				originY = event.getY(0);
				moveX = originX;
				moveY = originY;
				originDistance = 0.0f;
				moveDistance = 0.0f;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				touchState = TouchState.ZOOM;
				originDistance = eventDistance(event);
				zoomDistance = 0.0f;
				break;
			case MotionEvent.ACTION_MOVE:
				if (touchState == TouchState.MOVE) {
					double x = moveX - event.getX(0);
					double y = moveY - event.getY(0);
					moveDistance = Math.sqrt(x * x + y * y);
					float adjust = (float) (moveDistance * 0.25f);
					if (x < 0) {
						adjust = -adjust;
					}
					mOntouchAction.movePosition(adjust);
					moveX = event.getX(0);
					moveY = event.getY(0);
				}
				if (touchState == TouchState.ZOOM) {
					zoomDistance = eventDistance(event);
					double zoomAdjust = zoomDistance / originDistance;
					originDistance = zoomDistance;
					if (zoomAdjust > 1.1f) {
						zoomAdjust = 1.1f;
					} else if (zoomAdjust < 0.9f) {
						zoomAdjust = 0.9f;
					}
					mOntouchAction.setZoom((float)zoomAdjust);
				}
				break;
			case MotionEvent.ACTION_UP:
				// fall through
			case MotionEvent.ACTION_POINTER_UP:
				touchState = TouchState.NONE;
				break;
			}
			return true;
		}
		return false;
	}

	/**
	 * MOVE/ZOOMを検出した時の動作を定義するInterface
	 *
	 */
	public interface OnTouchAction {
		public void setZoom(float zoomAdjust);
		public void movePosition(float positionAdjust);
	}
}
