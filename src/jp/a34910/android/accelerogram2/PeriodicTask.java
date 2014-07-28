package jp.a34910.android.accelerogram2;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;

public abstract class PeriodicTask {
	/**
	 * 周期的に処理を行うための抽象クラス。
	 * 周期カウントはこのクラスのインスタンスで作るTimerのDaemonスレッドで行われる。
	 * periodicTaskOnUIThread()処理はこのインスタンスを作成したスレッドで実行する。
	 * periodicTaskOnTimerThread()処理はTimerのDaemonスレッドで実行する。
	 */
	private long period;
	private boolean isDaemon = true;
	private Timer timer;
	private TimerTask timerTask;
	private Handler handler;

	/**
	 * periodミリ秒の周期で動かす
	 * @param period 周期指定（ミリ秒）
	 */
	public PeriodicTask(long period) {
		this.handler = new Handler();
		this.period = period;
		this.timer = null;
	}

	/**
	 * 周期タスクの実行を開始する
	 */
	public void execute(){
		if (timer == null) { //timer == null（周期タスクは存在しない）
			timerTask = new TimerTask() {
				@Override
				public void run() {
					periodicTaskOnTimerThread();
					handler.post(new Runnable() {
						@Override
						public void run() {
							periodicTaskOnUIThread();
						}
					});
				}
			};
			timer = new Timer(isDaemon);
			timer.scheduleAtFixedRate(timerTask, period, period);
		}
	}

	/**
	 * 周期タスクの実行をキャンセルする
	 */
	public void cancel() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * インスタンスを作成したスレッド(例えばUIスレッド）で処理させるTask
	 */
	abstract protected void periodicTaskOnUIThread();

	/**
	 * タイマースレッドで処理させるTask
	 * doPeriodicTaskの直前に呼ばれる。
	 */
	abstract protected void periodicTaskOnTimerThread();
}
