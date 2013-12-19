package com.fuyue.util.net.requestor;

import android.text.TextUtils;

import com.fuyue.util.logging.Log;

/**
 * 从缓存获取数据的网络请求类
 * 
 * @author xuwei
 * 
 */
public class CacheRequestTask extends RequestTask {

	/** log tag. */
	private static final String TAG = CacheRequestTask.class.getSimpleName();

	/** cache 工具 */
	private RequestDataCache mDataCache;

	/**
	 * 构造函数
	 * 
	 * @param dataCache
	 *            dataCache
	 * @param listener
	 *            回调Listener
	 */
	public CacheRequestTask(RequestDataCache dataCache, OnRequestTaskListener listener) {
		super(listener);
		mDataCache = dataCache;
	}

	/**
	 * @throws Exception
	 */
	@Override
	public final void run() {
		super.run();
		if (DEBUG) {
			Log.d(TAG,
					"---- start cache request time:"
							+ System.currentTimeMillis());
		}

		if (mOnRequestTaskListener==null) {
			if (DEBUG) {
				Log.e(TAG, "cache request listener is null");
			}
			return;
		}

		if (mDataCache == null) {
			mOnRequestTaskListener.onFailed(IRequestModelErrorCode.ERROR_CODE_NO_URL);
			return;
		}

		String ret = mDataCache.load();

		if (TextUtils.isEmpty(ret)) {
			mOnRequestTaskListener
					.onFailed(IRequestModelErrorCode.ERROR_CODE_NET_FAILED);
		} else {
			mOnRequestTaskListener.onSuccess(ret);
		}

	}
}
