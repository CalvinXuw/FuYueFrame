/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fuyue.util.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;

import com.fuyue.util.download.Downloads.Impl;
import com.fuyue.util.logging.Log;

/**
 * Stores information about an individual download.
 */
public final class DownloadInfo {
	/**
	 * read the download info from database.
	 */
	public static class Reader {
		/** ContentResolver. */
		private ContentResolver mResolver;
		/** Cursor. */
		private Cursor mCursor;
		/** mOldChars. */
		private CharArrayBuffer mOldChars;
		/** mNewChars. */
		private CharArrayBuffer mNewChars;

		/**
		 * constructor.
		 * 
		 * @param resolver
		 *            ContentResolver
		 * @param cursor
		 *            Cursor
		 */
		public Reader(ContentResolver resolver, Cursor cursor) {
			mResolver = resolver;
			mCursor = cursor;
		}

		/**
		 * generate a new DownloadInfo from the database, suppose the cusor has
		 * move the the correct positon.
		 * 
		 * @param context
		 *            context
		 * @param systemFacade
		 *            SystemFacade
		 * @return the new DownloadInfo
		 */
		public DownloadInfo newDownloadInfo(Context context,
				SystemFacade systemFacade) {
			DownloadInfo info = new DownloadInfo(context, systemFacade);
			updateFromDatabase(info);
			readRequestHeaders(info);
			return info;
		}

		/**
		 * update the DownloadInfo from cursor.
		 * 
		 * @param info
		 *            the info need to be updated
		 */
		public void updateFromDatabase(DownloadInfo info) {
			synchronized (info) { // add by caohaitao fix SEARHBOX-65
									// 【百度搜索】【Android一期】【下载】下载历史记录中，文件大小不显示

				info.mId = getLong(Downloads.Impl._ID);
				info.mUri = getString(info.mUri, Downloads.Impl.COLUMN_URI);
				info.mNoIntegrity = getInt(Downloads.Impl.COLUMN_NO_INTEGRITY) == 1;
				info.mHint = getString(info.mHint,
						Downloads.Impl.COLUMN_FILE_NAME_HINT);
				info.mFileName = getString(info.mFileName, Downloads.Impl.DATA);
				info.mMimeType = getString(info.mMimeType,
						Downloads.Impl.COLUMN_MIME_TYPE);
				info.mDestination = getInt(Downloads.Impl.COLUMN_DESTINATION);
				info.mVisibility = getInt(Downloads.Impl.COLUMN_VISIBILITY);
				info.mStatus = getInt(Downloads.Impl.COLUMN_STATUS);
				info.mNumFailed = getInt(Constants.FAILED_CONNECTIONS);
				info.mFailedReason = getString(info.mFailedReason,
						Constants.FAILED_REASON);
				int retryRedirect = getInt(Constants.RETRY_AFTER_X_REDIRECT_COUNT);
				info.mRetryAfter = retryRedirect & 0xfffffff; // SUPPRESS
																// CHECKSTYLE
				info.mRedirectCount = retryRedirect >> 28; // SUPPRESS
															// CHECKSTYLE
				info.mLastMod = getLong(Downloads.Impl.COLUMN_LAST_MODIFICATION);
				info.mPackage = getString(info.mPackage,
						Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE);
				info.mClass = getString(info.mClass,
						Downloads.Impl.COLUMN_NOTIFICATION_CLASS);
				info.mExtras = getString(info.mExtras,
						Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS);
				info.mCookies = getString(info.mCookies,
						Downloads.Impl.COLUMN_COOKIE_DATA);
				info.mUserAgent = getString(info.mUserAgent,
						Downloads.Impl.COLUMN_USER_AGENT);
				info.mReferer = getString(info.mReferer,
						Downloads.Impl.COLUMN_REFERER);
				info.mTotalBytes = getLong(Downloads.Impl.COLUMN_TOTAL_BYTES);
				info.mCurrentBytes = getLong(Downloads.Impl.COLUMN_CURRENT_BYTES);
				info.mETag = getString(info.mETag, Constants.ETAG);
				info.mMediaScanned = getInt(Constants.MEDIA_SCANNED) == 1;
				info.mDeleted = getInt(Downloads.Impl.COLUMN_DELETED) == 1;
				info.mMediaProviderUri = getString(info.mMediaProviderUri,
						Downloads.Impl.COLUMN_MEDIAPROVIDER_URI);
				info.mIsPublicApi = getInt(Downloads.Impl.COLUMN_IS_PUBLIC_API) != 0;
				info.mAllowedNetworkTypes = getInt(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES);
				info.mAllowRoaming = getInt(Downloads.Impl.COLUMN_ALLOW_ROAMING) != 0;
				info.mTitle = getString(info.mTitle,
						Downloads.Impl.COLUMN_TITLE);
				info.mDescription = getString(info.mDescription,
						Downloads.Impl.COLUMN_DESCRIPTION);
				info.mBypassRecommendedSizeLimit = getInt(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT);

				int dbControl = getInt(Downloads.Impl.COLUMN_CONTROL);

				if (info.mControl == Downloads.CONTROL_RUN
						&& dbControl == Downloads.CONTROL_PAUSED) {
					info.mPauseNotiModifyFlag = false;
				}

				info.mControl = dbControl;

			}
		}

