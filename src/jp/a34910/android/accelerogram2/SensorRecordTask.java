package jp.a34910.android.accelerogram2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class SensorRecordTask extends PeriodicTask implements SensorEventListener,LocationListener {
	static private final String TAG = MainActivity.APP_NAME + "SensorRecordTask";
	static private final long PERIOD = 50;
	private long mPeriod = PERIOD;
	public enum Status {
		IDLE,
		REC,
		REPLAY,
		PAUSE,
		CALIBRATION,
	};
	private Status mStatus = Status.IDLE;
	private PointF mGsensor;
	private PointF mCalibration = new PointF(0, 0);
	private Location mLocation;
	private int mPosition;
	private GsensorData mGsensorData;
	private SensorRecordTask.SensorListener mSensorListener;
	private LPFilter mLPFilter;
	private Location mReplayLocation;
	private long mCount1sec = 0;
	private long mCount1secMax;

	public SensorRecordTask(SensorListener listener) {
		super(PERIOD);
		initSensorRecordTask(listener);
	}

	public SensorRecordTask(long period, SensorListener listener) {
		super(period);
		mPeriod = period;
		initSensorRecordTask(listener);
	}

	/**
	 * Gsensor計測を初期化する
	 * @param listener Gsensorの計測結果を通知するListener
	 */
	private void initSensorRecordTask(SensorListener listener) {
		this.mGsensorData = new GsensorData(mPeriod);
		this.mGsensor = new PointF(0, 0);
		this.mSensorListener = listener;
		this.mLPFilter = new LPFilter(mPeriod);
		this.mLPFilter.setFcutoff(1.0f);
		this.mCount1secMax = 1000 / mPeriod;
	}

	/**
	 * 記録・再生に利用しているGsensorDataを返す
	 * @return GsensorDataオブジェクト
	 */
	public GsensorData getGsensorData() {
		return this.mGsensorData;
	}

	/**
	 * 再生する位置を設定する
	 * @param position 再生位置
	 * @return 設定した再生位置
	 */
	public int setPosition(int position) {
		int maxPosition = mGsensorData.getSize();
		if (position < 0) position = 0;
		if (position > maxPosition) position = maxPosition;
		mPosition = position;
		return mPosition;
	}

	/**
	 * 現在保持しているGsensorDataをファイルに保存する
	 * @param toFile 保存するファイル
	 * @param filename ZIPアーカイブ内に設定するファイル名
	 * @return 保存に成功するとtrue
	 * @throws Exception
	 */
	public boolean saveToFile(final File toFile, final String filename) throws Exception {
		this.cancel();
		try {
			BufferedOutputStream bufferdos = new BufferedOutputStream(new FileOutputStream(toFile));
			ZipOutputStream zipos = new ZipOutputStream(bufferdos);
			zipos.putNextEntry(new ZipEntry(filename + ".xml"));
			mGsensorData.saveToXML(zipos);
			mGsensorData.isSaved = true;
			bufferdos.flush();
			zipos.closeEntry();
			zipos.flush();
			zipos.close();
			bufferdos.close();
		} catch (Exception e) {
			throw e;
		} finally {
			this.execute();
		}
		return true;
	}

	/**
	 * ファイルからGsensorDataを読み込む
	 * @param fromFile 読み込むファイル
	 * @param filename ZIPアーカイブ内のファイル名
	 * @return 読み込みに成功するとtrue
	 * @throws Exception
	 */
	public boolean loadFromFile(final File fromFile, final String filename) throws Exception{
		this.cancel();
		try {
			BufferedInputStream bufferedis = new BufferedInputStream(new FileInputStream(fromFile));
			ZipInputStream zipis = new ZipInputStream(bufferedis);
			ZipEntry entry = zipis.getNextEntry();
			if (entry == null || entry.getName().equals(filename + ".xml") == false) {
				zipis.close();
				bufferedis.close();
				Exception e = new Exception(filename + "内にデータがありません");
				throw e;
			}
			GsensorData newGsensorData = new GsensorData(mPeriod);
			newGsensorData.loadFromXML(zipis);
			mGsensorData = newGsensorData;
			mGsensorData.isSaved = true;
			zipis.close();
			bufferedis.close();
		} catch (Exception e) {
			throw e;
		} finally {
			this.execute();
		}
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] accelero;
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelero = event.values.clone();
			synchronized (mGsensor) {
				mGsensor.x = (accelero[0] / 9.8f) * 10;
				mGsensor.y = (accelero[1] / 9.8f) * 10;
			}
			break;
		default:
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onLocationChanged(Location location) {
		mLocation = location;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO 自動生成されたメソッド・スタブ

	}

	/**
	 * Gsensor計測用周期タスクのモードを設定する
	 * @param newStatus 設定するモード
	 * IDLE:計測のみ
	 * REC:計測したセンサ値を記録する
	 * REPLAY:記録したセンサ値を再生する
	 * PAUSE:センサ値の再生を一時停止する
	 * @return 設定したモード
	 */
	public Status setStatus(Status newStatus) {
		switch (newStatus) {
		case REC:
			if (mStatus == Status.IDLE) {
				mGsensorData.setupGsensorData();
				mGsensorData.setCalibration(mCalibration);
				mStatus = Status.REC;
			}
			break;
		case REPLAY:
			if (mStatus == Status.IDLE) {
				mPosition = 0;
				mStatus = Status.REPLAY;
			} else if (mStatus == Status.PAUSE) {
				mStatus = Status.REPLAY;
			}
			break;
		case PAUSE:
			if (mStatus == Status.REPLAY) {
				mStatus = Status.PAUSE;
			} else if (mStatus == Status.PAUSE){
				mStatus = Status.REPLAY;
			}
			break;
		case IDLE:
			mStatus = Status.IDLE;
			mPosition = 0;
			break;
		case CALIBRATION:
			mStatus = Status.CALIBRATION;
			break;
		default:
			break;
		}
		Log.d(TAG, "mStatus is" + mStatus);
		return mStatus;
	}

	@Override
	protected void periodicTaskOnUIThread() {
		if (mCount1sec++ >= mCount1secMax) {
			mCount1sec = 0;
			switch (mStatus) {
			case REPLAY:
			case PAUSE:
				mSensorListener.onPeriodicLocation(mReplayLocation);
				break;
			case IDLE:
			case REC:
				mSensorListener.onPeriodicLocation(mLocation);
			default:
				break;
			}
		}
	}

	@Override
	protected void periodicTaskOnTimerThread() {
		PointF gsensor = this.mLPFilter.lpFilter(mGsensor);
		switch (mStatus) {
		case REPLAY:
			gsensor = mGsensorData.getGsensor(mPosition);
			mReplayLocation =mGsensorData.getLocation(mPosition);
			if (mPosition < mGsensorData.getSize()) {
				mPosition++;
			}
			break;
		case PAUSE:
			gsensor = mGsensorData.getGsensor(mPosition);
			mReplayLocation =mGsensorData.getLocation(mPosition);
			break;
		case REC:
			mPosition = mGsensorData.add(gsensor, mLocation);
			// fall through
		case IDLE:
			gsensor = mGsensorData.addCalibration(gsensor);
			break;
		case CALIBRATION:
			mCalibration = mGsensorData.setCalibration(gsensor);
			mStatus = Status.IDLE;
			break;
		default:
			//do nothing
			break;
		}
		mSensorListener.onPeriodicGsensor(gsensor, mPosition);
	}

	public interface SensorListener {
		/**
		 * 計測データを周期タスクから通知するInterface
		 * （非UIスレッドからコールバックする）
		 * @param currentGsensor 現在のセンサ値
		 * @param gsensorData センサ値を保持するGsensorDataオブジェクト
		 * @param position 現在位置
		 */
		public void onPeriodicGsensor(PointF currentGsensor, int position);
		/**
		 * 計測データを周期タスクから通知するInterface
		 * （UIスレッドからコールバックする）
		 * @param location 現在のGPS Location
		 */
		public void onPeriodicLocation(Location location);
	}
}
