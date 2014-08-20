package jp.a34910.android.accelerogram2;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.a34910.android.accelerogram2.SensorRecordTask.Status;
import jp.a34910.android.accelerogram2.SurfaceCursor.Cursor;
import jp.a34910.android.accelerogram2.SurfaceGraph.Graph;
import jp.a34910.android.accelerogram2.SurfaceGraph.Graph.*;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class MainActivity extends ActionBarActivity implements OnClickListener,OnLongClickListener {
	static final String APP_NAME = "Accelerogram2/";
	static final long SENSOR_TASK_PERIOD = 50; //G-Sensor Sampling period = 50ms(fs:20Hz)
	static final String TAG = APP_NAME + "MainActivity";
	static final String DEFAULT_USERNAME = "TEST";
	static final String DEFAULT_FILENAME = "FileName";
	static final String FILE_EXTENSION = ".zip";
	static final String USER_EXTENSION = ".dir";
	static final float DEFAULT_ZOOM = 3.0f;

	static private ImageButton mRecBtn;
	static private ImageButton mReplayBtn;
	static private ImageButton mStopBtn;
	static private ImageButton mToggleGraphBtn;
	static private ImageButton mCalibrationBtn;
	static private WebView mMapView;
	static private SurfaceCursor mSurfaceCursor;
	static private SurfaceGraph mSurfaceGraph;
	static private SurfaceGraph mCompareGraph;
	static private LinearLayout mSurfaceLayout;

	static private LinearLayout mCommanderView;

	static private Activity mThisActivity;
	private int mScreenWidth;
	private int mScreenHeight;
	private File mSDCardPath;
	private String mUserName = DEFAULT_USERNAME;
	private ArrayList<String> mUserNameList;
	private ArrayList<String> mFileNameList;

	static private boolean mDrawTracksFlag = false;
	static private boolean mDrawGraphFlag = false;
	static private boolean mMapViewVisibilityFlag = true;

	static private SensorManager mSensorManager = null;
	static private LocationManager mLocationManager = null;
	private String mProvider;

	private SensorRecordTask mSensorRecordTask = null;
	private GsensorData mGsensorData = null;
	private SensorRecordTask.Status mStatus = Status.IDLE;
	private SensorRecordTask mCompareSensorTask = null;
	private boolean mForcusCompareFlag = false;

	private float mCursorZoom =DEFAULT_ZOOM;
	private float mGraphZoom = DEFAULT_ZOOM;

	private Graph.Mode mGraphMode = Mode.OVERLAP;
	private Cursor.Background mCursorBackground = Cursor.Background.CIRCLE;

	private class ButtonsBitmap {
		Bitmap rec_white;
		Bitmap rec_red;
		Bitmap replay_green;
		Bitmap replay_white;
		Bitmap pause_green;
	}
	private ButtonsBitmap mButtonsBitmap;
	/**
	 *  G-sensor/GPSの記録（再生）結果を画面に反映させるためのListener
	 *  onPeriodicLocation:1秒毎のコールバック（GPS位置情報を通知）
	 *  onPeriodicGsensor:50ms毎のコールバック（G-sensorの情報を通知）
	 */
	private SensorRecordTask.SensorListener mSensorListener = new SensorRecordTask.SensorListener() {
		@Override
		public void onPeriodicLocation(Location location) {
			if (!mForcusCompareFlag && mMapViewVisibilityFlag && location != null) {
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
				mMapView.loadUrl("javascript:moveTo(" + latitude +"," + longitude + ")");
//				Log.d(TAG + ":onPeriodicLocation", "latitude:" + latitude + " / logitude:" + longitude);
			}
		}
		@Override
		public void onPeriodicGsensor(PointF currentGsensor, int position) {
			if (mStatus == Status.IDLE) {
				mSurfaceCursor.setCursorPosition(currentGsensor);
			} else if (mStatus == Status.REPLAY || mStatus == Status.PAUSE) {
				if (!mForcusCompareFlag) {
					mSurfaceCursor.setCursorPosition(position);
					mSurfaceCursor.setTimestamp(mGsensorData.getTimeStamp(position));
				}
				mSurfaceGraph.setPosition(position);
			} else if (mStatus == Status.REC) {
				mSurfaceCursor.setCursorPosition(position);
				mSurfaceCursor.setTimestamp(mGsensorData.getTimeStamp(position));
				mSurfaceGraph.setPosition(position);
			} else if (mStatus == Status.CALIBRATION) {
				mStatus = Status.IDLE;
			}
		}
	};

	private SensorRecordTask.SensorListener mCompareListener = new SensorRecordTask.SensorListener() {
		@Override
		public void onPeriodicLocation(Location location) {
			if (mForcusCompareFlag && mMapViewVisibilityFlag && location != null) {
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
				mMapView.loadUrl("javascript:moveTo(" + latitude +"," + longitude + ")");
//				Log.d(TAG + ":onPeriodicLocation", "latitude:" + latitude + " / logitude:" + longitude);
			}
		}
		@Override
		public void onPeriodicGsensor(PointF currentGsensor, int position) {
			if (mStatus == Status.REPLAY || mStatus == Status.PAUSE) {
				if (mForcusCompareFlag) {
					mSurfaceCursor.setCursorPosition(position);
					mSurfaceCursor.setTimestamp(mCompareSensorTask.getGsensorData().getTimeStamp(position));
				}
				if (mCompareGraph != null) {
					mCompareGraph.setPosition(position);
				}
			}
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		mThisActivity = this;
		mSensorManager = (SensorManager) mThisActivity.getSystemService(Context.SENSOR_SERVICE);
		mLocationManager = (LocationManager)mThisActivity.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		mProvider = mLocationManager.getBestProvider(criteria, true);

		mSensorRecordTask = new SensorRecordTask(SENSOR_TASK_PERIOD, mSensorListener);

		mSDCardPath = new File(Environment.getExternalStorageDirectory(), APP_NAME);
		if (mSDCardPath != null && !mSDCardPath.exists()) {
			mSDCardPath.mkdir();
		}
		mUserNameList = setupUserNameList(mSDCardPath);
		mFileNameList = setupFileNameList(mSDCardPath);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
			WindowManager wm = mThisActivity.getWindowManager();
			Point size = new Point();
			wm.getDefaultDisplay().getSize(size);
		    mScreenWidth = size.x;
		    mScreenHeight = (int) (size.y - (48 * 3.375f)); //FIXME ナビゲーションバーの高さをハードコーディング(48dp * 3.375)
		}

		mButtonsBitmap = new ButtonsBitmap();
		mButtonsBitmap.rec_white = BitmapFactory.decodeResource(getResources(), R.drawable.ic_rec);
		mButtonsBitmap.rec_red = BitmapFactory.decodeResource(getResources(), R.drawable.ic_rec_red);
		mButtonsBitmap.replay_white = BitmapFactory.decodeResource(getResources(), R.drawable.ic_replay);
		mButtonsBitmap.replay_green = BitmapFactory.decodeResource(getResources(), R.drawable.ic_replay_green);
		mButtonsBitmap.pause_green = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pause_green);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			mSurfaceCursor = (SurfaceCursor)rootView.findViewById(R.id.surfaceCursor);
			mSurfaceGraph = (SurfaceGraph)rootView.findViewById(R.id.surfaceGraph);
			mRecBtn = (ImageButton)rootView.findViewById(R.id.rec_btn);
			mReplayBtn = (ImageButton)rootView.findViewById(R.id.replay_btn);
			mStopBtn = (ImageButton)rootView.findViewById(R.id.stop_btn);
			mCalibrationBtn = (ImageButton)rootView.findViewById(R.id.calibration_btn);
			mCommanderView = (LinearLayout)rootView.findViewById(R.id.commander_layout);
			mToggleGraphBtn = (ImageButton)rootView.findViewById(R.id.gmode_btn);
			mMapView = (WebView)rootView.findViewById(R.id.map_view);
			mSurfaceLayout = (LinearLayout)rootView.findViewById(R.id.surfaceLayout);
			return rootView;
		}
	}

	/**
	 * SDカードからファイルのリストを作り、ファイル名からUser名を抜き出してListを作成する
	 * @return User名のArrayList
	 */
	private ArrayList<String> setupUserNameList(File sdcard) {
		ArrayList<String> usernameList = new ArrayList<String>();
		HashSet<String > usernameHashList = new HashSet<String>();
		usernameHashList.add(DEFAULT_USERNAME);
		if (sdcard != null) {
			FilenameFilter filter_user = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(USER_EXTENSION);
				}
			};
			String[] filelist_user = sdcard.list(filter_user);
			for (String filename: filelist_user) {
				Matcher extension = Pattern.compile("\\" + USER_EXTENSION + "$").matcher(filename);
				usernameHashList.add(extension.replaceAll(""));
			}
			FilenameFilter filter_file = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(FILE_EXTENSION);
				}
			};
			String[] filelist = sdcard.list(filter_file);
			for (String filename: filelist) {
				Matcher usernameMatcher = Pattern.compile("\\[.*\\]").matcher(filename);
				if (usernameMatcher.find()) {
					usernameHashList.add(usernameMatcher.group().replaceAll("[\\[\\]]", ""));
				}
			}
			usernameList.addAll(usernameHashList);
			Collections.sort(usernameList);
		}
		return usernameList;
	}

	/**
	 * SDカード上のファイルのリストを作成する
	 * @return ファイル名のArrayList
	 */
	private ArrayList<String> setupFileNameList(File sdcard) {
		ArrayList<String > filenameList = new ArrayList<String>();
		if (sdcard != null) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(FILE_EXTENSION);
				}
			};
			String[] filelist = sdcard.list(filter);
			for (String filename: filelist) {
				Matcher extension = Pattern.compile("\\" + FILE_EXTENSION + "$").matcher(filename);
				filenameList.add(extension.replaceAll(""));
			}
			Collections.sort(filenameList);
		}
		return filenameList;
	}

	/**
	 * SurfaceView上をタッチした時に行う画面上の動作を決める
	 * setZoom:SurfaceView画面の拡大・縮小
	 * movePosition:再生（一時停止）状態なら再生位置を移動する
	 * skipPosition:再生（一時停止）状態なら再生位置をスキップする
	 */
	private SurfaceViewOnTouchListener.OnTouchAction mOnTouchAction = new SurfaceViewOnTouchListener.OnTouchAction() {
		@Override
		public void setZoom(View view, float zoomAdjust) {
			if (view == mSurfaceCursor) {
				mCursorZoom = mSurfaceCursor.setZoom(mCursorZoom * zoomAdjust);
			} else if (view == mSurfaceGraph) {
				mGraphZoom = mSurfaceGraph.setZoom(mGraphZoom * zoomAdjust);
				if (mCompareGraph != null) {
					mCompareGraph.setZoom(mGraphZoom);
				}
			} else if (view == mCompareGraph) {
				mGraphZoom = mCompareGraph.setZoom(mGraphZoom * zoomAdjust);
				mSurfaceGraph.setZoom(mGraphZoom);
			}
		}
		@Override
		public void movePosition(View view, float positionAdjust) {
			if (view == mSurfaceCursor || view == mSurfaceGraph) {
				if (mStatus == Status.REPLAY || mStatus == Status.PAUSE) {
					mForcusCompareFlag = false;
					mSurfaceCursor.setGsensorData(mGsensorData);
					mSensorRecordTask.setPosition((int)(mSensorRecordTask.getPosition() + positionAdjust));
					if (mCompareSensorTask != null) {
						mCompareSensorTask.setPosition((int)(mCompareSensorTask.getPosition() + positionAdjust));
					}
				}
			} else if (mCompareSensorTask != null && mCompareGraph != null && view == mCompareGraph) {
				if (mStatus == Status.PAUSE) {
					mForcusCompareFlag = true;
					mSurfaceCursor.setGsensorData(mCompareSensorTask.getGsensorData());
					mCompareSensorTask.setPosition((int)(mCompareSensorTask.getPosition() + positionAdjust));
				}
			}
		}
		@Override
		public void skipPosition(View view, float distance_from_center) {
			float adjust = 10 * (1000 / SENSOR_TASK_PERIOD);
			if (distance_from_center > 0) {
				adjust = -adjust;
			}
			if (view == mSurfaceCursor || view == mSurfaceGraph) {
				if (mStatus == Status.REPLAY || mStatus == Status.PAUSE) {
					mForcusCompareFlag = false;
					mSurfaceCursor.setGsensorData(mGsensorData);
					mSensorRecordTask.setPosition((int)(mSensorRecordTask.getPosition() + adjust));
					if (mCompareSensorTask != null) {
						mCompareSensorTask.setPosition((int)(mCompareSensorTask.getPosition() + adjust));
					}
				}
			} else if (mCompareSensorTask != null && mCompareGraph != null && view == mCompareGraph) {
				if (mStatus == Status.PAUSE) {
					mForcusCompareFlag = true;
					mSurfaceCursor.setGsensorData(mCompareSensorTask.getGsensorData());
					mCompareSensorTask.setPosition((int)(mCompareSensorTask.getPosition() + adjust));
				}
			}
		}
	};
	/**
	 * Commander/MapViewをドラッグしたときに移動する
	 */
	final OnTouchListener mDragMovingListener = new OnTouchListener() {
		private float touchedX;
		private float touchedY;
		private int viewLeft;
		private int viewTop;
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touchedX = event.getRawX();
				touchedY = event.getRawY();
				viewLeft = v.getLeft();
				viewTop = v.getTop();
				return true;
			case MotionEvent.ACTION_MOVE:
				int left = viewLeft + (int)(event.getRawX() - touchedX);
				int top = viewTop + (int)(event.getRawY() - touchedY);
				int viewWidth = v.getWidth();
				int viewHeight = v.getHeight();
				if (left < 0) {
					left = 0;
				}
				if (left > (mScreenWidth - viewWidth)){
					left = mScreenWidth - viewWidth;
				}
				if (top < 0) {
					top = 0;
				}
				if (top > (mScreenHeight - viewHeight)) {
					top = mScreenHeight - viewHeight;
				}
				v.layout(left, top, left + viewWidth, top + viewHeight);
				return true;
			default:
				break;
			}
			return false;
		}
	};

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		mGsensorData = mSensorRecordTask.getGsensorData();
		mSensorRecordTask.setSurfaceCursor(mSurfaceCursor);
		mSensorRecordTask.setSurfaceGraph(mSurfaceGraph);

		mGsensorData.setUserName(mUserName);
		mSurfaceCursor.setGsensorData(mGsensorData);

		mSurfaceCursor.setBackgroundMode(mCursorBackground);
		Cursor.Bitmap_GREEN = BitmapFactory.decodeResource(getResources(), R.drawable.ic_center_gravity_green);
		Cursor.Bitmap_RED = BitmapFactory.decodeResource(getResources(), R.drawable.ic_center_gravity_red);
		mSurfaceCursor.enableTracks(mDrawTracksFlag);

		mRecBtn.setOnClickListener(this);
		mReplayBtn.setOnClickListener(this);
		mStopBtn.setOnClickListener(this);
		mToggleGraphBtn.setOnClickListener(this);
		mCalibrationBtn.setOnLongClickListener(this);
		mCommanderView.setOnTouchListener(mDragMovingListener);

		mDrawGraphFlag = mSurfaceGraph.enableDrawGraph(mDrawGraphFlag);
		mGraphMode = mSurfaceGraph.setGraphMode(mGraphMode);

		List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for (Sensor sensor:sensors) {
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				mSensorManager.registerListener(mSensorRecordTask, sensor, SensorManager.SENSOR_DELAY_GAME);
				Log.d(TAG, "Register G-Sensor Listener");
			}
		}
		mMapView.getSettings().setJavaScriptEnabled(true);
		mMapView.loadUrl("file:///android_asset/map.html");
		mMapView.setOnTouchListener(mDragMovingListener);
		if (mLocationManager.getLastKnownLocation(mProvider) != null) {
			Location location = mLocationManager.getLastKnownLocation(mProvider);
			mSensorRecordTask.onLocationChanged(location);
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			mMapView.loadUrl("javascript:moveTo(" + latitude +"," + longitude + ")");
		}
		mLocationManager.requestLocationUpdates(mProvider, SENSOR_TASK_PERIOD, 0, mSensorRecordTask);

		mSurfaceCursor.setZoom(mCursorZoom);
		SurfaceViewOnTouchListener cursorListener = new SurfaceViewOnTouchListener(mThisActivity, mSurfaceCursor, mOnTouchAction);
		mSurfaceCursor.setOnTouchListener(cursorListener);

		mSurfaceGraph.setZoom(mGraphZoom);
		SurfaceViewOnTouchListener graphListener = new SurfaceViewOnTouchListener(mThisActivity, mSurfaceGraph, mOnTouchAction);
		mSurfaceGraph.setOnTouchListener(graphListener);

		mSensorRecordTask.execute();
		if (mCompareSensorTask != null) {
			mCompareSensorTask.execute();
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		if (mCompareSensorTask != null) {
			mCompareSensorTask.cancel();
		}
		mSensorRecordTask.cancel();
		mSensorManager.unregisterListener(mSensorRecordTask);
		mLocationManager.removeUpdates(mSensorRecordTask);
		Log.d(TAG, "UnRegister G-Sensor Listener");
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick");
		if (v == mRecBtn) {
			if (mStatus == Status.IDLE) {
				moveStatusREC();
			}
		} else if (v == mReplayBtn) {
			if (mStatus == Status.IDLE && mGsensorData.getSize() > 0) {
				moveStatusREPLAY();
			} else if (mStatus == Status.REPLAY || mStatus == Status.PAUSE) {
				toggleStatusREPLAYorPAUSE();
			}
		} else if ( v == mStopBtn) {
			if (mStatus != Status.IDLE) {
				moveStatusSTOP();
			}
		} else if (v == mToggleGraphBtn) {
			switch (mGraphMode) {
			case OVERLAP:
				mGraphMode = mSurfaceGraph.setGraphMode(Graph.Mode.SEPARATE);
				break;
			case SEPARATE:
				mGraphMode = mSurfaceGraph.setGraphMode(Graph.Mode.OVERLAP);
				break;
			default:
				break;
			}
			if (mCompareGraph != null) {
				mCompareGraph.setGraphMode(mGraphMode);
			}
		}
	}

	/**
	 * REC状態に遷移する
	 */
	private void moveStatusREC() {
		if ((mStatus = mSensorRecordTask.setStatus(Status.REC)) == Status.REC) {
			mGsensorData = mSensorRecordTask.getGsensorData();
			mGsensorData.setUserName(mUserName);
			mGsensorData.isSaved = false;
			mDrawTracksFlag = mSurfaceCursor.enableTracks(true);

			mSurfaceGraph.setGraphAlign(Graph.Align.RIGHT);
			mDrawGraphFlag = mSurfaceGraph.enableDrawGraph(true);

			mCompareGraph = showCompareGraph(false);
			mRecBtn.setImageBitmap(mButtonsBitmap.rec_red);
		}
	}

	/**
	 * REPLAY状態に遷移する
	 */
	private void moveStatusREPLAY() {
		if ((mStatus = mSensorRecordTask.setStatus(Status.REPLAY)) == Status.REPLAY) {
			mDrawTracksFlag = mSurfaceCursor.enableTracks(true);
			mSurfaceGraph.setGraphAlign(Graph.Align.CENTER);
			mDrawGraphFlag = mSurfaceGraph.enableDrawGraph(true);
			if (mCompareSensorTask != null && mCompareGraph != null) {
				mCompareSensorTask.setStatus(Status.REPLAY);
				mCompareGraph.enableDrawGraph(true);
			}
			mReplayBtn.setImageBitmap(mButtonsBitmap.replay_green);
		}
	}

	/**
	 * 交互にREPLAY/PAUSE状態に切り替える
	 */
	private void toggleStatusREPLAYorPAUSE() {
		if (mStatus == Status.REPLAY) {
			if ((mStatus = mSensorRecordTask.setStatus(Status.PAUSE)) == Status.PAUSE) {
				if (mCompareSensorTask != null) {
					mCompareSensorTask.setStatus(Status.PAUSE);
				}
				mReplayBtn.setImageBitmap(mButtonsBitmap.pause_green);
			}
		} else if (mStatus == Status.PAUSE) {
			if ((mStatus = mSensorRecordTask.setStatus(Status.REPLAY)) == Status.REPLAY) {
				if (mCompareSensorTask != null) {
					mForcusCompareFlag = false;
					mCompareSensorTask.setStatus(Status.REPLAY);
				}
				mReplayBtn.setImageBitmap(mButtonsBitmap.replay_green);
			}
		}
	}

	/**
	 * STOP状態に遷移する
	 * RECからSTOPに遷移する場合、ファイル保存の確認ダイアログを表示する
	 */
	private void moveStatusSTOP () {
		SensorRecordTask.Status beforStatus = mStatus;
		if ((mStatus = mSensorRecordTask.setStatus(Status.IDLE)) == Status.IDLE) {
			mDrawTracksFlag = mSurfaceCursor.enableTracks(false);
			mSurfaceCursor.setTimestamp(mGsensorData.getTimeStamp(0));
			mSurfaceGraph.setGraphAlign(Graph.Align.CENTER);
			if (mGsensorData.getSize() > 0) {
				mDrawGraphFlag = mSurfaceGraph.enableDrawGraph(true);
			} else {
				mDrawGraphFlag = mSurfaceGraph.enableDrawGraph(false);
			}
			if (mCompareSensorTask != null && mCompareGraph != null) {
				mCompareSensorTask.setStatus(Status.IDLE);
				mCompareGraph.enableDrawGraph(true);
			}
			mRecBtn.setImageBitmap(mButtonsBitmap.rec_white);
			mReplayBtn.setImageBitmap(mButtonsBitmap.replay_white);
			switch (beforStatus) {
			case REPLAY:
			case PAUSE:
				break;
			case REC:
				ConfirmationDialog.ConfirmedListener doSaveToFile = new ConfirmationDialog.ConfirmedListener() {
					@Override
					public void onConfirmed(View view) {
						file_save(mSDCardPath, mUserName);
					}
				};
				ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity, "ファイルに保存しますか？");
				dialog.setOnConfirmListener("YES", doSaveToFile);
				dialog.setOnDeniedListener("NO", null);
				dialog.show();
			default:
			}
		}
	}

	@Override
	public boolean onLongClick(View v) {
		Log.d(TAG, "onLongClick");
		if (v == mCalibrationBtn) {
			if (mStatus == Status.IDLE) {
				mStatus = mSensorRecordTask.setStatus(Status.CALIBRATION);
				Log.d(TAG, "Start Calibration");
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		file_save_menu = menu.findItem(R.id.action_save);
		file_load_menu = menu.findItem(R.id.action_load);
		file_edit_menu = menu.findItem(R.id.file_edit_menu);
		return true;
	}

	private MenuItem file_save_menu;
	private MenuItem file_load_menu;
	private MenuItem file_edit_menu;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.file_menu:
			if (mSDCardPath != null && mStatus == Status.IDLE) {
				if (mGsensorData.getSize() > 0) {
					file_save_menu.setEnabled(true);
				} else {
					file_save_menu.setEnabled(false);
				}
				file_load_menu.setEnabled(true);
				file_edit_menu.setEnabled(true);
			} else {
				file_save_menu.setEnabled(false);
				file_load_menu.setEnabled(false);
				file_edit_menu.setEnabled(false);
			}
			return true;
		case R.id.action_comment:
			edit_comment();
			return true;
		case R.id.action_username:
			selectUserName(mSDCardPath);
			return true;
		case R.id.action_togglemap:
			if (mMapViewVisibilityFlag) {
				mMapView.setVisibility(View.INVISIBLE);
				mMapViewVisibilityFlag = false;
			} else {
				mMapView.setVisibility(View.VISIBLE);
				mMapViewVisibilityFlag = true;
			}
			return true;
		case R.id.action_save:
			if (mStatus == Status.IDLE && mGsensorData.getSize() > 0) {
				file_save(mSDCardPath, mUserName);
			}
			return true;
		case R.id.action_load:
			if (mStatus == Status.IDLE) {
				file_load(mSDCardPath);
			}
			return true;
		case R.id.action_select_compare:
			if (mCompareSensorTask == null) {
				mCompareSensorTask = new SensorRecordTask(SENSOR_TASK_PERIOD, mCompareListener);
			}
			select_compare(mSDCardPath);
			if (mCompareGraph != null) {
				mCompareGraph.setPosition(mCompareSensorTask.getPosition());
			}
			return true;
		case R.id.action_toggle_compare:
			if (mCompareSensorTask != null) {
				if (mCompareGraph == null) {
					mCompareGraph = showCompareGraph(true);
				} else {
					mCompareGraph = showCompareGraph(false);
				}
			} else {
				ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity, "比較データを選択してください", null);
				dialog.show();
			}
			return true;
		case R.id.action_set_default_zoom:
			mCursorZoom = mSurfaceCursor.setZoom(DEFAULT_ZOOM);
			mGraphZoom = mSurfaceGraph.setZoom(DEFAULT_ZOOM);
			if (mCompareGraph != null) {
				mCompareGraph.setZoom(DEFAULT_ZOOM);
			}
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * コメント文字列を編集するダイアログを表示
	 * コメントの編集後にGsensorDataにコメントを設定して表示する
	 */
	private void edit_comment() {
		final EditText edit = new EditText(mThisActivity);
		InputFilter[] inputFilter = new InputFilter[1];
		inputFilter[0] = new InputFilter.LengthFilter(20);
		edit.setSingleLine();
		edit.setFilters(inputFilter);
		ConfirmationDialog.ConfirmedListener editComplete = new ConfirmationDialog.ConfirmedListener() {
			@Override
			public void onConfirmed(View view) {
				EditText et = (EditText)view;
				String comment = et.getText().toString();
				mGsensorData.setComment(comment);
				mSurfaceCursor.setComment(comment);
			}
		};
		ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity);
		dialog.setTitle("Comment");
		dialog.setView(edit);
		dialog.setOnConfirmListener("OK", editComplete);
		dialog.setOnDeniedListener("Cancel", null);
		dialog.show();
	}

	/**
	 * User名選択のダイアログを表示し選択結果に応じて処理を行う
	 * 既存のUser名を選択：選択されたUser名に変更
	 * 新規のUser名を選択：新規User名を入力するダイアログを表示し入力したUser名に変更
	 *
	 * @param sdcard SDカードのパス
	 */
	private void selectUserName(final File sdcard) {
		ArrayList<String> usernameList = new ArrayList<String>();
		usernameList.addAll(mUserNameList);
		usernameList.add("NEW...");
		NameSelectDialog selectDialog = new NameSelectDialog(
				mThisActivity,
				"Select User",
				usernameList,
				new NameSelectDialog.SelectedListener() {
			@Override
			public void onSelected(String selected) {
				if (selected.equals("NEW...")) {
					final EditText edit = new EditText(mThisActivity);
					edit.setSingleLine();
					ConfirmationDialog.ConfirmedListener editComplete = new ConfirmationDialog.ConfirmedListener() {
						@Override
						public void onConfirmed(View view) {
							EditText et = (EditText)view;
							String selected = et.getText().toString();
							if (!mUserNameList.contains(selected)) {
								mUserNameList.add(selected);
								Collections.sort(mUserNameList);
								File newUser = new File(sdcard, selected + USER_EXTENSION);
								if (!newUser.exists()) {
									newUser.mkdir();
								}
							}
							mUserName = selected;
							mGsensorData.setUserName(selected);
							mGsensorData.isSaved = false;
							mSurfaceCursor.setUserName(selected);
						}
					};
					ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity);
					dialog.setTitle("New User");
					dialog.setView(edit);
					dialog.setOnConfirmListener("OK", editComplete);
					dialog.setOnDeniedListener("Cancel", null);
					dialog.show();
				} else {
					mUserName = selected;
					mGsensorData.setUserName(selected);
					mGsensorData.isSaved = false;
					mSurfaceCursor.setUserName(selected);
				}
			}
		});
		selectDialog.show();
	}

	/**
	 * 指定したUser名を含むファイル名でファイルに保存する
	 * @param sdcard SDカードのパス
	 * @param username User名
	 */
	private void file_save(final File sdcard, final String username) {
		final String filename = mGsensorData.getFileName() + "[" + username + "]";
		final File toFile = new File(sdcard, filename + FILE_EXTENSION);
		FileIOPostHandler handler = new FileIOPostHandler() {
			@Override
			public void onSuccess() {
				mFileNameList = setupFileNameList(sdcard);
				Toast.makeText(mThisActivity, filename + "に保存しました", Toast.LENGTH_LONG).show();
			}
			@Override
			public void onFailure(String errMessage) {
				String msg = filename + "の保存に失敗しました。\n" + errMessage;
				ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity, msg, null);
				dialog.show();
			}
		};
		FileIOAsyncTask saveTask = new FileIOAsyncTask(mThisActivity, handler) {
			@Override
			boolean backgroundTask() throws Exception {
				return mSensorRecordTask.saveToFile(toFile, filename);
			}
		};
		saveTask.execute();
	}

	/**
	 * ファイルの一覧のダイアログを表示し、選択されたファイルを読み込む
	 * @param sdcard SDカードのパス
	 */
	private void file_load(final File sdcard) {
		NameSelectDialog.SelectedListener selectedListener = new NameSelectDialog.SelectedListener() {
			@Override
			public void onSelected(final String selected) {
				final String filename = selected;
				final File fromFile = new File(sdcard, filename + FILE_EXTENSION);
				FileIOPostHandler handler = new FileIOPostHandler() {
					@Override
					public void onSuccess() {
						Matcher usernameMatcher = Pattern.compile("\\[.*\\]").matcher(selected);
						if (usernameMatcher.find()) {
							mUserName = usernameMatcher.group().replaceAll("[\\[\\]]", "");
						}
						mGsensorData = mSensorRecordTask.getGsensorData();
						mSurfaceCursor.setTimestamp(mGsensorData.getTimeStamp(0));
						mSurfaceGraph.enableDrawGraph(true);
						Toast.makeText(mThisActivity, selected + "から読込ました", Toast.LENGTH_LONG).show();
					}

					@Override
					public void onFailure(String errMessage) {
						String msg = selected + "の読込に失敗しました。\n" + errMessage;
						ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity, msg, null);
						dialog.show();
					}
				};
				FileIOAsyncTask loadTask = new FileIOAsyncTask(mThisActivity, handler) {
					@Override
					boolean backgroundTask() throws Exception {
						return mSensorRecordTask.loadFromFile(fromFile, filename);
					}
				};
				loadTask.execute();
			}
		};
		mFileNameList = setupFileNameList(sdcard);
		NameSelectDialog selectDialog = new NameSelectDialog(mThisActivity, "File Load", mFileNameList, selectedListener);
		selectDialog.show();
	}

	private void select_compare(final File sdcard) {
		NameSelectDialog.SelectedListener selectedListener = new NameSelectDialog.SelectedListener() {
			@Override
			public void onSelected(final String selected) {
				final String filename = selected;
				final File fromFile = new File(sdcard, filename + FILE_EXTENSION);
				FileIOPostHandler handler = new FileIOPostHandler() {
					@Override
					public void onSuccess() {
						mCompareGraph = showCompareGraph(true);
						Toast.makeText(mThisActivity, selected + "から読込ました", Toast.LENGTH_LONG).show();
					}

					@Override
					public void onFailure(String errMessage) {
						String msg = selected + "の読込に失敗しました。\n" + errMessage;
						ConfirmationDialog dialog = new ConfirmationDialog(mThisActivity, msg, null);
						dialog.show();
					}
				};
				FileIOAsyncTask loadTask = new FileIOAsyncTask(mThisActivity, handler) {
					@Override
					boolean backgroundTask() throws Exception {
						return mCompareSensorTask.loadFromFile(fromFile, filename);
					}
				};
				loadTask.execute();
			}
		};
		mFileNameList = setupFileNameList(sdcard);
		NameSelectDialog selectDialog = new NameSelectDialog(mThisActivity, "Select Compare data", mFileNameList, selectedListener);
		selectDialog.show();
	}

	private final int GRAPH_POSI = 0;
	/**
	 * 比較用データグラフ表示のON/OFF
	 * @param enable 表示許可
	 * @return 生成したグラフのオブジェクト（表示OFFの場合、比較用データが存在しない場合はnull）を返す
	 */
	private SurfaceGraph showCompareGraph(boolean enable) {
		SurfaceGraph sfg = null;
		if (mCompareSensorTask != null) {
			if (enable) {
				sfg = new SurfaceGraph(mThisActivity);
				SurfaceViewOnTouchListener compareListener = new SurfaceViewOnTouchListener(mThisActivity, sfg, mOnTouchAction);
				sfg.setOnTouchListener(compareListener);
				sfg.setGraphAlign(Align.CENTER);
				sfg.setGraphMode(mGraphMode);
				mCompareSensorTask.setSurfaceGraph(sfg);
				sfg.setPosition(mCompareSensorTask.getPosition());
				sfg.enableDrawGraph(true);
				mCompareSensorTask.execute();
				int height = mSurfaceGraph.getHeight();
				if (mCompareGraph != null) {
					mSurfaceLayout.removeViewAt(GRAPH_POSI);
				}
				mSurfaceLayout.addView(sfg, GRAPH_POSI, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
			} else {
				mCompareSensorTask.cancel();
				if (mCompareGraph != null) {
					mSurfaceLayout.removeViewAt(GRAPH_POSI);
					mSurfaceLayout.addView(mCompareGraph, GRAPH_POSI, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
				}
				sfg = null;
			}
		}
		return sfg;
	}
}
