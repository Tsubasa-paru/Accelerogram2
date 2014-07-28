package jp.a34910.android.accelerogram2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParser;

import android.graphics.PointF;
import android.location.Location;
import android.util.Xml;

public class GsensorData {
//	static private final String TAG = MainActivity.APP_NAME + "GsensorData";
	static private final long PERIOD = 50;
	private final int MINUTE10 = 60 * 10;

	private ArrayList<PointF> mGsensorList;
	private ArrayList<Location> mLocationList;
	private PointF mCalibration;
	private int mPosition;
	private int mDefaultSize;

	private int mMonth;
	private int mDate;
	private int mHour;
	private int mMinute;
	private int mSecond;
	private int mMilliSecond;

	private String mUserName = "";
	private String mComment = "";
	public boolean isSaved = false;

	/**
	 * コンストラクタ
	 */
	public GsensorData() {
		this.mMilliSecond =  (int)PERIOD;
		this.mDefaultSize = MINUTE10 * (1000 / this.mMilliSecond);
		this.setupGsensorData();
	}

	public GsensorData(long period) {
		this.mMilliSecond = (int) period;
		this.mDefaultSize = MINUTE10 * (1000 / this.mMilliSecond);
		this.setupGsensorData();
	}

	public GsensorData(int minute, long period) {
		this.mMilliSecond = (int) period;
		this.mDefaultSize = minute * (1000 / this.mMilliSecond);
		this.setupGsensorData();
	}

	/**
	 * G-sensorオブジェクトの初期化
	 */
	public void setupGsensorData() {
		this.mGsensorList = new ArrayList<PointF>(this.mDefaultSize);
		this.mLocationList = new ArrayList<Location>(this.mDefaultSize);
		this.mCalibration = new PointF(0, 0);
		this.mPosition = 0;
		this.isSaved = false;
		this.getCalendar();
	}

	/**
	 * GsensorData内のユーザ名を返す
	 * @return ユーザ名
	 */
	public String getUserName() {
		return mUserName;
	}

	/**
	 * GsensorDataにユーザ名を設定する
	 * @param username ユーザ名
	 */
	public void setUserName(String username) {
		if (this.mUserName.equals(username) == false) {
			this.mUserName = username;
			this.isSaved = false;
		}
	}

	/**
	 * GsensorData内のコメントを返す
	 * @return コメント文字列
	 */
	public String getComment() {
		return mComment;
	}

	/**
	 * GsensorDataにコメントを設定する
	 * @param comment コメント
	 */
	public void setComment(String comment) {
		if (this.mComment.equals(comment) == false) {
			this.mComment = comment;
			this.isSaved = false;
		}
	}

	/**
	 * G-sensor/Locationを追加する
	 * @param gsensor G-sensor値
	 * @param location GPS位置情報
	 * @return 追加後のデータのサイズ
	 */
	public int add(PointF gsensor, Location location) {
		mGsensorList.add(gsensor);
		mLocationList.add(location);
		return this.mPosition++;
	}

	/**
	 * 現在保持しているデータのサイズを返す
	 * @return 現在のデータサイズ
	 */
	public int getSize() {
		return this.mPosition;
	}

	/**
	 * 指定位置のG-sensor値を返す
	 * @param position 指定位置
	 * @return G-sensor値
	 */
	public PointF getGsensor(int position) {
		if (this.mPosition == 0) {
			return new PointF(0, 0);
		}
		if (position >= this.mPosition) {
			position = this.mPosition - 1;
		}
		PointF gsensor = mGsensorList.get(position);
		return new PointF((gsensor.x - mCalibration.x), (gsensor.y - mCalibration.y));
	}

	/**
	 * 指定位置のLocationを返す
	 * @param position 指定位置
	 * @return Location
	 */
	public Location getLocation(int position) {
		if (this.mPosition == 0) {
			return null;
		}
		if (position >= this.mPosition) {
			position = this.mPosition - 1;
		}
		return mLocationList.get(position);
	}

