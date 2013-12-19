package com.fuyue.danmei.config;

import android.content.Context;

import com.fuyue.danmei.read.BookConstants;
import com.fuyue.danmei.read.BookConstants.OrientationStyle;
import com.fuyue.danmei.read.BookConstants.ReadStyle;
import com.fuyue.util.ConfigPreference;

/**
 * 阅读配置参数
 * 
 * @author Calvin
 * 
 */
public class ReadSettingConfig extends ConfigPreference {

	/** 文件名 */
	private final static String SP_NAME = ".preference.read";
	/** 写入类型 */
	private final static int MODE = Context.MODE_PRIVATE;

	/** key 字体大小 */
	private final static String KEY_FONT_SIZE = "fontsize";
	/** key 阅读模式 */
	private final static String KEY_READSTYLE = "readstyle";
	/** key 屏幕方向 */
	private final static String KEY_ORIENTATION = "orientation";

	/** 静态实例 */
	private static ReadSettingConfig sReadSettingConfig;

	/**
	 * 获取静态实例
	 * 
	 * @param context
	 * @return
	 */
	public static ReadSettingConfig getInstance(Context context) {
		if (sReadSettingConfig == null) {
			sReadSettingConfig = new ReadSettingConfig(context);
		}
		return sReadSettingConfig;
	}

	/**
	 * 构造，及对首次调用的配置参数初始化
	 * 
	 * @param context
	 */
	private ReadSettingConfig(Context context) {
		super(context, SP_NAME, MODE);
	}

	/**
	 * 获取字体大小
	 * 
	 * @return
	 */
	public int getFontSize() {
		int fontsize = getInt(KEY_FONT_SIZE);
		return fontsize != 0 ? fontsize : BookConstants.TEXT_SIZE_NORMAL;
	}

	/**
	 * 获取阅读模式
	 * 
	 * @return
	 */
	public ReadStyle getReadStyle() {
		int readstyle = getInt(KEY_READSTYLE);
		ReadStyle style = ReadStyle.LIGHT;
		if (readstyle == 1) {
			style = ReadStyle.LIGHT;
		} else if (readstyle == 2) {
			style = ReadStyle.DARK;
		}
		return style;
	}

	/**
	 * 获取阅读模式
	 * 
	 * @return
	 */
	public OrientationStyle getOrientationStyle() {
		int orientationStyle = getInt(KEY_ORIENTATION);
		OrientationStyle style = OrientationStyle.PORTRAIT;
		if (orientationStyle == 1) {
			style = OrientationStyle.PORTRAIT;
		} else if (orientationStyle == 2) {
			style = OrientationStyle.LANDSCAPE;
		}
		return style;
	}

	/**
	 * 设置字体大小
	 * 
	 * @param fontSize
	 */
	public void setFontSize(int fontSize) {
		putInt(KEY_FONT_SIZE, fontSize);
	}

	/**
	 * 设置阅读模式
	 * 
	 * @param style
	 */
	public void setReadStyle(ReadStyle style) {
		if (style == ReadStyle.LIGHT) {
			putInt(KEY_READSTYLE, 1);
		} else if (style == ReadStyle.DARK) {
			putInt(KEY_READSTYLE, 2);
		}
	}

	/**
	 * 设置屏幕旋转模式
	 * 
	 * @param style
	 */
	public void setOrientationStyle(OrientationStyle style) {
		if (style == OrientationStyle.PORTRAIT) {
			putInt(KEY_ORIENTATION, 1);
		} else if (style == OrientationStyle.LANDSCAPE) {
			putInt(KEY_ORIENTATION, 2);
		}
	}
}
