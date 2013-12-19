package com.fuyue.danmei.read;


/**
 * 阅读配置
 * 
 * @author Calvin
 * 
 */
public class BookConstants {

	/** 字体大小 大号 */
	public static final int TEXT_SIZE_LARGE = 20;
	/** 字体大小 普通 */
	public static final int TEXT_SIZE_NORMAL = 16;
	/** 字体大小 小号 */
	public static final int TEXT_SIZE_SMALL = 12;

	/** 阅读模式 白天 夜间 */
	public enum ReadStyle {
		LIGHT, DARK
	}

	/** 屏幕选转样式 横屏 竖屏 */
	public enum OrientationStyle {
		LANDSCAPE, PORTRAIT
	}

	/** 白天模式字色 */
	public static final String TEXT_COLOR_LIGHT = "#444444";
	/** 白天模式信息字色 */
	public static final String TEXT_COLOR_LIGHT_INFO = "#888888";
	/** 白天模式背景 */
	public static final String BACKGROUND_LIGHT = "#ebe9e0";
	/** 黑夜模式字色 */
	public static final String TEXT_COLOR_DARK = "#828282";
	/** 白天模式信息字色 */
	public static final String TEXT_COLOR_DARK_INFO = "#777777";
	/** 黑夜模式背景 */
	public static final String BACKGROUND_DARK = "#2a2829";

	/** 字体大小 value array */
	public static final int[] TEXT_SIZE_VALUE = new int[] { TEXT_SIZE_LARGE,
			TEXT_SIZE_NORMAL, TEXT_SIZE_SMALL };
	/** 字体大小 key array */
	public static final String[] TEXT_SIZE_KEY = new String[] { "大号", "普通",
			"小号" };

	/** 阅读模式 value array */
	public static final ReadStyle[] READ_STYLE_VALUE = new ReadStyle[] {
			ReadStyle.LIGHT, ReadStyle.DARK };
	/** 阅读模式 key array */
	public static final String[] READ_STYLE_KEY = new String[] { "白天模式", "夜间模式" };

	/** 屏幕旋转样式 value array */
	public static final OrientationStyle[] ORIENTATION_STYLE_VALUE = new OrientationStyle[] {
			OrientationStyle.LANDSCAPE, OrientationStyle.PORTRAIT };
	/** 屏幕旋转样式 key array */
	public static final String[] ORIENTATION_STYLE_KEY = new String[] { "横屏阅读",
			"竖屏阅读" };
}
