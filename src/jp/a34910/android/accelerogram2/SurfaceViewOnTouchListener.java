package jp.a34910.android.accelerogram2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

@SuppressLint("ClickableViewAccessibility")
public class SurfaceViewOnTouchListener implements OnTouchListener, OnGestureListener,OnDoubleTapListener {
	static private final String TAG = MainActivity.APP_NAME + "onTouchListener";
	private View mTargetView;
	private OnTouchAction mOnTouchAction;
	private GestureDetectorCompat mGestureDetector;

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

	private final float FAST_MODE = 0.25f;
//	private final float FINE_MODE = 0.1f;
	private float moveMode = FAST_MODE;

	/**
	 * ZOOM/MOVE/SKIPを検出した時の動作を定義するInterface
	 *
	 */
	public interface OnTouchAction {
		public void setZoom(View view, float zoomAdjust);
		public void movePosition(View view, float positionAdjust);
		public void skipPosition(View view, float distance_from_center);
	}

	public SurfaceViewOnTouchListener(Context context, View view, OnTouchAction action) {
		mTargetView = view;
		mOnTouchAction = action;
		mGestureDetector = new GestureDetectorCompat(context, this);
		mGestureDetector.setOnDoubleTapListener(this);
	}

	private double eventDistance(MotionEvent e) {
		float x = e.getX(0) - e.getX(1);
		float y = e.getY(0) - e.getY(1);
		return Math.sqrt(x * x + y * y);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == mTargetView) {
			if (mGestureDetector.onTouchEvent(event)) {
				return true;
			}
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_POINTER_DOWN:// Second Touch
				Log.d(TAG, "Change to ZOOM");
				touchState = TouchState.ZOOM;
				originDistance = eventDistance(event);
				zoomDistance = 0.0f;
				break;
			case MotionEvent.ACTION_MOVE:
				if (touchState == TouchState.ZOOM) {
					zoomDistance = eventDistance(event);
					double zoomAdjust = zoomDistance / originDistance;
					originDistance = zoomDistance;
					if (zoomAdjust > 1.1f) {
						zoomAdjust = 1.1f;
					} else if (zoomAdjust < 0.9f) {
						zoomAdjust = 0.9f;
					}
					mOnTouchAction.setZoom(mTargetView, (float)zoomAdjust);
				}
				break;
			case MotionEvent.ACTION_UP:
				// fall through
			case MotionEvent.ACTION_POINTER_UP:
				Log.d(TAG, "Change to NONE");
				touchState = TouchState.NONE;
				break;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		Log.d(TAG, "Change to MOVE");
		touchState = TouchState.MOVE;
		originDistance = 0.0f;
		moveDistance = 0.0f;
		moveMode = FAST_MODE;
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// Do nothing
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// Do nothing
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (touchState == TouchState.MOVE) {
			double x = distanceX;
			double y = distanceY;
			moveDistance = Math.sqrt(x * x + y * y);
			float adjust = (float) (moveDistance * moveMode);
			if (x < 0) {
				adjust = -adjust;
			}
			mOnTouchAction.movePosition(mTargetView, adjust);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// Do nothing
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		// Do nothing
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		// Do nothing
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		// DoubleTap検出時にSKIP
		float distance_from_center = (mTargetView.getWidth() / 2 ) - e.getRawX();
		mOnTouchAction.skipPosition(mTargetView, distance_from_center);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		// Do nothing
		return false;
	}
}
