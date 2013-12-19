package com.fuyue.danmei.read;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.fuyue.danmei.BaseActivity;
import com.fuyue.danmei.config.ReadSettingConfig;
import com.fuyue.danmei.read.BookConstants.OrientationStyle;
import com.fuyue.danmei.read.BookConstants.ReadStyle;
import com.fuyue.danmei.read.BookItem.ChapterItem;
import com.fuyue.danmei.read.BookPageFactory.ChapterProgressCallback;
import com.fuyue.danmei.read.db.BooksReadChapterDao;
import com.fuyue.danmei.read.db.BooksReadProgressDao;
import com.fuyue.danmei.read.db.BooksReadProgressDao.BooksReadProgressItem;
import com.fuyue.frame.R;
import com.fuyue.util.Utility;
import com.fuyue.util.logging.Log;
import com.fuyue.util.ui.BookPageWidget;
import com.kyview.AdViewLayout;
import com.kyview.AdViewTargeting;
import com.kyview.AdViewTargeting.SwitcherMode;
import com.kyview.interstitial.AdInstlManager;
import com.kyview.interstitial.AdInstlTargeting;
import com.umeng.analytics.MobclickAgent;

/**
 * 图书阅读页面
 * 
 * @author Calvin
 * 
 */
public class BooksReadActivity extends BaseActivity {

	/** key book info */
	private static final String BOOK_ITEM = "bookitem";
	/** key chapter index */
	private static final String CHAPTER_INDEX = "chapterindex";

	/**
	 * 获取图书阅读页面的intent
	 * 
	 * @param activity
	 * @param item
	 * @return
	 */
	public static Intent getIntent(Activity activity, BookItem bookinfo,
			int chapterIndex) {
		Intent intent = new Intent(activity, BooksReadActivity.class);
		intent.putExtra(BOOK_ITEM, bookinfo);
		intent.putExtra(CHAPTER_INDEX, chapterIndex);
		return intent;
	}

	/** 正文字体大小 */
	private int mFontSize;
	/** 阅读模式 */
	private ReadStyle mReadStyle;
	/** 旋转方式 */
	private OrientationStyle mOrientationStyle;

	/** 图书信息 */
	private BookItem mBookInfo;
	/** 章节列表 */
	private List<ChapterItem> mChapters;
	/** 当前章节索引 */
	private int mCurrentChapterIndex;

	/** 绘制当前页面以及下一个页面 */
	public Canvas mCurPageCanvas, mNextPageCanvas;
	private Bitmap mCurPageBitmap, mNextPageBitmap;

	/** 翻页效果绘制类 */
	private BookPageWidget mPageWidget;
	private BookPageFactory mPagefactory;

