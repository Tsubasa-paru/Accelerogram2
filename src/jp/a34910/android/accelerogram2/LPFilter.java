package jp.a34910.android.accelerogram2;

import android.graphics.PointF;

public class LPFilter {
	private long period;
	private float fs;
	private float f0 = 1.0f;
	private float a1;
	private float a2;
	private float b0;
	private float b1;
	private float b2;
	private PointF d1;
	private PointF d2;

	/**
	 * コンストラクタ
	 * サンプリング周期（msec）を指定する
	 * @param period サンプリング周期（msec）
	 */
	public LPFilter(long period) {
		this.period = period;
		this.setupLPF();
	}

	/**
	 * ローパスフィルタのカットオフ周波数を設定
	 * 設定したカットオフ周波数でフィルタ定数を再計算する
	 * @param f0 カットオフ周波数
	 * @return 設定したカットオフ周波数
	 */
	public float setFcutoff(float f0) {
		if ((f0 >= 1) && (f0 <= 5)) {
			this.f0 = f0;
			this.setupLPF();
		}
		return this.f0;
	}

	/**
	 * フィルタ定数を計算する
	 */
	private void setupLPF() {
		fs = 1000.0f / period;
		double omega = Math.tan((Math.PI * f0) / fs);
		double A = Math.sin(omega) / 0.707d;
		double cos_omega = Math.cos(omega);
		b0 = (float)((1 - cos_omega) / (2 * (1 + A)));
		b1 = (float)((1 - cos_omega) / (1 + A));
		b2 = (float)((1 - cos_omega) / (2 * (1 + A)));
		a1 = (float)((2 * cos_omega) / (1 + A));
		a2 = (float)((A - 1) / (A + 1));

		d1 = new PointF(0.0f, 0.0f);
		d2 = new PointF(0.0f, 0.0f);
	}

	/**
	 * ローパスフィルタの計算
	 * @param value サンプリングデータ
	 * @return フィルタ後のデータ
	 */
	public float lpFilter(float value) {
		float tmp;
		float filtered;
		tmp = value + a1 * d1.x + a2 * d2.x;
		filtered = b0 * tmp + b1 * d1.x + b2 * d2.x;
		d2.x = d1.x;
		d1.x = tmp;
		return filtered;
	}

	/**
	 * ローパスフィルタの計算
	 * @param value サンプリングデータ
	 * @return フィルタ後のデータ
	 */
	public PointF lpFilter(PointF value) {
		PointF tmp = new PointF();
		tmp.x = value.x + a1 * d1.x + a2 * d2.x;
		tmp.y = value.y + a1 * d1.y + a2 * d2.y;
		PointF filtered = new PointF();
		filtered.x = b0 * tmp.x + b1 * d1.x + b2 * d2.x;
		filtered.y = b0 * tmp.y + b1 * d1.y + b2 * d2.y;
		d2 = d1;
		d1 = tmp;
		return filtered;
	}

}