		/**
		 * read the request header parameters.
		 * 
		 * @param info
		 *            DownloadInfo
		 */
		private void readRequestHeaders(DownloadInfo info) {
			info.mRequestHeaders.clear();
			Uri headerUri = Uri.withAppendedPath(info.getAllDownloadsUri(),
					Downloads.Impl.RequestHeaders.URI_SEGMENT);
			Cursor cursor = mResolver.query(headerUri, null, null, null, null);
			try {
				int headerIndex = cursor
						.getColumnIndexOrThrow(Downloads.Impl.RequestHeaders.COLUMN_HEADER);
				int valueIndex = cursor
						.getColumnIndexOrThrow(Downloads.Impl.RequestHeaders.COLUMN_VALUE);
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
						.moveToNext()) {
					addHeader(info, cursor.getString(headerIndex),
							cursor.getString(valueIndex));
				}
			} finally {
				cursor.close();
			}

			if (info.mCookies != null) {
				addHeader(info, "Cookie", info.mCookies);
			}
			if (info.mReferer != null) {
				addHeader(info, "Referer", info.mReferer);
			}
		}

		/**
		 * add a header param to the downloadinfo.
		 * 
		 * @param info
		 *            DownloadInfo
		 * @param header
		 *            header key
		 * @param value
		 *            header value
		 */
		private void addHeader(DownloadInfo info, String header, String value) {
			info.mRequestHeaders.add(Pair.create(header, value));
		}

		/**
		 * Returns a String that holds the current value of the column,
		 * optimizing for the case where the value hasn't changed.
		 * 
		 * @param old
		 *            old
		 * @param column
		 *            column
		 * @return string
		 */
		private String getString(String old, String column) {
			int index = mCursor.getColumnIndexOrThrow(column);
			if (old == null) {
				return mCursor.getString(index);
			}
			if (mNewChars == null) {
				final int bufferSize = 128;
				mNewChars = new CharArrayBuffer(bufferSize);
			}
			mCursor.copyStringToBuffer(index, mNewChars);
			int length = mNewChars.sizeCopied;
			if (length != old.length()) {
				return new String(mNewChars.data, 0, length);
			}
			if (mOldChars == null || mOldChars.sizeCopied < length) {
				mOldChars = new CharArrayBuffer(length);
			}
			char[] oldArray = mOldChars.data;
			char[] newArray = mNewChars.data;
			old.getChars(0, length, oldArray, 0);
			for (int i = length - 1; i >= 0; --i) {
				if (oldArray[i] != newArray[i]) {
					return new String(newArray, 0, length);
				}
			}
			return old;
		}

		/**
		 * get int from the cusor.
		 * 
		 * @param column
		 *            columnname
		 * @return int value
		 */
		private Integer getInt(String column) {
			return mCursor.getInt(mCursor.getColumnIndexOrThrow(column));
		}