	/**
	 * G-sensorのキャリブレーション値を設定する
	 * @param calib キャリブレーション値
	 * @return 設定したキャリブレーション値
	 */
	public PointF setCalibration(PointF calib) {
		if (calib == null) {
			this.mCalibration = new PointF(0, 0);
		} else {
			this.mCalibration = new PointF(calib.x, calib.y);
		}
		return this.mCalibration;
	}

	/**
	 * 指定したG-sensor値にキャリブレーション補正を行う
	 * @param gsensor G-sensor値
	 * @return 補正後のG-sensor値
	 */
	public PointF addCalibration(PointF gsensor) {
		if (gsensor == null) {
			gsensor = new PointF(0, 0);
		}
		return new PointF((gsensor.x - mCalibration.x), (gsensor.y - mCalibration.y));
	}

	/**
	 * G-sensor初期化時の時間情報からファイル名を生成する
	 * @return 生成したファイル名
	 */
	public String getFileName() {
		String filename;
		if (mPosition > 0) {
			long rec_time =  (mPosition / (1000 / mMilliSecond)) / 60;
			String date =  String.format(Locale.JAPAN, "%02d-%02d-%02d:%02d:%02d", mMonth, mDate, mHour, mMinute, mSecond);
			filename = date + "(" + rec_time + "min)";
		} else {
			filename = "";
		}
		return filename;
	}

	/**
	 * カレンダーを取得する
	 */
	private void getCalendar() {
		Calendar cal = Calendar.getInstance();
		this.mMonth = cal.get(Calendar.MONTH) + 1;
		this.mDate = cal.get(Calendar.DATE);
		this.mHour = cal.get(Calendar.HOUR_OF_DAY);
		this.mMinute = cal.get(Calendar.MINUTE);
		this.mSecond = cal.get(Calendar.SECOND);
	}

	/**
	 * 指定位置のタイムスタンプを生成する
	 * @param position 指定位置
	 * @return 生成したタイムスタンプ
	 */
	public String getTimeStamp(int position) {
		long unit = 1000 / this.mMilliSecond;
		long msec = (position % unit) * (100 / unit);
		long second = this.mSecond + (position / unit);
		long minute = this.mMinute + (second / 60);
		long hour = this.mHour + (minute / 60);
		if (second >= 60) {
			second = second % 60;
		}
		if (minute >= 60) {
			minute = minute % 60;
		}
		if (hour >= 24) {
			hour = hour - 24;
		}
		return String.format(Locale.JAPAN, "%02d:%02d:%02d.%02d", hour, minute, second, msec);
	}

	static private final String USER_TAG = "user";
	static private final String COMMENT_TAG = "comment";
	static private final String MONTH_TAG = "month";
	static private final String DATE_TAG = "date";
	static private final String HOUR_TAG = "hour";
	static private final String MINUTE_TAG = "minute";
	static private final String SECOND_TAG = "second";
	static private final String MILLISECOND_TAG = "msecond";
	static private final String CARIBRATION_TAG = "caribration";
	static private final String X_TAG = "x";
	static private final String Y_TAG = "y";
	static private final String GSENSOR_TAG = "gsensor";
	static private final String GSENSOR_X_TAG = "gsensor_x";
	static private final String GSENSOR_Y_TAG = "gsensor_y";
	static private final String PROVIDER_TAG = "provider";
	static private final String LATITUDE_TAG = "latitude";
	static private final String LONGITUDE_TAG = "longitude";