	/** 控制器 */
	private ReadContollerHolder mContollerHolder;
	/** 时间监听 */
	private TimeChangeReceiver mTimeChangeReceiver = new TimeChangeReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.book_read);

		// 初始化绘制视图
		ViewGroup content = (ViewGroup) findViewById(R.id.layout_content);

		// 初始化控制器部分
		mContollerHolder = new ReadContollerHolder();
		mContollerHolder.mBackBtn = findViewById(R.id.btn_read_control_back);
		mContollerHolder.mControlView = findViewById(R.id.layout_read_control);
		mContollerHolder.mFontControllerView = findViewById(R.id.layout_read_control_font);
		mContollerHolder.mControlDismissBtn = findViewById(R.id.btn_read_control_dismiss);
		mContollerHolder.mFontSizeBtn = findViewById(R.id.btn_read_control_fontsize);
		mContollerHolder.mFontSizeSmallBtn = findViewById(R.id.btn_read_control_fontsize_small);
		mContollerHolder.mFontSizeNormalBtn = findViewById(R.id.btn_read_control_fontsize_normal);
		mContollerHolder.mFontSizeLargeBtn = findViewById(R.id.btn_read_control_fontsize_large);
		mContollerHolder.mReadStyleBtn = findViewById(R.id.btn_read_control_readstyle);
		mContollerHolder.mOrientationBtn = findViewById(R.id.btn_read_control_orientation);
		mContollerHolder.mSaveBtn = findViewById(R.id.btn_read_control_save);
		mContollerHolder.mFinishHint = findViewById(R.id.text_read_control_hint);

		mContollerHolder.mBackBtn.setOnClickListener(mBackOnCancelListener);
		mContollerHolder.mControlView
				.setOnClickListener(mDismissOnClickListener);
		mContollerHolder.mControlDismissBtn
				.setOnClickListener(mDismissOnClickListener);
		mContollerHolder.mFontSizeBtn
				.setOnClickListener(mFontSizeOnClickListener);
		mContollerHolder.mFontSizeSmallBtn
				.setOnClickListener(mFontSizeOnClickListener);
		mContollerHolder.mFontSizeNormalBtn
				.setOnClickListener(mFontSizeOnClickListener);
		mContollerHolder.mFontSizeLargeBtn
				.setOnClickListener(mFontSizeOnClickListener);
		mContollerHolder.mReadStyleBtn
				.setOnClickListener(mReadStyleOnClickListener);
		mContollerHolder.mOrientationBtn
				.setOnClickListener(mOrientationOnClickListener);
		mContollerHolder.mSaveBtn.setOnClickListener(mSaveOnClickListener);

		int screenWidth = (int) Utility.getScreenWidth(this);
		int screenHeight = (int) ((int) Utility.getScreenHeight(this) - 50 * Utility
				.getDensity(this));

		mFontSize = ReadSettingConfig.getInstance(this).getFontSize();
		mReadStyle = ReadSettingConfig.getInstance(this).getReadStyle();
		mOrientationStyle = ReadSettingConfig.getInstance(this)
				.getOrientationStyle();

		// 初始化图书信息
		mBookInfo = (BookItem) getIntent().getExtras().getSerializable(
				BOOK_ITEM);
		mCurrentChapterIndex = getIntent().getExtras().getInt(CHAPTER_INDEX);
		mChapters = mBookInfo.mChapterItems;

		// 初始化绘制画布
		mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
				Bitmap.Config.ARGB_8888);
		mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
				Bitmap.Config.ARGB_8888);
		mCurPageCanvas = new Canvas(mCurPageBitmap);
		mNextPageCanvas = new Canvas(mNextPageBitmap);

		// 初始化界面
		mPageWidget = new BookPageWidget(this, screenWidth, screenHeight);
		content.addView(mPageWidget);
		mPageWidget.setBitmaps(mCurPageBitmap, mCurPageBitmap);
		mPageWidget.setStyle(mReadStyle);
		mPageWidget.setOnTouchListener(mOnPageDargListener);

		// 初始化页面信息绘制类
		mPagefactory = new BookPageFactory(this, mFontSize, mReadStyle);
		mPagefactory.setChapterProgressCallback(new ChapterProgressCallback() {

			@Override
			public int getChapterStartProgress() {
				return getProgressByChapterIndex(mCurrentChapterIndex);
			}

			@Override
			public int getBookTotalLength() {
				return getBookTotalProgress();
			}
		});

		openBook(mCurrentChapterIndex, getChapterProgressWithDbProgress());
		mPagefactory.drawPage(mCurPageCanvas);
		adjustOrientation();
		adjustController();
		autoDismiss();

		registerReceiver(mTimeChangeReceiver, new IntentFilter(
				Intent.ACTION_TIME_TICK));

		AdViewTargeting.setSwitcherMode(SwitcherMode.DEFAULT); // 广告可被关闭，如不需要可修改为SwitcherMode.DEFAULT

		/* 下面两行只用于测试,完成后一定要去掉,参考文挡说明 */
		// AdViewTargeting.setUpdateMode(UpdateMode.EVERYTIME); // 每次都从服务器取配置
		// AdViewTargeting.setRunMode(RunMode.TEST); // 保证所有选中的广告公司都为测试状态
		/* 下面这句方便开发者进行发布渠道统计,详细调用可以参考java doc */
		// AdViewTargeting.setChannel(Channel.GOOGLEMARKET);
		AdViewLayout adViewLayout = (AdViewLayout) findViewById(R.id.ad_view);
	}

	/**
	 * 设置自动消失
	 */
	private void autoDismiss() {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				if (mContollerHolder.mControlView.getVisibility() != View.GONE) {
					hideController();
				}
			}
		}, 3000);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (mContollerHolder.mControlView.getVisibility() != View.VISIBLE) {
				showController();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/** 拖拽事件进行中 */
	private boolean mIsDraging = false;

	/**
	 * 时间变化监听
	 * 
	 * @author Calvin
	 * 
	 */
	private class TimeChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mIsDraging) {
				return;
			}
			adjustPageFactory();
		}

	}

	/**
	 * 调整屏幕方向
	 */
	private void adjustOrientation() {
		if (mOrientationStyle == OrientationStyle.PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (mOrientationStyle == OrientationStyle.LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	/**
	 * 调整页面工厂
	 */
	private void adjustPageFactory() {
		if (mPagefactory != null) {
			int screenWidth = (int) Utility.getScreenWidth(this);
			int screenHeight = (int) ((int) Utility.getScreenHeight(this) - 50 * Utility
					.getDensity(this));

			mPageWidget.setScreen(screenWidth, screenHeight);
			mPageWidget.setStyle(mReadStyle);
			mPagefactory.updateFactory(mFontSize, mReadStyle);

			mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
					Bitmap.Config.ARGB_8888);
			mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
					Bitmap.Config.ARGB_8888);
			mCurPageCanvas = new Canvas(mCurPageBitmap);
			mNextPageCanvas = new Canvas(mNextPageBitmap);
			mPageWidget.setBitmaps(mCurPageBitmap, mCurPageBitmap);

			mPagefactory.reCalculateCurrentPage();
			mPagefactory.drawPage(mCurPageCanvas);

			mPageWidget.invalidate();
		}
	}

	/**
	 * 调整控制器
	 */
	private void adjustController() {
		if (mPagefactory.isLastPage()) {
			mContollerHolder.mFontSizeBtn.setVisibility(View.GONE);
			mContollerHolder.mFontControllerView.setVisibility(View.GONE);
			mContollerHolder.mReadStyleBtn.setVisibility(View.GONE);
			mContollerHolder.mOrientationBtn.setVisibility(View.GONE);

			mContollerHolder.mSaveBtn.setVisibility(View.VISIBLE);
			mContollerHolder.mFinishHint.setVisibility(View.VISIBLE);
		} else {
			mContollerHolder.mFontSizeBtn.setVisibility(View.VISIBLE);
			mContollerHolder.mReadStyleBtn.setVisibility(View.VISIBLE);
			mContollerHolder.mOrientationBtn.setVisibility(View.VISIBLE);

			mContollerHolder.mSaveBtn.setVisibility(View.GONE);
			mContollerHolder.mFinishHint.setVisibility(View.GONE);
		}

		// 阅读模式按钮
		if (mReadStyle == ReadStyle.LIGHT) {
			mContollerHolder.mReadStyleBtn
					.setBackgroundResource(R.drawable.btn_read_controller_night);
		} else {
			mContollerHolder.mReadStyleBtn
					.setBackgroundResource(R.drawable.btn_read_controller_day);
		}

		// 字体调整按钮
		if (mFontSize == BookConstants.TEXT_SIZE_SMALL) {
			mContollerHolder.mFontSizeSmallBtn
					.setBackgroundResource(R.drawable.bg_read_controller_font);
			mContollerHolder.mFontSizeNormalBtn
					.setBackgroundColor(Color.TRANSPARENT);
			mContollerHolder.mFontSizeLargeBtn
					.setBackgroundColor(Color.TRANSPARENT);

			((TextView) mContollerHolder.mFontSizeSmallBtn).setTextColor(Color
					.parseColor("#ffffff"));
			((TextView) mContollerHolder.mFontSizeNormalBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
			((TextView) mContollerHolder.mFontSizeLargeBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
		} else if (mFontSize == BookConstants.TEXT_SIZE_NORMAL) {
			mContollerHolder.mFontSizeNormalBtn
					.setBackgroundResource(R.drawable.bg_read_controller_font);
			mContollerHolder.mFontSizeSmallBtn
					.setBackgroundColor(Color.TRANSPARENT);
			mContollerHolder.mFontSizeLargeBtn
					.setBackgroundColor(Color.TRANSPARENT);

			((TextView) mContollerHolder.mFontSizeSmallBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
			((TextView) mContollerHolder.mFontSizeNormalBtn).setTextColor(Color
					.parseColor("#ffffff"));
			((TextView) mContollerHolder.mFontSizeLargeBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
		} else if (mFontSize == BookConstants.TEXT_SIZE_LARGE) {
			mContollerHolder.mFontSizeLargeBtn
					.setBackgroundResource(R.drawable.bg_read_controller_font);
			mContollerHolder.mFontSizeNormalBtn
					.setBackgroundColor(Color.TRANSPARENT);
			mContollerHolder.mFontSizeSmallBtn
					.setBackgroundColor(Color.TRANSPARENT);

			((TextView) mContollerHolder.mFontSizeSmallBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
			((TextView) mContollerHolder.mFontSizeNormalBtn).setTextColor(Color
					.parseColor("#aaaaaa"));
			((TextView) mContollerHolder.mFontSizeLargeBtn).setTextColor(Color
					.parseColor("#ffffff"));
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		adjustOrientation();
		adjustPageFactory();
		super.onConfigurationChanged(newConfig);
	}

	private OnTouchListener mOnPageDargListener = new OnTouchListener() {

		/** 记录视图中央点击事件 */
		private boolean mCenterClickDispatch = false;

		/**
		 * 是否点击的位置为屏幕中央
		 * 
		 * @param v
		 * @param e
		 * @return
		 */
		private boolean isInCenter(View v, MotionEvent e) {
			int touchX = (int) e.getX();
			int touchY = (int) e.getY();

			if ((touchX > v.getWidth() * 1 / 3)
					&& (touchX < v.getWidth() * 2 / 3)
					&& (touchY > v.getHeight() * 1 / 3)
					&& (touchY < v.getHeight() * 2 / 3)) {
				return true;
			}
			return false;
		}

		/**
		 * 点击事件
		 */
		private void onClick() {
			showController();
		}

		@Override
		public boolean onTouch(View v, MotionEvent e) {
			// 首先判断是否用户点击目的为点击屏幕中央，唤起点击事件
			if (e.getAction() == MotionEvent.ACTION_UP && mCenterClickDispatch
					&& isInCenter(v, e)) {
				onClick();
				mCenterClickDispatch = false;
				return true;
			} else if (e.getAction() == MotionEvent.ACTION_UP
					&& mCenterClickDispatch) {
				mCenterClickDispatch = false;
			} else if (e.getAction() == MotionEvent.ACTION_MOVE
					&& mCenterClickDispatch) {
				return true;
			} else if (e.getAction() == MotionEvent.ACTION_DOWN) {
				if (isInCenter(v, e)) {
					mCenterClickDispatch = true;
					return true;
				}
			}

			// 书页拉拽控制
			if (e.getAction() == MotionEvent.ACTION_DOWN) {
				mIsDraging = true;
				mPageWidget.abortAnimation();
				mPageWidget.calcCornerXY(e.getX(), e.getY());
				mPagefactory.drawPage(mCurPageCanvas);
				/** 左翻 */
				if (mPageWidget.DragToRight()) {
					if (mPagefactory.isFirstPage()) {
						int prePosition = mCurrentChapterIndex - 1;
						if (prePosition < 0) {
							Toast.makeText(getApplicationContext(),
									R.string.book_read_first,
									Toast.LENGTH_SHORT).show();
							mPageWidget.doTouchEvent(e, true);
							mIsDraging = false;
							return false;
						}
						// 此处begin并无实际意义，具体begin位置会在turnPreChapter中重算
						openBook(prePosition,
								getChapterTotalProgress(prePosition));
						mCurrentChapterIndex--;
						mPagefactory.turnPreChapter();
					} else {
						mPagefactory.turePrePage();
					}
					mPagefactory.drawPage(mNextPageCanvas);
				} else {// 右翻
					if (mPagefactory.isLastPage()) {
						int nextPosition = mCurrentChapterIndex + 1;
						if (nextPosition > mChapters.size() - 1) {
							Toast.makeText(getApplicationContext(),
									R.string.book_read_last, Toast.LENGTH_SHORT)
									.show();
							mPageWidget.doTouchEvent(e, true);
							mIsDraging = false;
							return false;
						}
						openBook(nextPosition, 0);
						mCurrentChapterIndex++;
						mPagefactory.turnNextChapter();
					} else {
						mPagefactory.tureNextPage();
					}
					mPagefactory.drawPage(mNextPageCanvas);
				}
				mPageWidget.setBitmaps(mCurPageBitmap, mNextPageBitmap);
			} else if (e.getAction() == MotionEvent.ACTION_UP
					|| e.getAction() == MotionEvent.ACTION_MOVE) {
				/** 左翻 */
				if (mPageWidget.DragToRight()) {
					if (mPagefactory.isFirstPage()) {
						int prePosition = mCurrentChapterIndex - 1;
						if (prePosition < 0) {
							mIsDraging = false;
							return mPageWidget.doTouchEvent(e, true);
						}
					}
					mPagefactory.drawPage(mNextPageCanvas);
				} else {// 右翻
					if (mPagefactory.isLastPage()) {
						showController();

						// 插入广告
						AdInstlTargeting
								.setAdWidthHeight(
										(int) Utility
												.getScreenWidth(BooksReadActivity.this),
										(int) Utility
												.getScreenHeight(BooksReadActivity.this));
						AdInstlManager adInstlManager = new AdInstlManager(
								BooksReadActivity.this,
								Utility.getApplicationStringMetaData(
										BooksReadActivity.this,
										"ADVIEW_SDK_KEY"));
						adInstlManager.requestad();

						int nextPosition = mCurrentChapterIndex + 1;
						if (nextPosition > mChapters.size() - 1) {
							mIsDraging = false;
							return mPageWidget.doTouchEvent(e, true);
						}
					}
				}
				if (e.getAction() == MotionEvent.ACTION_UP) {
					mIsDraging = false;
				}
			}
			return mPageWidget.doTouchEvent(e, false);
		}
	};

	@Override
	protected void onPause() {
		// 不放在onDestory中，实际上onDestory的调用要晚于上一个页面的onResume的调用，导致页面无法正常刷新显示
		float current = mPagefactory.getCurrentPageProgress()
				+ getProgressByChapterIndex(mCurrentChapterIndex);
		int total = getBookTotalProgress();
		float fPercent = current / total;
		DecimalFormat df = new DecimalFormat("#0.0");
		String strPercent = df.format(fPercent * 100);

		BooksReadProgressDao.getInstance(this).insertProgress(
				mBookInfo.mBookId,
				mChapters.get(mCurrentChapterIndex).mChapterId,
				mPagefactory.getCurrentPageProgress(),
				Float.parseFloat(strPercent));
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onDestroy() {
		if (mCurPageBitmap != null && !mCurPageBitmap.isRecycled()) {
			mCurPageBitmap.recycle();
		}
		if (mNextPageBitmap != null && !mNextPageBitmap.isRecycled()) {
			mNextPageBitmap.recycle();
		}
		unregisterReceiver(mTimeChangeReceiver);
		mPagefactory.release();
		super.onDestroy();
	}

	/**
	 * @describe 打开图书
	 * @param pageIndex
	 * @param begin
	 */
	public void openBook(int pageIndex, int begin) {
		try {
			mPagefactory.openbook(mBookInfo, mChapters.get(pageIndex), begin);
			// 添加已读章节
			BooksReadChapterDao.getInstance(this).insertChapterRecord(
					mBookInfo.mBookId, mChapters.get(pageIndex).mChapterId);
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
			Toast.makeText(this, "打开电子书失败", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
	}

	/**
	 * 获取整本书的长度
	 * 
	 * @return
	 */
	private int getBookTotalProgress() {
		int totalLen = 0;
		for (ChapterItem chapterItem : mChapters) {
			totalLen += chapterItem.getChapterLength(this);
		}
		return totalLen;
	}

	/**
	 * 获取某一章的长度
	 * 
	 * @param pageIndex
	 * @return
	 */
	private int getChapterTotalProgress(int pageIndex) {
		int chapterLength = (int) mChapters.get(pageIndex).getChapterLength(
				this);
		return chapterLength;
	}

	/**
	 * 
	 * 计算当前章节在全本之中的起始进度
	 * 
	 * @param chapterIndex
	 * @param totalProgress
	 * @return
	 */
	private int getProgressByChapterIndex(int chapterIndex) {
		int totalLen = 0;
		for (int i = 0; i < chapterIndex; i++) {
			totalLen += mChapters.get(i).getChapterLength(this);
		}
		return totalLen;
	}

	/**
	 * 通过数据库中记录的阅读进度反查当前章节的续读进度
	 * 
	 * @return
	 */
	private int getChapterProgressWithDbProgress() {

		BooksReadProgressItem item = BooksReadProgressDao.getInstance(this)
				.queryProgressByBookId(mBookInfo.mBookId);

		if (item != null
				&& mChapters.get(mCurrentChapterIndex).mChapterId == item.mChapterId) {
			return item.mProgress;
		}
		return -1;
	}

	/*
	 * 控制器部分
	 */

	/**
	 * 显示控制器
	 */
	private void showController() {
		adjustController();
		mContollerHolder.mControlView.setVisibility(View.VISIBLE);
		mContollerHolder.mControlView.postInvalidate();
		Animation displayAnimation = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in);
		displayAnimation.setFillAfter(true);
		displayAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mContollerHolder.mControlView.postInvalidate();
			}
		});
		mContollerHolder.mControlView.startAnimation(displayAnimation);
	}

	/**
	 * 隐藏控制器
	 */
	private void hideController() {
		Animation dismissAnimation = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out);
		dismissAnimation.setFillAfter(true);
		dismissAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				mContollerHolder.mControlDismissBtn.setEnabled(false);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mContollerHolder.mControlView.clearAnimation();
				mContollerHolder.mControlView.setVisibility(View.GONE);
				mContollerHolder.mControlDismissBtn.setEnabled(true);

				if (mContollerHolder.mFontControllerView.getVisibility() == View.VISIBLE) {
					mContollerHolder.mFontControllerView
							.setVisibility(View.GONE);
				}
			}
		});
		mContollerHolder.mControlView.startAnimation(dismissAnimation);
	}

	/** 返回 */
	private OnClickListener mBackOnCancelListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};

	/** 控制器消失点击监听 */
	private OnClickListener mDismissOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == mContollerHolder.mControlView) {
				return;
			}
			hideController();
		}
	};

	/** 字体大小点击监听 */
	private OnClickListener mFontSizeOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == mContollerHolder.mFontSizeBtn) {
				if (mContollerHolder.mFontControllerView.getVisibility() == View.GONE) {
					mContollerHolder.mFontControllerView
							.setVisibility(View.VISIBLE);
				} else {
					mContollerHolder.mFontControllerView
							.setVisibility(View.GONE);
				}
				return;
			}
			if (v == mContollerHolder.mFontSizeSmallBtn) {
				mFontSize = BookConstants.TEXT_SIZE_SMALL;
			} else if (v == mContollerHolder.mFontSizeNormalBtn) {
				mFontSize = BookConstants.TEXT_SIZE_NORMAL;
			} else if (v == mContollerHolder.mFontSizeLargeBtn) {
				mFontSize = BookConstants.TEXT_SIZE_LARGE;
			}
			ReadSettingConfig.getInstance(getApplicationContext()).setFontSize(
					mFontSize);
			adjustPageFactory();
			adjustController();
		}
	};

	/** 阅读模式点击监听 */
	private OnClickListener mReadStyleOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mReadStyle == ReadStyle.LIGHT) {
				mReadStyle = ReadStyle.DARK;
			} else {
				mReadStyle = ReadStyle.LIGHT;
			}
			ReadSettingConfig.getInstance(getApplicationContext())
					.setReadStyle(mReadStyle);
			adjustPageFactory();
			adjustController();
		}
	};

	/** 屏幕方向点击监听 */
	private OnClickListener mOrientationOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mOrientationStyle == OrientationStyle.LANDSCAPE) {
				mOrientationStyle = OrientationStyle.PORTRAIT;
			} else {
				mOrientationStyle = OrientationStyle.LANDSCAPE;
			}
			ReadSettingConfig.getInstance(getApplicationContext())
					.setOrientationStyle(mOrientationStyle);
			adjustOrientation();
		}
	};

	/** 保存图片点击监听 */
	private OnClickListener mSaveOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			BookToast.showSaveToast(BooksReadActivity.this);
			Bitmap bitmap = mBookInfo
					.getBackCoverImage(getApplicationContext());
			MediaStore.Images.Media.insertImage(getContentResolver(), bitmap,
					"myPhoto", "");
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory())));
		}
	};

	/**
	 * 控制器view容器
	 * 
	 * @author Calvin
	 * 
	 */
	private class ReadContollerHolder {
		/** 控制器 */
		private View mControlView;
		/** 字体大小控制 */
		private View mFontControllerView;

		/** 控制器显示消失控制 */
		private View mControlDismissBtn;

		/** 返回按钮 */
		private View mBackBtn;
		/** 字体大小调整按钮 */
		private View mFontSizeBtn;
		/** 阅读模式调整按钮 */
		private View mReadStyleBtn;
		/** 屏幕方向调整按钮 */
		private View mOrientationBtn;

		/** 字体大小调整按钮 小号 */
		private View mFontSizeSmallBtn;
		/** 字体大小调整按钮 中号 */
		private View mFontSizeNormalBtn;
		/** 字体大小调整按钮 大号 */
		private View mFontSizeLargeBtn;

		/** 保存图片 */
		private View mSaveBtn;
		/** 完结提示 */
		private View mFinishHint;
	}
}