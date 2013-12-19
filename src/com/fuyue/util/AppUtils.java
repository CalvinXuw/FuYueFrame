package com.fuyue.util;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.fuyue.BaseApplicaion;
import com.fuyue.util.logging.Log;

/**
 * 公共方法的集合
 */

public final class AppUtils {
	/** log tag. */
	private static final String TAG = AppUtils.class.getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/**
	 * 构造函数
	 */
	private AppUtils() {

	}

	/**
	 * 通过解析APk文件包，获取AndroidManifest.xml，来判断是否是正常的APK文件。如果找到则认为是正常的，否则认为是错误的。
	 * 
	 * @param filename
	 *            文件名字
	 * @return true表示正常,false 表示不正常。
	 */
	public static boolean isAPK(String filename) {
		boolean relt = false;

		if (TextUtils.isEmpty(filename) || !(new File(filename).exists())) {
			if (DEBUG) {
				Log.e(TAG, "apk文件找不到");
			}
			return false;
		}

		try {
			// 使用ZipFile判断下载的包里是否包含Manifest文件
			ZipFile zipfile = new ZipFile(filename);
			if (zipfile.getEntry("AndroidManifest.xml") != null) {
				relt = true;
			}

			zipfile.close();
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, "解析APK出错:" + e.getMessage());
			}
			relt = false;
		}

		return relt;
	}

	/**
	 * 支持自动安装的安装方法，为方便修改将其从原方法中抽出，增加AppItem参数。 预计在所有修改完成后，替换原来的installApk方法。
	 * 
	 * @param ctx
	 *            context
	 * @param filepath
	 *            filepath
	 * @param item
	 *            AppItem
	 */
	public static void installApk(Context ctx, String filepath) {
		if (filepath == null) {
			return;
		}

		try {
			Uri uri = Uri.fromFile(new File(filepath));
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(uri,
					"application/vnd.android.package-archive");
			ctx.startActivity(intent);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}
	}
}