		/**
		 * get Long from the cusor.
		 * 
		 * @param column
		 *            columnname
		 * @return Long value
		 */
		private Long getLong(String column) {
			return mCursor.getLong(mCursor.getColumnIndexOrThrow(column));
		}
	}

	// the following NETWORK_* constants are used to indicates specfic reasons
	// for disallowing a
	// download from using a network, since specific causes can require special
	// handling

	/**
	 * The network is usable for the given download.
	 */
	public static final int NETWORK_OK = 1;

	/**
	 * There is no network connectivity.
	 */
	public static final int NETWORK_NO_CONNECTION = 2;

	/**
	 * The download exceeds the maximum size for this network.
	 */
	public static final int NETWORK_UNUSABLE_DUE_TO_SIZE = 3;

	/**
	 * The download exceeds the recommended maximum size for this network, the
	 * user must confirm for this download to proceed without WiFi.
	 */
	public static final int NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE = 4;

	/**
	 * The current connection is roaming, and the download can't proceed over a
	 * roaming connection.
	 */
	public static final int NETWORK_CANNOT_USE_ROAMING = 5;

	/**
	 * The app requesting the download specific that it can't use the current
	 * network connection.
	 */
	public static final int NETWORK_TYPE_DISALLOWED_BY_REQUESTOR = 6;

	/**
	 * For intents used to notify the user that a download exceeds a size
	 * threshold, if this extra is true, WiFi is required for this download
	 * size; otherwise, it is only recommended.
	 */
	public static final String EXTRA_IS_WIFI_REQUIRED = "isWifiRequired";

	/** 切换到暂停状态后，notification的样式是否已经修改 */
	public boolean mPauseNotiModifyFlag = true;

	/** id. */
	public long mId;
	/** uri. */
	public String mUri;
	/** mNoIntegrity */
	public boolean mNoIntegrity;
	/** hint text. */
	public String mHint;
	/** file name. */
	public String mFileName;
	/** mime type. */
	public String mMimeType;
	/** store destination. */
	public int mDestination;
	/** visible to the ui. */
	public int mVisibility;
	/** mControl */
	public int mControl = Downloads.CONTROL_PAUSED;
	/** status. */
	public int mStatus;
	/** mNumFailed. */
	public int mNumFailed;
	/** mRetryAfter */
	public int mRetryAfter;
	/** redirect count. */
	public int mRedirectCount;
	/** last modified time. */
	public long mLastMod;
	/** mPackage */
	public String mPackage;
	/** mClass . */
	public String mClass;
	/** mExtras . */
	public String mExtras;
	/** http cookies. */
	public String mCookies;
	/** http header ua. */
	public String mUserAgent;
	/** http header Referer. */
	public String mReferer;
	/** total size. */
	public long mTotalBytes;
	/** current downloaded size. */
	public long mCurrentBytes;
	/** http header etag. */
	public String mETag;
	/** has been scanned? */
	public boolean mMediaScanned;
	/** has been deleted ? */
	public boolean mDeleted;
	/** media provider uri. */
	public String mMediaProviderUri;
	/** mIsPublicApi. */
	public boolean mIsPublicApi;
	/** allowed network type, wifi/mobile. */
	public int mAllowedNetworkTypes;
	/** allow roaming network download */
	public boolean mAllowRoaming;
	/** title */
	public String mTitle;
	/** description */
	public String mDescription;
	/** the allowed limit size. */
	public int mBypassRecommendedSizeLimit;
	/** mFuzz */
	public int mFuzz;
	/** mHasActiveThread. */
	public volatile boolean mHasActiveThread;
	/** Download failed reason. */
	public String mFailedReason;
	/** mRequestHeaders . */
	private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();
	/** SystemFacade. */
	private SystemFacade mSystemFacade;
	/** context. */
	private Context mContext;

	/**
	 * DownloadInfo CONSTRUCTOR.
	 * 
	 * @param context
	 *            context
	 * @param systemFacade
	 *            SystemFacade
	 */
	private DownloadInfo(Context context, SystemFacade systemFacade) {
		mContext = context;
		mSystemFacade = systemFacade;
		mFuzz = Helpers.RANDOM.nextInt(1001); // SUPPRESS CHECKSTYLE
	}

	/**
	 * getHeaders.
	 * 
	 * @return the headers
	 */
	public Collection<Pair<String, String>> getHeaders() {
		return Collections.unmodifiableList(mRequestHeaders);
	}

	/**
	 * sendIntentIfRequested.
	 */
	public void sendIntentIfRequested() {
		if (mPackage == null) {
			return;
		}

		Intent intent;
		if (mIsPublicApi) {
			intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
			intent.setPackage(mPackage);
			intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, mId);
		} else { // legacy behavior
			if (mClass == null) {
				return;
			}
			intent = new Intent(Downloads.Impl.ACTION_DOWNLOAD_COMPLETED);
			intent.setClassName(mPackage, mClass);
			if (mExtras != null) {
				intent.putExtra(Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS,
						mExtras);
			}
			// We only send the content: URI, for security reasons. Otherwise,
			// malicious
			// applications would have an easier time spoofing download results
			// by
			// sending spoofed intents.
			intent.setData(getMyDownloadsUri());
		}
		mSystemFacade.sendBroadcast(intent);
	}

	/**
	 * Returns the time when a download should be restarted.
	 * 
	 * @return Returns the time when a download should be restarted.
	 * @param now
	 *            now
	 */
	public long restartTime(long now) {
		if (mNumFailed == 0) {
			return now;
		}
		if (mRetryAfter > 0) {
			return mLastMod + mRetryAfter;
		}
		// 修改等待策略，之前策略时间太长。
		// return mLastMod + Constants.RETRY_FIRST_DELAY * (1000 + mFuzz) * (1
		// << (mNumFailed - 1)); // SUPPRESS CHECKSTYLE
		return mLastMod + Constants.RETRY_FIRST_DELAY * 1000; // SUPPRESS
																// CHECKSTYLE
	}

	/**
	 * Returns whether this download (which the download manager hasn't seen
	 * yet) should be started.
	 * 
	 * @param now
	 *            now
	 * @return is ready?
	 */
	private boolean isReadyToStart(long now) {
		/*
		 * if (mHasActiveThread) { // already running return false; }
		 */
		if (mControl == Downloads.Impl.CONTROL_PAUSED) {
			// the download is paused, so it's not going to start
			return false;
		}
		switch (mStatus) {
		case 0: // status hasn't been initialized yet, this is a new download
		case Downloads.Impl.STATUS_PAUSED_BY_APP: // 暂停状态的下载可以重新开始
		case Downloads.Impl.STATUS_PENDING: // download is explicit marked as
											// ready to start
		case Downloads.Impl.STATUS_RUNNING: // download interrupted (process
											// killed etc) while
											// running, without a chance to
											// update the database
			return true;

		case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
		case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
			return checkCanUseNetwork() == NETWORK_OK;

		case Downloads.Impl.STATUS_WAITING_TO_RETRY:
			// download was waiting for a delayed restart
			return restartTime(now) <= now;
		default:
			break;
		}
		if (Downloads.Impl.isStatusCompleted(mStatus)) {
			if (mHasActiveThread) {
				mHasActiveThread = false;
			}
		}
		return false;
	}

	/**
	 * Returns whether this download has a visible notification after
	 * completion.
	 * 
	 * @return whether has completeion notification
	 */
	public boolean hasCompletionNotification() {
		if (!Downloads.Impl.isStatusCompleted(mStatus)) {
			return false;
		}
		if (mVisibility == Downloads.Impl.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) {
			return true;
		}
		return false;
	}

	/**
	 * Returns whether this download is allowed to use the network.
	 * 
	 * @return one of the NETWORK_* constants
	 */
	public int checkCanUseNetwork() {
		Integer networkType = mSystemFacade.getActiveNetworkType();
		if (networkType == null) {
			return NETWORK_NO_CONNECTION;
		}
		if (!isRoamingAllowed() && mSystemFacade.isNetworkRoaming()) {
			return NETWORK_CANNOT_USE_ROAMING;
		}
		return checkIsNetworkTypeAllowed(networkType);
	}

	/**
	 * if roaming is allowed.
	 * 
	 * @return alowed return true
	 */
	private boolean isRoamingAllowed() {
		if (mIsPublicApi) {
			return mAllowRoaming;
		} else { // legacy behavior
			return mDestination != Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING;
		}
	}

	/**
	 * RETURN a non-localized string appropriate for logging corresponding to
	 * one of the NETWORK_* constants.
	 * 
	 * @param networkError
	 *            networkError
	 * @return as the description
	 */
	public String getLogMessageForNetworkError(int networkError) {
		switch (networkError) {
		case NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE:
			return "download size exceeds recommended limit for mobile network";

		case NETWORK_UNUSABLE_DUE_TO_SIZE:
			return "download size exceeds limit for mobile network";

		case NETWORK_NO_CONNECTION:
			return "no network connection available";

		case NETWORK_CANNOT_USE_ROAMING:
			return "download cannot use the current network connection because it is roaming";

		case NETWORK_TYPE_DISALLOWED_BY_REQUESTOR:
			return "download was requested to not use the current network type";

		default:
			return "unknown error with network connectivity";
		}
	}

	/**
	 * Check if this download can proceed over the given network type.
	 * 
	 * @param networkType
	 *            a constant from ConnectivityManager.TYPE_*.
	 * @return one of the NETWORK_* constants
	 */
	private int checkIsNetworkTypeAllowed(int networkType) {
		if (mIsPublicApi) {
			int flag = translateNetworkTypeToApiFlag(networkType);
			if ((flag & mAllowedNetworkTypes) == 0) {
				return NETWORK_TYPE_DISALLOWED_BY_REQUESTOR;
			}
		}
		return checkSizeAllowedForNetwork(networkType);
	}

	/**
	 * Translate a ConnectivityManager.TYPE_* constant to the corresponding
	 * DownloadManager.Request.NETWORK_* bit flag.
	 * 
	 * @param networkType
	 *            network type
	 * @return the translated type
	 */
	private int translateNetworkTypeToApiFlag(int networkType) {
		switch (networkType) {
		case ConnectivityManager.TYPE_MOBILE:
			return DownloadManager.Request.NETWORK_MOBILE;

		case ConnectivityManager.TYPE_WIFI:
			return DownloadManager.Request.NETWORK_WIFI;

		default:
			return 0;
		}
	}

	/**
	 * Check if the download's size prohibits it from running over the current
	 * network.
	 * 
	 * @param networkType
	 *            network type
	 * @return one of the NETWORK_* constants
	 */
	private int checkSizeAllowedForNetwork(int networkType) {
		if (mTotalBytes <= 0) {
			return NETWORK_OK; // we don't know the size yet
		}
		if (networkType == ConnectivityManager.TYPE_WIFI) {
			return NETWORK_OK; // anything goes over wifi
		}
		Long maxBytesOverMobile = mSystemFacade.getMaxBytesOverMobile();
		if (maxBytesOverMobile != null && mTotalBytes > maxBytesOverMobile) {
			return NETWORK_UNUSABLE_DUE_TO_SIZE;
		}
		if (mBypassRecommendedSizeLimit == 0) {
			Long recommendedMaxBytesOverMobile = mSystemFacade
					.getRecommendedMaxBytesOverMobile();
			if (recommendedMaxBytesOverMobile != null
					&& mTotalBytes > recommendedMaxBytesOverMobile) {
				return NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE;
			}
		}
		return NETWORK_OK;
	}

	/**
	 * start the doanlowd if ready.
	 * 
	 * @param now
	 *            time
	 */
	void startIfReady(long now) {
		if (!isReadyToStart(now)
				|| DownloadService.mCurrentThreadNum > Constants.MAX_THREAD) {
			return;
		}

		if (Constants.LOGV) {
			Log.v(Constants.TAG, "Service spawning thread to handle download "
					+ mId);
		}
		if (mHasActiveThread) {
			// throw new
			// IllegalStateException("Multiple threads on same download");
			// already running
			return;
		}
		if (mStatus != Impl.STATUS_RUNNING) {
			mStatus = Impl.STATUS_RUNNING;
			ContentValues values = new ContentValues();
			values.put(Impl.COLUMN_STATUS, mStatus);
			mContext.getContentResolver().update(getAllDownloadsUri(), values,
					null, null);
		}
		DownloadThread downloader = new DownloadThread(mContext, mSystemFacade,
				this);
		mHasActiveThread = true;
		++DownloadService.mCurrentThreadNum;
		mSystemFacade.startThread(downloader);
	}

	/**
	 * whether on cache.
	 * 
	 * @return oncache return true
	 */
	public boolean isOnCache() {
		return (mDestination == Downloads.Impl.DESTINATION_CACHE_PARTITION
				|| mDestination == Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING || mDestination == Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE);
	}

	/**
	 * get my_downloads uri.
	 * 
	 * @return uri
	 */
	public Uri getMyDownloadsUri() {
		return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, mId);
	}

	/**
	 * get all_downloads uri.
	 * 
	 * @return the url.
	 */
	public Uri getAllDownloadsUri() {
		return ContentUris.withAppendedId(
				Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, mId);
	}

	/**
	 * log the verbose info .
	 */
	public void logVerboseInfo() {
		Log.v(Constants.TAG, "Service adding new entry");
		Log.v(Constants.TAG, "ID      : " + mId);
		Log.v(Constants.TAG, "URI     : " + mUri);
		Log.v(Constants.TAG, "NO_INTEG: " + mNoIntegrity);
		Log.v(Constants.TAG, "HINT    : " + mHint);
		Log.v(Constants.TAG, "FILENAME: " + mFileName);
		Log.v(Constants.TAG, "MIMETYPE: " + mMimeType);
		Log.v(Constants.TAG, "DESTINAT: " + mDestination);
		Log.v(Constants.TAG, "VISIBILI: " + mVisibility);
		Log.v(Constants.TAG, "CONTROL : " + mControl);
		Log.v(Constants.TAG, "STATUS  : " + mStatus);
		Log.v(Constants.TAG, "FAILED_C: " + mNumFailed);
		Log.v(Constants.TAG, "RETRY_AF: " + mRetryAfter);
		Log.v(Constants.TAG, "REDIRECT: " + mRedirectCount);
		Log.v(Constants.TAG, "LAST_MOD: " + mLastMod);
		Log.v(Constants.TAG, "PACKAGE : " + mPackage);
		Log.v(Constants.TAG, "CLASS   : " + mClass);
		Log.v(Constants.TAG, "COOKIES : " + mCookies);
		Log.v(Constants.TAG, "AGENT   : " + mUserAgent);
		Log.v(Constants.TAG, "REFERER : " + mReferer);
		Log.v(Constants.TAG, "TOTAL   : " + mTotalBytes);
		Log.v(Constants.TAG, "CURRENT : " + mCurrentBytes);
		Log.v(Constants.TAG, "ETAG    : " + mETag);
		Log.v(Constants.TAG, "SCANNED : " + mMediaScanned);
		Log.v(Constants.TAG, "DELETED : " + mDeleted);
		Log.v(Constants.TAG, "MEDIAPROVIDER_URI : " + mMediaProviderUri);
	}

	/**
	 * Returns the amount of time (as measured from the "now" parameter) at
	 * which a download will be active. 0 = immediately - service should stick
	 * around to handle this download. -1 = never - service can go away without
	 * ever waking up. positive value - service must wake up in the future, as
	 * specified in ms from "now"
	 * 
	 * @param now
	 *            now
	 * @return acton
	 */
	long nextAction(long now) {
		if (Downloads.Impl.isStatusCompleted(mStatus)) {
			return -1;
		}
		if (mStatus != Downloads.Impl.STATUS_WAITING_TO_RETRY) {
			return 0;
		}
		long when = restartTime(now);
		if (when <= now) {
			return 0;
		}
		return when - now;
	}

	/**
	 * Returns whether a file should be scanned
	 * 
	 * @return should scan
	 */
	boolean shouldScanFile() {
		return !mMediaScanned
				&& mDestination == Downloads.Impl.DESTINATION_EXTERNAL
				&& Downloads.Impl.isStatusSuccess(mStatus)
				&& !Constants.MIMETYPE_DRM_MESSAGE.equalsIgnoreCase(mMimeType);
	}

	/**
	 * notify to pause the download, due to the size of the file may be too
	 * large for the current Network.
	 * 
	 * @param isWifiRequired
	 *            whether wifi is prefered for the current download.
	 */
	void notifyPauseDueToSize(boolean isWifiRequired) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(getAllDownloadsUri());
		intent.setClassName(mContext.getPackageName(),
				SizeLimitActivity.class.getName());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_IS_WIFI_REQUIRED, isWifiRequired);
		mContext.startActivity(intent);
	}
}
