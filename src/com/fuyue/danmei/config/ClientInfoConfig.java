package com.fuyue.danmei.config;

import android.content.Context;

import com.fuyue.util.ConfigPreference;

/**
 * 客户端配置参数
 * 
 * @author Calvin
 * 
 */
public class ClientInfoConfig extends ConfigPreference {

	/** 文件名 */
	private final static String SP_NAME = ".preference.client";
	/** 写入类型 */
	private final static int MODE = Context.MODE_PRIVATE;

	/** key when the lasttime check client update */
	private final static String KEY_LASTTIME_CHECK_UPDATE = "lasttime_check_update";
	/** 书架展现形式 */
	private final static String KEY_BOOKSHELF_STYLE = "bookshelfstyle";

	/** time interval of check update */
	private final static int CHECK_UPDATE_INTERVAL = 24 * 60 * 60 * 1000;

	/** 静态实例 */
	private static ClientInfoConfig sClientInfoConfig;

	/**
	 * 获取静态实例
	 * 
	 * @param context
	 * @return
	 */
	public static ClientInfoConfig getInstance(Context context) {
		if (sClientInfoConfig == null) {
			sClientInfoConfig = new ClientInfoConfig(context);
		}
		return sClientInfoConfig;
	}

	/**
	 * 构造，及对首次调用的配置参数初始化
	 * 
	 * @param context
	 */
	private ClientInfoConfig(Context context) {
		super(context, SP_NAME, MODE);
	}

	/**
	 * 检查客户端更新
	 * 
	 * @return
	 */
	public boolean shouldCheckUpdate() {
		long currentTime = System.currentTimeMillis();
		long lastTime = getLong(KEY_LASTTIME_CHECK_UPDATE);

		putLong(KEY_LASTTIME_CHECK_UPDATE, currentTime);
		if (currentTime - lastTime > CHECK_UPDATE_INTERVAL) {
			return true;
		}
		return false;
	}

	/**
	 * 是否为列表展现形式
	 * 
	 * @return
	 */
	public boolean isListBookshelf() {
		return getBoolean(KEY_BOOKSHELF_STYLE);
	}

	/**
	 * 设置是否为列表展现形式
	 * 
	 * @param isList
	 */
	public void setIsListBookshelf(boolean isList) {
		putBoolean(KEY_BOOKSHELF_STYLE, isList);
	}
}
