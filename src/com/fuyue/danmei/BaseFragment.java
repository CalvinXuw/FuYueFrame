package com.fuyue.danmei;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.fuyue.BaseApplicaion;
import com.fuyue.frame.R;
import com.fuyue.util.SdkVersionUtils;
import com.fuyue.util.Utility;
import com.fuyue.util.imagecache.ImageCache.ImageCacheParams;
import com.fuyue.util.imagecache.ImageFetcher;
import com.fuyue.util.logging.Log;
import com.fuyue.util.model.AbstractModel.OnModelProcessListener;
import com.fuyue.util.model.ModelManageQueue;

/**
 * BaseFragment所有实现的Fragment页面应将继承自此类
 * 
 * @author Xuwei
 * 
 */
public abstract class BaseFragment extends Fragment implements
		OnModelProcessListener {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/** 默认image cache dir */
	private final String IMAGE_CACHE_DIR = "images";

	/** 图片缓存文件夹 */
	protected String mImageCacheDir = "images";
	/** 图片最大尺寸 */
	protected int mImageSize;
	/** 图片加载工具类 */
	protected ImageFetcher mImageFetcher;

	/** model管理类 */
	protected ModelManageQueue mModelManageQueue;

	/** 像素密度 */
	protected float mDensity;
	/** 宽度 dip */
	protected int mWidth;
	/** 高度 dip */
	protected int mHeight;

	/** 是否需要初始化ImageFetcher */
	private boolean mIsNeedImageFetcher = true;

	/**
	 * 需要一个空的构造方法
	 */
	public BaseFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
	}

	/**
	 * 设置是否需要imageFetcher图片加载框架，ImageFetcher会涉及使用到
	 * {@link android.app.Fragment#setRetainInstance(boolean)}方法，该方法并不适用于Nested
	 * Fragment。
	 * 
	 * @param isNeed
	 */
	protected void setNeedImageFetcher(boolean isNeed) {
		mIsNeedImageFetcher = isNeed;
	}

	/**
	 * 初始化
	 */
	private void init() {
		mDensity = Utility.getDensity(getActivity());
		mHeight = (int) (Utility.getScreenHeight(getActivity()) / mDensity);
		mWidth = (int) (Utility.getScreenWidth(getActivity()) / mDensity);

		if (mIsNeedImageFetcher) {
			setupImageFetcher();
		}

		mModelManageQueue = new ModelManageQueue();
		setupModel();
	}

	/**
	 * 设定独立的缓存配置，如：在列表缩略图或大图展示之中，图片尺寸及缓存目录都需要进行分别的设置。
	 */
	protected void setImageCacheParams() {
		if (mIsNeedImageFetcher) {
			if (DEBUG) {
				Log.w(TAG, "should override the setImageCacheParams method");
			}
		}
		// do nothing
	}

	/**
	 * 初始化ImageFetcher
	 */
	private void setupImageFetcher() {
		setImageCacheParams();
		if (mImageSize == 0) {
			mImageSize = (mHeight > mWidth ? mHeight : mWidth) / 2;
		}

		if (mImageCacheDir == null) {
			mImageCacheDir = IMAGE_CACHE_DIR;
		}

		ImageCacheParams params = new ImageCacheParams(getActivity(),
				mImageCacheDir);
		params.setMemCacheSizePercent(0.2f);

		mImageFetcher = new ImageFetcher(getActivity(), mImageSize,
				mImageCacheDir);
		mImageFetcher.addImageCache(getFragmentManager(), params);
	}

	/**
	 * 加载当前页面需要用到的model
	 */
	protected void setupModel() {
		// do nothing
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mImageFetcher != null) {
			mImageFetcher.setPauseWork(false);
		}

		mModelManageQueue.pauseQueue(false);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mImageFetcher != null) {
			mImageFetcher.setPauseWork(true);
		}

		mModelManageQueue.pauseQueue(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mImageFetcher != null) {
			mImageFetcher.setExitTasksEarly(true);
			mImageFetcher.clearMemoryCache();
			mImageFetcher.flushCache();
			mImageFetcher.closeCache();
		}

		mModelManageQueue.clearQueue();
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		if (SdkVersionUtils.hasFroyo()) {
			getActivity().overridePendingTransition(
					R.anim.activity_anim_push_left_in,
					R.anim.activity_anim_push_left_out);
		}
	}

	/**
	 * 需要子类重写父类方法，用于提示的异常
	 * 
	 * @author Calvin
	 * 
	 */
	protected class NeedOverrideException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5969876713354283460L;

		public NeedOverrideException(String string) {
			super(string);
		}
	}
}