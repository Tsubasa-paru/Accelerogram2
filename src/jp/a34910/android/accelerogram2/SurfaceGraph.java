package jp.a34910.android.accelerogram2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceGraph extends SurfaceView implements SurfaceHolder.Callback {
	static private final String TAG = MainActivity.APP_NAME + "SurfaceGraph";
	static private final float ZOOM_MAX = 6.0f;
	static private final float ZOOM_MIN = 1.0f;
	static private final int DRAW_PERIOD = 50;

	private SurfaceHolder mSurfaceHolder;
	private Bitmap mGraphBitmap;
	private Canvas mGraphCanvas;
	private GsensorData mGsensorData;
	private DrawGraphTask mDrawGraphTask;

	private boolean mEnableDrawGraphFlag = false;
	private int mPosition;
	private float mZoom;
	private int mWidth;
	private int mHeight;
	private Graph.Align mGraphAlign;
	private Graph.Mode mGraphMode;

	static public class Graph {
		public enum Align {
			RIGHT,
			CENTER,
			LEFT,
		}
		public enum Mode {
			OVERLAP,
			SEPARATE,
		}
	}

	public SurfaceGraph(Context context) {
		super(context);
		initSurfaceGraph();
	}

	public SurfaceGraph(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSurfaceGraph();
	}

	public SurfaceGraph(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSurfaceGraph();
	}

	/**
	 * グラフ描画用SurfaceViewの初期化
	 */
	private void initSurfaceGraph() {
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
		setFocusable(true);
		this.setZoom(3.0f);
		this.setGraphAlign(Graph.Align.CENTER);
		this.setGraphMode(Graph.Mode.OVERLAP);
		this.mDrawGraphTask = new DrawGraphTask(DRAW_PERIOD);
		this.mGsensorData = null;
		this.mPosition = 0;
	}

	/**
	 * グラフ描画の許可／禁止
	 * @param enable true:グラフを描画する/false:グラフを描画しない（背景のみ描画）
	 * @return グラフ描画の許可/禁止の状態
	 */
	public boolean enableDrawGraph(boolean enable) {
		mEnableDrawGraphFlag = enable;
		return mEnableDrawGraphFlag;
	}

	/**
	 * グラフのズームIN/OUTを指定する
	 * @param zoom ズーム倍率
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
	 * 描画するグラフのAlignを設定する
	 * @param align RIGHT:右寄せ/CENTER:中央/LEFT:左寄せ
	 * @return 設定したAlign
	 */
	public Graph.Align setGraphAlign(Graph.Align align) {
		this.mGraphAlign = align;
		return this.mGraphAlign;
	}

	/**
	 * 描画するグラフのモードを設定する
	 * @param mode OVERLAP:縦方向・横方向のGを合成して描画/SEPARATE:縦方向・横方向のGを個別に描画
	 * @return 設定したモード
	 */
	public Graph.Mode setGraphMode(Graph.Mode mode) {
		this.mGraphMode = mode;
		return this.mGraphMode;
	}

	/**
	 * 描画するグラフのG-sensorオブジェクトを設定する
	 * @param gsensorData G-sensorオブジェクト
	 */
	public void setGsensorData(GsensorData gsensorData) {
		this.mGsensorData = gsensorData;
	}

	/**
	 * 描画するグラフの現在位置を設定する
	 * @param position 現在位置
	 */
	public void setPosition(int position) {
		this.mPosition = position;
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
		mGraphBitmap = Bitmap.createBitmap((int) (mWidth * ZOOM_MAX ), mHeight, Bitmap.Config.ARGB_8888);
		mGraphCanvas = new Canvas(mGraphBitmap);
		mDrawGraphTask.execute();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		synchronized (mSurfaceHolder) {
			mDrawGraphTask.cancel();
		}
		Log.d(TAG, "surface destroyed");
	}

	/**
	 * グラフを描画する周期タスク
	 */
	private class DrawGraphTask extends PeriodicTask {
		private final float STROKE = 1.5f;
		private Paint scalePaint;
		private Paint axisPaint;
		private Paint acceleratePaint;
		private Paint deceleratePaint;
		private Paint lateralPaint;
		private Paint composedPaint;

		public DrawGraphTask(long period) {
			super(period);
			initDrawGraphTask();
		}

		/**
		 * 描画タスクの初期化
		 */
		private void initDrawGraphTask() {
			acceleratePaint = new Paint();
			acceleratePaint.setColor(Color.RED);
			acceleratePaint.setAlpha(128);
			acceleratePaint.setAntiAlias(true);

			deceleratePaint = new Paint();
			deceleratePaint.setColor(Color.BLUE);
			deceleratePaint.setAlpha(128);
			deceleratePaint.setAntiAlias(true);

			lateralPaint = new Paint();
			lateralPaint.setColor(Color.GREEN);
			lateralPaint.setAlpha(128);
			lateralPaint.setAntiAlias(true);

			composedPaint = new Paint();
			composedPaint.setColor(Color.YELLOW);
			composedPaint.setAlpha(128);
			composedPaint.setAntiAlias(true);

			scalePaint = new Paint();
			scalePaint.setColor(Color.CYAN);
			scalePaint.setAlpha(128);
			scalePaint.setAntiAlias(true);

			axisPaint = new Paint();
			axisPaint.setColor(Color.YELLOW);
			axisPaint.setAlpha(255);
			axisPaint.setAntiAlias(true);
		}

		@Override
		protected void periodicTaskOnUIThread() {
			// Do nothing
		}

		@Override
		protected void periodicTaskOnTimerThread() {
			int width = mGraphBitmap.getWidth();
			int height = mGraphBitmap.getHeight();

			mGraphCanvas.drawColor(Color.BLACK);

			switch (mGraphMode) {
			case OVERLAP:
				if (mEnableDrawGraphFlag) {
					drawOverlapGraph(width, height);
				}
				drawOverlapGraphScale(width, height);
				break;
			case SEPARATE:
				if (mEnableDrawGraphFlag) {
					drawSeparateGraph(width, height);
				}
				drawSeparateGraphScale(width, height);
			default:
				break;
			}

			synchronized (mSurfaceHolder) {
				Canvas sfcanvas = mSurfaceHolder.lockCanvas();
				if (sfcanvas != null) {
					Rect src;
					Rect dst = new Rect(0, 0, mWidth, mHeight);
					float zoom = ZOOM_MAX / mZoom;
					switch (mGraphAlign) {
					case RIGHT:
						src = new Rect((int) (width - mWidth * zoom), 0, width, height);
						break;
					case CENTER:
						int center = width / 2;
						src = new Rect((int)(center - (mWidth / 2) * zoom), 0, (int)(center + (mWidth / 2) * zoom), height);
						break;
					case LEFT:
						src = new Rect(0, 0, (int) (mWidth * zoom), height);
						break;
					default:
						src = new Rect(0, 0, width, height);
						break;
					}
					sfcanvas.drawColor(0, PorterDuff.Mode.CLEAR);
					sfcanvas.drawBitmap(mGraphBitmap, src, dst, null);
					mSurfaceHolder.unlockCanvasAndPost(sfcanvas);
				}
			}
		}

		/**
		 * 縦方向・横方向のGグラフを個別にグラフ描画する
		 * @param width 描画するCanvasの幅
		 * @param height 描画するCanvasの高さ
		 */
		private void drawSeparateGraph(int width, int height) {
			int period = (int)(1000 / MainActivity.SENSOR_TASK_PERIOD);
			float xstep = (float)width / (60.0f * period);
			float ystep = (float)height / 20.0f;
			float longitudinalG0 = ((float)height / 4.0f) * 3.0f;
			float lateralG0 = (float)height / 4.0f;

			if (mGsensorData != null) {
				int size;
				int index;
				PointF gsensor;
				float longitudinalG;
				float lateralG;
				Paint longitudinalPaint;
				float strokeWidth = xstep * 2f;
				acceleratePaint.setStrokeWidth(strokeWidth);
				deceleratePaint.setStrokeWidth(strokeWidth);
				lateralPaint.setStrokeWidth(strokeWidth);
				if (mGsensorData != null) {
					size = mGsensorData.getSize();
				} else {
					size = 0;
				}
				switch (mGraphAlign) {
				case RIGHT:
					index = mPosition;
					for (float x = width; x >= 0; x -= xstep) {
						if (mGsensorData == null) break;
						if (index > 0 ) {
							gsensor = mGsensorData.getGsensor(index--);
							longitudinalG = gsensor.y;
							lateralG = gsensor.x;
						} else {
							longitudinalG = 0;
							lateralG = 0;
						}
						if (longitudinalG >= 0) {
							longitudinalPaint = acceleratePaint;
						} else {
							longitudinalPaint = deceleratePaint;
						}
						mGraphCanvas.drawLine(x, longitudinalG0, x, longitudinalG0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(x, lateralG0, x, lateralG0 + (ystep * lateralG), lateralPaint);
					}
					break;
				case CENTER:
					float center = (float)width / 2;
					index = 0;
					for (float x = 0; x <= center; x += xstep, index++) {
						int befor = mPosition - index;
						int after = mPosition + index;
						if (mGsensorData == null) break;
						if (befor >= 0) {
							gsensor = mGsensorData.getGsensor(befor);
							longitudinalG = gsensor.y;
							lateralG = gsensor.x;
						} else {
							longitudinalG = 0;
							lateralG = 0;
						}
						if (longitudinalG >= 0) {
							longitudinalPaint = acceleratePaint;
						} else {
							longitudinalPaint = deceleratePaint;
						}
						mGraphCanvas.drawLine(center - x, longitudinalG0, center - x, longitudinalG0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(center - x, lateralG0, center - x, lateralG0 + (ystep * lateralG), lateralPaint);
						if (mGsensorData == null) break;
						if (after < size) {
							gsensor = mGsensorData.getGsensor(after);
							longitudinalG = gsensor.y;
							lateralG = gsensor.x;
						} else {
							longitudinalG = 0;
							lateralG = 0;
						}
						if (longitudinalG >= 0) {
							longitudinalPaint = acceleratePaint;
						} else {
							longitudinalPaint = deceleratePaint;
						}
						mGraphCanvas.drawLine(center + x, longitudinalG0, center + x, longitudinalG0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(center + x, lateralG0, center + x, lateralG0 + (ystep * lateralG), lateralPaint);
					}
					break;
				case LEFT:
					index = mPosition;
					for (float x = 0; x <= width ; x += xstep) {
						if (mGsensorData == null) break;
						if (index < size) {
							gsensor = mGsensorData.getGsensor(index++);
							longitudinalG = gsensor.y;
							lateralG = gsensor.x;
						} else {
							longitudinalG = 0;
							lateralG = 0;
						}
						if (longitudinalG >= 0) {
							longitudinalPaint = acceleratePaint;
						} else {
							longitudinalPaint = deceleratePaint;
						}
						mGraphCanvas.drawLine(x, longitudinalG0, x, longitudinalG0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(x, lateralG0, x, lateralG0 + (ystep * lateralG), lateralPaint);
					}
					break;
				default:
					break;
				}
			}
		}

		/**
		 * 縦方向・横方向を分離したスケールを描画する
		 * @param width 描画するCanvasの幅
		 * @param height 描画するCanvasの高さ
		 */
		private void drawSeparateGraphScale(int width, int height) {
			int xstep = width / 60;
			int ystep = height / 20;
			int separateLine = height / 2;
			float strokeWidth = STROKE * (ZOOM_MAX / mZoom);
			scalePaint.setStrokeWidth(strokeWidth);
			axisPaint.setStrokeWidth(strokeWidth);

			switch (mGraphAlign) {
			case RIGHT:
				for (int x = width; x >= 0; x -= xstep) {
					mGraphCanvas.drawLine(x, 0, x, height, scalePaint);
				}
				mGraphCanvas.drawLine(width - strokeWidth, 0, width - strokeWidth, height, axisPaint);
				break;
			case CENTER:
				int center = width / 2;
				for (int x = 0; x <= center; x += xstep) {
					mGraphCanvas.drawLine(center + x, 0, center + x, height, scalePaint);
					mGraphCanvas.drawLine(center - x, 0, center - x, height, scalePaint);
				}
				mGraphCanvas.drawLine(center, 0, center, height, axisPaint);
				break;
			case LEFT:
				for (int x = 0; x <= width ; x += xstep) {
					mGraphCanvas.drawLine(x, 0, x, height, scalePaint);
				}
				mGraphCanvas.drawLine(strokeWidth, 0, strokeWidth, height, axisPaint);
				break;
			default:
				break;
			}
			axisPaint.setStrokeWidth(STROKE);
			scalePaint.setStrokeWidth(STROKE);
			for (int y = 0; y <= separateLine; y += ystep) {
				if (y == (ystep * 5)) {
					mGraphCanvas.drawLine(0, separateLine + y, width, separateLine + y, axisPaint);
					mGraphCanvas.drawLine(0, separateLine - y, width, separateLine - y, axisPaint);
				} else {
					mGraphCanvas.drawLine(0, separateLine + y, width, separateLine + y, scalePaint);
					mGraphCanvas.drawLine(0, separateLine - y, width, separateLine - y, scalePaint);
				}
			}
		}

		/**
		 * 縦方向・横方向のGを合成してグラフ描画する
		 * @param width 描画するCanvasの幅
		 * @param height 描画するCanvasの高さ
		 */
		private void drawOverlapGraph(int width, int height) {
			int period = (int)(1000 / MainActivity.SENSOR_TASK_PERIOD);
			float xstep = (float)width / (60.0f * period);
			float ystep = (float)height / 5.0f;
			float g0 = (float)height;

			if (mGsensorData != null) {
				int size;
				int index;
				PointF gsensor;
				float longitudinalG;
				float lateralG;
				Paint longitudinalPaint;
				float strokeWidth = xstep * 2f;
				acceleratePaint.setStrokeWidth(strokeWidth);
				deceleratePaint.setStrokeWidth(strokeWidth);
				lateralPaint.setStrokeWidth(strokeWidth);
				composedPaint.setStrokeWidth(STROKE * (ZOOM_MAX / mZoom * 2));
				if (mGsensorData != null) {
					size = mGsensorData.getSize();
				} else {
					size = 0;
				}
				switch (mGraphAlign) {
				case RIGHT:
					index = mPosition;
					for (float x = width; x >= 0; x -= xstep) {
						if (mGsensorData == null) break;
						if (index > 0 ) {
							gsensor = mGsensorData.getGsensor(index--);
							if (gsensor.y >= 0) {
								longitudinalPaint = acceleratePaint;
							} else {
								longitudinalPaint = deceleratePaint;
							}
							longitudinalG = Math.abs(gsensor.y);
							lateralG = Math.abs(gsensor.x);
						} else {
							longitudinalG = 0;
							lateralG = 0;
							longitudinalPaint = acceleratePaint;
						}
						mGraphCanvas.drawLine(x, g0, x, g0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(x, g0, x, g0 - (ystep * lateralG), lateralPaint);
						mGraphCanvas.drawPoint(x, g0 - (ystep * ((float)Math.sqrt(longitudinalG * longitudinalG + lateralG * lateralG))), composedPaint);
					}
					break;
				case CENTER:
					float center = (float)width / 2;
					index = 0;
					for (float x = 0; x <= center; x += xstep, index++) {
						int befor = mPosition - index;
						int after = mPosition + index;
						if (mGsensorData == null) break;
						if (befor >= 0) {
							gsensor = mGsensorData.getGsensor(befor);
							if (gsensor.y >= 0) {
								longitudinalPaint = acceleratePaint;
							} else {
								longitudinalPaint = deceleratePaint;
							}
							longitudinalG = Math.abs(gsensor.y);
							lateralG = Math.abs(gsensor.x);
						} else {
							longitudinalG = 0;
							lateralG = 0;
							longitudinalPaint = acceleratePaint;
						}
						mGraphCanvas.drawLine(center - x, g0, center - x, g0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(center - x, g0, center - x, g0 - (ystep * lateralG), lateralPaint);
						mGraphCanvas.drawPoint(center - x, g0 - (ystep * ((float)Math.sqrt(longitudinalG * longitudinalG + lateralG * lateralG))), composedPaint);
						if (mGsensorData == null) break;
						if (after < size) {
							gsensor = mGsensorData.getGsensor(after);
							if (gsensor.y >= 0) {
								longitudinalPaint = acceleratePaint;
							} else {
								longitudinalPaint = deceleratePaint;
							}
							longitudinalG = Math.abs(gsensor.y);
							lateralG = Math.abs(gsensor.x);
						} else {
							longitudinalG = 0;
							lateralG = 0;
							longitudinalPaint = acceleratePaint;
						}
						mGraphCanvas.drawLine(center + x, g0, center + x, g0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(center + x, g0, center + x, g0 - (ystep * lateralG), lateralPaint);
						mGraphCanvas.drawPoint(center + x, g0 - (ystep * ((float)Math.sqrt(longitudinalG * longitudinalG + lateralG * lateralG))), composedPaint);
					}
					break;
				case LEFT:
					index = mPosition;
					for (float x = 0; x <= width ; x += xstep) {
						if (mGsensorData == null) break;
						if (index < size) {
							gsensor = mGsensorData.getGsensor(index++);
							if (gsensor.y >= 0) {
								longitudinalPaint = acceleratePaint;
							} else {
								longitudinalPaint = deceleratePaint;
							}
							longitudinalG = Math.abs(gsensor.y);
							lateralG = Math.abs(gsensor.x);
						} else {
							longitudinalG = 0;
							lateralG = 0;
							longitudinalPaint = acceleratePaint;
						}
						mGraphCanvas.drawLine(x, g0, x, g0 - (ystep * longitudinalG), longitudinalPaint);
						mGraphCanvas.drawLine(x, g0, x, g0 - (ystep * lateralG), lateralPaint);
						mGraphCanvas.drawPoint(x, g0 - (ystep * ((float)Math.sqrt(longitudinalG * longitudinalG + lateralG * lateralG))), composedPaint);
					}
					break;
				default:
					break;
				}
			}
		}

		/**
		 * 縦方向・横方向を合成したスケールを描画する
		 * @param width 描画するCanvasの幅
		 * @param height 描画するCanvasの高さ
		 */
		private void drawOverlapGraphScale(int width, int height) {
			float xstep = (float)width / 60;
			float ystep = (float)height / 5;
			float g0 = (float)height;
			float strokeWidth = STROKE * (ZOOM_MAX / mZoom);
			scalePaint.setStrokeWidth(strokeWidth);
			axisPaint.setStrokeWidth(strokeWidth);

			switch (mGraphAlign) {
			case RIGHT:
				for (float x = width; x >= 0; x -= xstep) {
					mGraphCanvas.drawLine(x, 0, x, height, scalePaint);
				}
				mGraphCanvas.drawLine(width - strokeWidth, 0, width - strokeWidth, height, axisPaint);
				break;
			case CENTER:
				float center = (float)width / 2;
				for (float x = 0; x <= center; x += xstep) {
					mGraphCanvas.drawLine(center + x, 0, center + x, height, scalePaint);
					mGraphCanvas.drawLine(center - x, 0, center - x, height, scalePaint);
				}
				mGraphCanvas.drawLine(center, 0, center, height, axisPaint);
				break;
			case LEFT:
				for (float x = 0; x <= width ; x += xstep) {
					mGraphCanvas.drawLine(x, 0, x, height, scalePaint);
				}
				mGraphCanvas.drawLine(strokeWidth, 0, strokeWidth, height, axisPaint);
				break;
			default:
				break;
			}
			axisPaint.setStrokeWidth(STROKE);
			scalePaint.setStrokeWidth(STROKE);
			for (float y = 0; y <= g0; y += ystep) {
				if (y == 0) {
					mGraphCanvas.drawLine(0, g0 - strokeWidth, width, g0 - strokeWidth, axisPaint);
				} else {
					mGraphCanvas.drawLine(0, g0 - y, width, g0 - y, scalePaint);
				}
			}
		}
	}
}
