package com.fuyue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import com.fuyue.frame.BuildConfig;
import com.fuyue.util.CrashHandler;
import com.fuyue.util.download.DownloadServiceCallback;
import com.fuyue.util.logging.Configuration;
import com.fuyue.util.logging.Log;

/**
 * 基类application，实现了全局的配置信息
 * 
 * @author Calvin
 * 
 */
public class BaseApplicaion extends Application implements
		DownloadServiceCallback {

	/** 配置文件位置，需要在src目录下创建配置文件 */
	private static final String PROPERTY_FILENAME = "/android.fuyue.cfg";
	/** 包名 */
	private static final String KEY_PROPERTY_PACKAGENAME = "packagename";
	/** 应用名 */
	private static final String KEY_PROPERTY_APPNAME = "appname";
	/** 产品名 */
	private static final String KEY_PROPERTY_PRODUCTNAME = "productname";
	/** 版权声明 */
	private static final String KEY_PROPERTY_COPYRIGHT = "copyright";

	static {
		InputStream is = Configuration.class
				.getResourceAsStream(PROPERTY_FILENAME);
		Properties prop = new Properties();
		try {
			prop.load(is);
			sPackageName = prop.getProperty(KEY_PROPERTY_PACKAGENAME,
					"com.ifeng.android");

			sAppName = prop.getProperty(KEY_PROPERTY_APPNAME, "腐阅");
			sProductName = prop.getProperty(KEY_PROPERTY_PRODUCTNAME, "腐阅");
			sCopyright = prop.getProperty(KEY_PROPERTY_COPYRIGHT, "腐阅");
		} catch (IOException e) {
			Log.e(Configuration.class.getName(),
					"check the ifeng frame config file again !");
			Log.e(Configuration.class.getName(), e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(Configuration.class.getName(), e.getMessage());
				}
			}
		}
	}

	/** tag */
	public final String TAG = getClass().getSimpleName();
	/** 是否开启调试模式 */
	public static final boolean DEBUG = BuildConfig.DEBUG;
	/** 是否将全部日志信息输出到文件 或者 从配置文件中读取输出条件信息 */
	public static final boolean LOG_TO_FILE_ALLMESSAGE = true;

	/** 是否将log日志输出到日志文件中 */
	public static boolean sLogToFile = true;
	/** 包名 */
	public static String sPackageName;
	/** 应用名 */
	public static String sAppName;
	/** 产品名 */
	public static String sProductName;
	/** 版权声明 */
	public static String sCopyright;

	@Override
	public void onDownloadServiceCreate() {

	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			sAppName = new String(sAppName.getBytes("ISO-8859-1"), "utf-8");
			sProductName = new String(sProductName.getBytes("ISO-8859-1"),
					"utf-8");
			sCopyright = new String(sCopyright.getBytes("ISO-8859-1"), "utf-8");
		} catch (Exception e) {
		}

		// 初始化异常崩溃类
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
	}

	/***
	 * 退出应用程序
	 * 
	 * @param context
	 */
	public void AppExit() {
		try {
			ActivityManager activityMgr = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			activityMgr.restartPackage(getPackageName());
			System.exit(0);
		} catch (Exception e) {
		}
	}
}