	/**
	 * XMLをパースしてG-sensor値、GPS情報等を読み込む
	 * @param is XMLを流し込むInputStream
	 * @return true:読込に成功
	 * @throws Exception XmlPullParseException
	 */
	public boolean loadFromXML(InputStream is) throws Exception {
		XmlPullParser xmlparser = Xml.newPullParser();

		this.mPosition = 0;
		xmlparser.setInput(is, "UTF-8");
		String parameter = "";
		String value = "";
		String third_tag = "";
		String gsensor_x = "";
		String gsensor_y = "";
		String carib_x = "";
		String carib_y = "";
		String provider = "";
		String latitude = "";
		String longitude = "";
		for (int eventType = xmlparser.getEventType();
				eventType != XmlPullParser.END_DOCUMENT;
				eventType = xmlparser.next()) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				switch (xmlparser.getDepth()) {
				case 1: // First layer : ApplicationName
					// Do nothing
					break;
				case 2: // Second layer : ParameterSet Name
					parameter = xmlparser.getName();
					break;
				case 3: // Third layer : X_TAG or Y_TAG
					third_tag = xmlparser.getName();
					break;
				default:
					break;
				}
				break;
			case XmlPullParser.TEXT:
				switch (xmlparser.getDepth()) {
				case 1:
					// Do nothing
					break;
				case 2:
					value = xmlparser.getText();
					if (value.trim().length() > 0) {
						if (parameter.equals(USER_TAG)) {
							this.mUserName = new String(value);
						} else if (parameter.equals(COMMENT_TAG)) {
							this.mComment = new String(value);
						} else if (parameter.equals(MONTH_TAG)) {
							this.mMonth = Integer.valueOf(value);
						} else if (parameter.equals(DATE_TAG)) {
							this.mDate = Integer.valueOf(value);
						} else if (parameter.equals(HOUR_TAG)) {
							this.mHour = Integer.valueOf(value);
						} else if (parameter.equals(MINUTE_TAG)) {
							this.mMinute = Integer.valueOf(value);
						} else if (parameter.equals(SECOND_TAG)) {
							this.mSecond = Integer.valueOf(value);
						} else if (parameter.equals(MILLISECOND_TAG)) {
							this.mMilliSecond = Integer.valueOf(value);
						}
					}
					break;
				case 3:
					value = xmlparser.getText();
					if (value.trim().length() > 0) {
						if (parameter.equals(CARIBRATION_TAG)) {
							if (third_tag.equals(X_TAG)) {
								carib_x = value;
							} else {
								carib_y = value;
							}
						} else if (parameter.equals(GSENSOR_TAG)) {
							if (third_tag.equals(GSENSOR_X_TAG)) {
								gsensor_x = value;
							} else if (third_tag.equals(GSENSOR_Y_TAG)){
								gsensor_y = value;
							} else if (third_tag.equals(PROVIDER_TAG)) {
								provider = value;
							} else if (third_tag.equals(LATITUDE_TAG)) {
								latitude = value;
							} else if (third_tag.equals(LONGITUDE_TAG)) {
								longitude = value;
							}
						}
					}
					break;
				default:
					break;
				}
				break;
			case XmlPullParser.END_TAG:
				switch (xmlparser.getDepth()) {
				case 1:
					//Do nothing
					break;
				case 2:
					if (parameter.equals(CARIBRATION_TAG)) {
						if ((carib_x.length() > 0) && (carib_y.length() > 0)) {
							this.mCalibration.x = Float.valueOf(carib_x);
							this.mCalibration.y = Float.valueOf(carib_y);
						}
						carib_x = "";
						carib_y = "";
					} else if (parameter.equals(GSENSOR_TAG)) {
						if ((gsensor_x.length() > 0) && (gsensor_y.length() > 0) &&
							(provider.length() > 0) && (latitude.length() > 0) && (longitude.length() > 0)) {
							PointF gsensor = new PointF(Float.valueOf(gsensor_x), Float.valueOf(gsensor_y));
							Location location = new Location(provider);
							location.setLatitude(Double.valueOf(latitude));
							location.setLongitude(Double.valueOf(longitude));
							this.add(gsensor, location);
						}
						gsensor_x = "";
						gsensor_y = "";
						provider = "";
						latitude = "";
						longitude = "";
					}
					parameter = "";
					value = "";
					break;
				case 3:
					third_tag = "";
					value = "";
					break;
				default:
					break;
				}
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * G-sensor値、GPS情報等からXMLを生成する
	 * @param os 生成したXMLを流し込むOutputStream
	 * @return true:XML生成に成功
	 * @throws Exception ParserConfigurationException,DOMException,TransformerConfigurationException,IllegalArgumentException
	 */
	public boolean saveToXML(OutputStream os) throws Exception {
		Element gsensorTag = null;
		Element gsensorXTag = null;
		Element gsensorYTag = null;
		Element providerTag = null;
		Element latitudeTag = null;
		Element longitudeTag = null;
		Text text = null;

		DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = dbuilder.newDocument();
		text = document.createTextNode("\n");
		document.appendChild(text);
		Element Accelerogram = document.createElement("accelerogram"); //document root

		Element userTag = document.createElement(USER_TAG);
		userTag.appendChild(document.createTextNode(mUserName));
		Accelerogram.appendChild(userTag);

		Element commentTag = document.createElement(COMMENT_TAG);
		commentTag.appendChild(document.createTextNode(mComment));
		Accelerogram.appendChild(commentTag);

		Element monthTag = document.createElement(MONTH_TAG);
		text = document.createTextNode(String.valueOf(this.mMonth));
		monthTag.appendChild(text);
		Accelerogram.appendChild(monthTag);

		Element dateTag = document.createElement(DATE_TAG);
		text = document.createTextNode(String.valueOf(this.mDate));
		dateTag.appendChild(text);
		Accelerogram.appendChild(dateTag);

		Element hourTag = document.createElement(HOUR_TAG);
		text = document.createTextNode(String.valueOf(this.mHour));
		hourTag.appendChild(text);
		Accelerogram.appendChild(hourTag);

		Element minuteTag = document.createElement(MINUTE_TAG);
		text = document.createTextNode(String.valueOf(this.mMinute));
		minuteTag.appendChild(text);
		Accelerogram.appendChild(minuteTag);

		Element secondTag = document.createElement(SECOND_TAG);
		text = document.createTextNode(String.valueOf(this.mSecond));
		secondTag.appendChild(text);
		Accelerogram.appendChild(secondTag);

		Element msecTag = document.createElement(MILLISECOND_TAG);
		text = document.createTextNode(String.valueOf(this.mMilliSecond));
		msecTag.appendChild(text);
		Accelerogram.appendChild(msecTag);

		Element caribTag = document.createElement(CARIBRATION_TAG);

		Element caribXTag = document.createElement(X_TAG);
		text = document.createTextNode(String.valueOf(this.mCalibration.x));
		caribXTag.appendChild(text);
		caribTag.appendChild(caribXTag);

		Element caribYTag = document.createElement(Y_TAG);
		text = document.createTextNode(String.valueOf(this.mCalibration.y));
		caribYTag.appendChild(text);
		caribTag.appendChild(caribYTag);

		Accelerogram.appendChild(caribTag);

		for (int index = 0; index < this.mPosition; index++) {
			PointF gsensor = this.mGsensorList.get(index);
			Location location = this.mLocationList.get(index);

			gsensorTag = document.createElement(GSENSOR_TAG);

			gsensorXTag = document.createElement(GSENSOR_X_TAG);
			text = document.createTextNode(String.valueOf(gsensor.x));
			gsensorXTag.appendChild(text);
			gsensorTag.appendChild(gsensorXTag);

			gsensorYTag = document.createElement(GSENSOR_Y_TAG);
			text = document.createTextNode(String.valueOf(gsensor.y));
			gsensorYTag.appendChild(text);
			gsensorTag.appendChild(gsensorYTag);

			providerTag = document.createElement(PROVIDER_TAG);
			text = document.createTextNode(location.getProvider());
			providerTag.appendChild(text);
			gsensorTag.appendChild(providerTag);

			latitudeTag = document.createElement(LATITUDE_TAG);
			text = document.createTextNode(String.valueOf(location.getLatitude()));
			latitudeTag.appendChild(text);
			gsensorTag.appendChild(latitudeTag);

			longitudeTag = document.createElement(LONGITUDE_TAG);
			text = document.createTextNode(String.valueOf(location.getLongitude()));
			longitudeTag.appendChild(text);
			gsensorTag.appendChild(longitudeTag);

			Accelerogram.appendChild(gsensorTag);
		}

		document.appendChild(Accelerogram);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(new DOMSource(document), new StreamResult(os));

		return true;
	}
}
