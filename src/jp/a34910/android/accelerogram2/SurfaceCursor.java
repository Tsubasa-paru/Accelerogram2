package jp.a34910.android.accelerogram2;

import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceCursor extends SurfaceView implements SurfaceHolder.Callback{
	static private final String TAG = MainActivity.APP_NAME + "SurfaceCursor";
	static private final float ZOOM_MAX = 4.0f;
	static private final float ZOOM_MIN = 1.0f;
	static private final int DRAW_PERIOD = 50;

	private SurfaceHolder mSurfaceHolder;
	private Bitmap mCursorBitmap;
	private Canvas mCursorCanvas;

	private int mWidth;
	private int mHeight;
	private int mCenterX;
	private int mCenterY;
	private int mStep;
	private PointF mGsensorImmediate;
	private String mTimestamp = "00:00:00.00";
	private String mUserName = "";
	private String mComment = "";

	private DrawSurfaceTask mDrawSurfaceTask;
	private GsensorData mGsensorData = null;
	private int mPosition;
	private float mZoom = ZOOM_MIN;
	private boolean mGsensorAlertFlag = false;
	private boolean mEnableTracsFlag = false;
	private boolean mStillTracksFlag = false;

	/**
	 * 描画するカーソルの情報
	 * Bitmap_GREEN:通常状態に描画するカーソルのビットマップ
	 * Bitmap_RED:異常発生時に描画するカーソルのビットマップ
	 * Background:描画する背景のモード
	 */
	static public class Cursor {
		static public Bitmap Bitmap_RED;
		static public Bitmap Bitmap_GREEN;
		public enum Background {
			CIRCLE,
			SQUARE,
		}
	}
	private Cursor.Background mCursorBackground;

	public SurfaceCursor(Context context) {
		super(context);
		initSurfaceCursor();
	}

	public SurfaceCursor(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSurfaceCursor();
	}

	public SurfaceCursor(Context context, AttributeSet attrs, int defstyle) {
		super(context, attrs, defstyle);
		initSurfaceCursor();
	}

	/**
	 * カーソル表示用SurfaceViewの初期化
	 */
	private void initSurfaceCursor() {
		mSurfaceHolder =getHolder();
		mSurfaceHolder.addCallback(this);
		setFocusable(true);
		Cursor.Bitmap_RED = BitmapFactory.decodeResource(getResources(), R.drawable.ic_center_gravity_red);
		Cursor.Bitmap_GREEN = BitmapFactory.decodeResource(getResources(), R.drawable.ic_center_gravity_green);
		this.setZoom(3.0f);
		this.setBackgroundMode(Cursor.Background.CIRCLE);
		this.mPosition = 0;
		this.mGsensorData = new GsensorData(DRAW_PERIOD);// This is dummy
		this.mDrawSurfaceTask = new DrawSurfaceTask(DRAW_PERIOD);
		this.mGsensorImmediate = new PointF(0, 0);
	}

	/**
	 * カーソル描画時のズーム倍率を設定する
	 * @param zoom ズーム倍率（最大：3.0/最小1.5）
	 * @return 設定したズーム倍率
	 */
	public float setZoom(float zoom) {
		if ((zoom >= ZOOM_MIN) && (zoom <= ZOOM_MAX)) {
			this.mZoom =  zoom;
		}
		Log.d(TAG, "mZoom:" + this.mZoom);
		return this.mZoom;
	}

	/**
	 * カーソル描画時にアラート表示を指示するフラグを設定する
	 * @param alertflag true:アラート発生 false:それ以外
	 */
	public void setAlertFlag(boolean alertflag) {
		this.mGsensorAlertFlag = alertflag;
	}

	/**
	 * REC/REPLAY/PAUSE時に描画するGセンサデータのオブジェクトを設定する
	 * @param gsensorData G-sensorデータのオブジェクト
	 */
	public void setGsensorData(GsensorData gsensorData) {
		this.mGsensorData = gsensorData;
		if (this.mGsensorData != null) {
			this.mUserName = this.mGsensorData.getUserName();
			this.mComment = this.mGsensorData.getComment();
		}
	}

	/**
	 * カーソルの描画位置（即値）を設定する
	 * @param gsensor カーソル位置（gsensor.x：縦方向/gsensor.y：横方向）
	 */
	public void setCursorPosition(PointF gsensor) {
		this.mGsensorImmediate.x = gsensor.x;
		this.mGsensorImmediate.y = gsensor.y;
	}

	/**
	 * Gセンサデータのオブジェクト内でのカーソル描画位置を指定する
	 * @param position 描画位置
	 */
	public void setCursorPosition(int position) {
		this.mPosition = position;
	}

	/**
	 * Information部に表示するタイムスタンプを設定する
	 * @param timestamp タイムスタンプ
	 */
	public void setTimestamp(String timestamp) {
		this.mTimestamp = timestamp;
	}

	/**
	 * Information部に表示するユーザ名を設定する
	 * @param username ユーザ名
	 */
	public void setUserName(String username) {
		this.mUserName = username;
	}

	/**
	 * Information部に表示するコメントを設定する
	 * @param comment コメント
	 */
	public void setComment(String comment) {
		this.mComment = comment;
	}

	/**
	 * 背景描画のモードを設定する
	 * @param mode 背景モード
	 * @return 設定した背景モード
	 */
	public Cursor.Background setBackgroundMode(Cursor.Background mode) {
		this.mCursorBackground = mode;
		return this.mCursorBackground;
	}

	/**
	 * 軌跡表示をON/OFFを設定する
	 * @param enable true:軌跡表示ON / false:軌跡表示OFF
	 * @return 軌跡表示の状態
	 */
	public boolean enableTracks(boolean enable) {
		mEnableTracsFlag = enable;
		return mEnableTracsFlag;
	}

	/**
	 * 軌跡表示の一時停止ON/OFFを設定する
	 * @param still true:一時停止ON / 一時停止OFF
	 * @return 一時停止の状態
	 */
	public boolean stillTracks(boolean still) {
		if (mEnableTracsFlag) {
			mStillTracksFlag = still;
		}
		return mStillTracksFlag;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surface created");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surface changed");
		mWidth = width;
		mHeight = height;
		mCenterX = width / 2;
		mCenterY = height / 2;
		mStep = mCenterX / 8;
		PointF center = new PointF(0.0f, 0.0f);
		setCursorPosition(center);
		mCursorBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCursorCanvas = new Canvas(mCursorBitmap);
		mDrawSurfaceTask.execute();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		synchronized (mSurfaceHolder) {
			mDrawSurfaceTask.cancel();
		}
		Log.d(TAG, "surface destroyed");
	}

	/**
	 * カーソル描画の周期タスク
	 */
	private class DrawSurfaceTask extends PeriodicTask {
		private final float STROKE = 1.5f;
		private final float FONTSIZE = 48.0f;
		private final int TIMESTAMP_HEIGHT = 50;
		private Paint axisPaint;
		private Paint latticePaint;
		private Paint circlePaint;
		private Paint textPaint;
		private Paint framePaint;
		private Paint cursorPaint;
		private Paint trackPaint;
		private int cursorAlpha;
		private int[] Alpha = new int[256];

		public DrawSurfaceTask(long period) {
			super(period);

			for (int i = 0; i < 256; i++) {
				Alpha[i] = (int)(255 * Math.log10(1.0f + (0.035 * i)));
			}

			axisPaint = new Paint();
			axisPaint.setStrokeWidth(STROKE);
			axisPaint.setColor(Color.BLUE);
			axisPaint.setAntiAlias(true);

			latticePaint = new Paint();
			latticePaint.setStrokeWidth(STROKE);
			latticePaint.setColor(Color.CYAN);
			latticePaint.setAntiAlias(true);

			circlePaint = new Paint();
			circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
			circlePaint.setAlpha(128);
			circlePaint.setAntiAlias(true);

			textPaint = new Paint();
			textPaint.setColor(Color.YELLOW);
			textPaint.setAntiAlias(true);

			framePaint = new Paint();
			framePaint.setColor(Color.DKGRAY);
			framePaint.setStyle(Style.FILL);
			framePaint.setAlpha(128);
			framePaint.setAntiAlias(true);

			cursorPaint = new Paint();
			cursorAlpha = 0;
			cursorPaint.setAlpha(Alpha[cursorAlpha]);

			trackPaint = new Paint();
			trackPaint.setColor(Color.YELLOW);
			trackPaint.setStyle(Style.FILL);
			trackPaint.setStrokeWidth(STROKE * 2);
			trackPaint.setAntiAlias(true);
		}

		@Override
		protected void periodicTaskOnTimerThread() {
			drawBackground();
			if (mEnableTracsFlag) {
				drawTracks();
			}
			drawCursor();
			drawInformationText(0f);
			synchronized (mSurfaceHolder) {
				Canvas sfcanvas = mSurfaceHolder.lockCanvas();
				if (sfcanvas != null) {
					sfcanvas.drawBitmap(mCursorBitmap, 0, 0, null);
					mSurfaceHolder.unlockCanvasAndPost(sfcanvas);
				}
			}
		}

		@Override
		protected void periodicTaskOnUIThread() {
			// Do nothing
		}

		/**
		 * 背景描画
		 * 背景モードによってSQUARE/CIRCLEの背景を描画する
		 */
		private void drawBackground() {
			float step = mStep * mZoom;
			int indexMax = (int) (12 * (1 / mZoom));
			textPaint.setTextSize(32);
			switch (mCursorBackground) {
			case SQUARE:
				mCursorCanvas.drawColor(Color.LTGRAY);
				for (int index = 1; index <= indexMax; index++) {
					mCursorCanvas.drawLine(0, mCenterY - (step * index), mWidth, mCenterY - (step * index), latticePaint);
					mCursorCanvas.drawLine(0, mCenterY + (step * index), mWidth, mCenterY + (step * index), latticePaint);
					mCursorCanvas.drawLine(mCenterX - (step * index), 0, mCenterX - (step * index), mHeight, latticePaint);
					mCursorCanvas.drawLine(mCenterX + (step * index), 0, mCenterX + (step * index), mHeight, latticePaint);
					mCursorCanvas.drawText("0." + index + "G", mCenterX, mCenterY + (step * index), textPaint);
					mCursorCanvas.drawText("0." + index + "G", mCenterX, mCenterY - (step * index), textPaint);
					mCursorCanvas.drawText("0." + index + "G", mCenterX + (step *index), mCenterY, textPaint);
					mCursorCanvas.drawText("0." + index + "G", mCenterX - (step * index), mCenterY, textPaint);
				}
				break;
			case CIRCLE:
				mCursorCanvas.drawColor(Color.LTGRAY);
				for (int index = indexMax; index > 0; index--) {
					int r = 200 - (24 * index);
					int g = 200 - (24 * index);
					int b = 200 + (6 * index);
					if (r < 0) {
						r = 16;
						g = 16;
						b = 16;
					}
					circlePaint.setColor(Color.argb(255, r, g, b));
					mCursorCanvas.drawCircle(mCenterX, mCenterY, step * index, circlePaint);
					String text = String.format(Locale.getDefault(), "%.1fG", (float) index /10);
					mCursorCanvas.drawText(text, mCenterX, mCenterY + (step * index), textPaint);
					mCursorCanvas.drawText(text, mCenterX, mCenterY - (step * index), textPaint);
					mCursorCanvas.drawText(text, mCenterX + (step *index), mCenterY, textPaint);
					mCursorCanvas.drawText(text, mCenterX - (step * index), mCenterY, textPaint);
				}
				break;
			default:
				break;
			}
			mCursorCanvas.drawLine(mCenterX, 0, mCenterX, mHeight, axisPaint);
			mCursorCanvas.drawLine(0, mCenterY, mWidth, mCenterY, axisPaint);
		}

		private final int TRACK_PERIOD = 4;//50ms x 4 = 200ms周期
		private final int TRACK_TIME = 5 * 1000;//5sec間表示
		private final int TRACK_MAX = TRACK_TIME / (DRAW_PERIOD * TRACK_PERIOD);
		/**
		 * 軌跡描画
		 * 一定期間のカーソル移動軌跡を描画する
		 */
		private void drawTracks() {
			if (mGsensorData != null) {
				int position = mPosition & ~(TRACK_PERIOD - 1); // mPosition - (mPosition % TRACK_PERIOD)
				final float size = 16;
				float start_x = mCenterX - (mGsensorData.getGsensor(mPosition).x * mStep * mZoom);
				float start_y = mCenterY + (mGsensorData.getGsensor(mPosition).y * mStep * mZoom);
				for (int count = 0; count < TRACK_MAX; count++, position -= 4) {
					if (position < 0) {
						break;
					}
					PointF gsensor = mGsensorData.getGsensor(position);
					int alpha = (255 / TRACK_MAX) * (TRACK_MAX - count);
					float track_x = mCenterX - (gsensor.x * mStep * mZoom);
					float track_y = mCenterY + (gsensor.y * mStep * mZoom);
					RectF rect = new RectF(track_x - size, track_y - size, track_x + size, track_y + size);
					trackPaint.setAlpha(Alpha[alpha]);
					mCursorCanvas.drawRect(rect, trackPaint);
					mCursorCanvas.drawLine(start_x, start_y, track_x, track_y, trackPaint);
					start_x = track_x;
					start_y = track_y;
				}
			}
		}

		/**
		 * カーソル描画
		 * 現在のカーソル位置にカーソルを描画する
		 * アラートが設定されていた時、カーソルを一定期間アラート状態にする
		 */
		private void drawCursor() {
			PointF gsensor;
			if (mEnableTracsFlag && mGsensorData != null) {
				gsensor = mGsensorData.getGsensor(mPosition);
			} else {
				gsensor = mGsensorImmediate;
			}
			float x_posi = mCenterX - (gsensor.x * mStep * mZoom);
			float y_posi = mCenterY + (gsensor.y * mStep * mZoom);
			mCursorCanvas.drawBitmap(Cursor.Bitmap_GREEN,
					x_posi - (Cursor.Bitmap_GREEN.getWidth() /2),
					y_posi - (Cursor.Bitmap_GREEN.getHeight() / 2),
					null);

			if (cursorAlpha > 0) {
				cursorAlpha = cursorAlpha - 8;
				if (cursorAlpha < 0) cursorAlpha = 0;
			}
			if (mGsensorAlertFlag == true) {
				mGsensorAlertFlag = false;
				cursorAlpha = 255;
			}
			cursorPaint.setAlpha(Alpha[cursorAlpha]);

			mCursorCanvas.drawBitmap(Cursor.Bitmap_RED,
					x_posi - (Cursor.Bitmap_RED.getWidth() /2),
					y_posi - (Cursor.Bitmap_RED.getHeight() / 2),
					cursorPaint);
		}

		/**
		 * Informationテキスト描画
		 * 指定した位置にタイムスタンプ／ユーザ名／ファイル名／コメントを描画する
		 * @param top 描画位置の上端
		 */
		private void drawInformationText(float top) {
			final float frame_left = 0.0f;
			final float frame_top = top;
			final float frame_right = mWidth;
			float frame_bottom = TIMESTAMP_HEIGHT;
			final float text_x = frame_left + STROKE;
			float text_y = frame_bottom - STROKE * 4;
			StringBuilder draw_text = new StringBuilder(mTimestamp + "\t" + mUserName + "\t");
			if (mGsensorData.getSize() > 0) {
				if (mGsensorData.isSaved) {
					draw_text.append(mGsensorData.getFileName());
				} else {
					draw_text.append("*" + mGsensorData.getFileName());
				}
			}
			if (mComment.length() > 0) {
				frame_bottom = TIMESTAMP_HEIGHT * 2;
			}
			mCursorCanvas.drawRect(frame_left, frame_top, frame_right, frame_bottom, framePaint);

			textPaint.setTextSize(FONTSIZE);
			mCursorCanvas.drawText(draw_text.toString(), text_x, text_y, textPaint);
			if (mComment.length() > 0) {
				text_y = frame_bottom - STROKE * 4;
				mCursorCanvas.drawText(mComment, text_x, text_y, textPaint);
			}
		}
	}
}
