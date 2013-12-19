package com.fuyue.util.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 顶部滑动Tab栏自定义控件。预先调用{@link #setNormalTextColor}、{@link #setNormalTextSize}
 * 等配置方法后， 调用{@link #addTabsByTitles}添加标签栏内容。
 * 
 * @author XuWei 2013-5-27
 * 
 */
public class SlideTabbarView extends FrameLayout {

	/** 默认滑块滑动时间 */
	private int DEFAULT_SLIDE_TIME = 250;
	/** 默认动画帧数 */
	private int DEFAULT_ANIMATION_FPS = 16;
	/** 默认Tab标题字色 */
	private int DEFAULI_TEXTCOLOR = Color.BLACK;
	/** 默认Tab标题字号 */
	private int DEFAULT_TEXTSIZE = 20;
	/** 默认资源id */
	private int DEFAULT_RES_ID = -1;
	/** 初始化标签 */
	private int DEFAULT_TAB = -1;

	/** 滑动时间 */
	private int mSlideTime = DEFAULT_SLIDE_TIME;
	/** 动画帧数 */
	private int mFps = DEFAULT_ANIMATION_FPS;
	/** 正常态Tab标题字色 */
	private int mNormalTextColor = DEFAULI_TEXTCOLOR;
	/** 激活态Tab标题字色 */
	private int mActiveTextColor = DEFAULI_TEXTCOLOR;
	/** 正常态Tab标题字号 */
	private int mNormalTextSize = DEFAULT_TEXTSIZE;
	/** 激活态Tab标题字号 */
	private int mActiveTextSize = DEFAULT_TEXTSIZE;

	/** 标签栏容器 */
	private HorizontalScrollView mHorizontalScrollView;
	/** Tab标题的容器 */
	private LinearLayout mTabContainerLayout;
	/** 背景色View */
	private View mBackgroundView;
	/** 滑块View */
	private View mHintView;
	/** 背景资源id */
	private int mBackgroundResId = DEFAULT_RES_ID;
	/** 滑块背景资源id */
	private int mHintResId = DEFAULT_RES_ID;

	/** Tab标题 */
	private String[] mTabTitles;
	/** 是否处于滑动状态下 */
	private boolean mMoving;
	/** Tab点击事件监听 */
	private OnTabSelectedListener mOnTabSelectedListener;
	/** 滑块锁 */
	private Object mMovingLock = new Object();

	/** 屏幕宽度 */
	private int mWidth;
	/** 屏幕像素密度 */
	private float mScaledDensity;

	/** 当前选中标签 */
	private int mCurrent;
	/** 像素密度 */
	private float mDensity;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public SlideTabbarView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (mMoving && !changed) {
			return;
		}
		super.onLayout(changed, left, top, right, bottom);

		if (mTabTitles != null && !mMoving) {
			View targetView = mTabContainerLayout.getChildAt(mCurrent);
			mHintView.layout(targetView.getLeft(), targetView.getTop(),
					targetView.getRight(), targetView.getBottom());
			mHorizontalScrollView.smoothScrollTo(targetView.getLeft(), 0);
		}
	}

	/**
	 * 初始化基本参数及对象
	 */
	private void init() {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		mDensity = dm.density;

		mBackgroundView = new View(getContext());
		mBackgroundView.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHintView = new View(getContext());
		mHintView.setLayoutParams(new LayoutParams(0, 0));

		mTabContainerLayout = new LinearLayout(getContext());
		mTabContainerLayout.setOrientation(LinearLayout.HORIZONTAL);
		mTabContainerLayout.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHorizontalScrollView = new HorizontalScrollView(getContext());
		mHorizontalScrollView.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		FrameLayout frameLayout = new FrameLayout(getContext());
		frameLayout.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHorizontalScrollView.setHorizontalScrollBarEnabled(false);

		frameLayout.addView(mHintView);
		frameLayout.addView(mTabContainerLayout);

		mHorizontalScrollView.addView(frameLayout);

		// 注意addView的先后顺序，保持遮罩效果显示正常
		addView(mBackgroundView);
		addView(mHorizontalScrollView);

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(displayMetrics);
		mWidth = dm.widthPixels;
		mScaledDensity = getResources().getDisplayMetrics().scaledDensity;
	}

	/**
	 * 根据分页创建标签栏，用于定栏目的标签，比如仅有2、3页时
	 * 
	 * @param listener
	 * @param titles
	 */
	public void addTabsByTabs(OnTabSelectedListener listener, String... titles) {
		addTabs((int) px2dip(mWidth / titles.length), listener, titles);
	}

	/**
	 * 根据分页标题创建标签栏，用于滑动的多栏目标签
	 * 
	 * @param tabWidth
	 * @param listener
	 * @param titles
	 */
	public void addTabsByTitles(int tabWidth, OnTabSelectedListener listener,
			String... titles) {
		addTabs(tabWidth, listener, titles);
	}

	/**
	 * 对应提供的标题进行初始化
	 * 
	 * @param tabWidth
	 *            分页宽度
	 * @param listener
	 *            {@link OnTabSelectedListener}
	 * @param titles
	 */
	private void addTabs(int tabWidth, OnTabSelectedListener listener,
			String... titles) {
		if (titles == null) {
			return;
		}

		mOnTabSelectedListener = listener;
		mTabTitles = titles;

		mTabContainerLayout.removeAllViews();
		int tabSize = titles.length;
		mTabContainerLayout.setWeightSum(tabSize);
		for (int i = 0; i < mTabTitles.length; i++) {
			String title = mTabTitles[i];
			TextView titleView = new TextView(getContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					dip2px(tabWidth), LinearLayout.LayoutParams.FILL_PARENT);
			params.gravity = Gravity.CENTER;
			titleView.setGravity(Gravity.CENTER);
			titleView.setLayoutParams(params);
			titleView.setText(title);
			titleView.setTag(i);
			titleView.setOnClickListener(mOnTabClickListener);
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mNormalTextSize);
			titleView.setTextColor(mNormalTextColor);
			mTabContainerLayout.addView(titleView);
		}

		mHintView.setLayoutParams(new LayoutParams(dip2px(tabWidth),
				FrameLayout.LayoutParams.FILL_PARENT));

		if (mBackgroundResId != DEFAULT_RES_ID) {
			mBackgroundView.setBackgroundResource(mBackgroundResId);
		}

		if (mHintResId != DEFAULT_RES_ID) {
			mHintView.setBackgroundResource(mHintResId);
		}

		moveToWhich(DEFAULT_TAB, false);
	}

	/**
	 * 选择选项卡
	 * 
	 * @param position
	 */
	public void setSelect(int position) {
		synchronized (mMovingLock) {
			moveToWhich(Math.max(0, Math.min(position, mTabTitles.length - 1)),
					true);
		}
	}

	/**
	 * 选择Tab标签
	 * 
	 * @param which
	 */
	private void moveToWhich(int which, boolean byUser) {
		if (which == mCurrent) {
			return;
		} else if (which == DEFAULT_TAB) {
			which = 0;
		}
		synchronized (mMovingLock) {
			mMoving = true;
			Thread movingThread = new Thread(new MovingThread(which,
					new Handler(getContext().getMainLooper())));
			movingThread.start();
			if (!byUser) {
				mOnTabSelectedListener.onSelected(which);
			}
		}
	}

	/**
	 * tab被点击
	 */
	private OnClickListener mOnTabClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (!mMoving) {
				moveToWhich((Integer) v.getTag(), false);
			}
		}
	};

	/**
	 * 滑动动作同步线程
	 * 
	 * @author XuWei
	 * 
	 */
	private class MovingThread implements Runnable {

		/** Handler */
		private Handler mHandler;
		/** 滑动距离 */
		private int mDx;
		/** 距离步长 */
		private int mDistanceStep;
		/** 时间步长 */
		private int mTimeStep;

		/** 原Tab标签View */
		private TextView mOldView;
		/** 目标Tab标签View */
		private TextView mNewView;

		/** 目标Tab字号步长 */
		private float mNewTextSizeStep;
		/** 原Tab字号步长 */
		private float mOldTextSizeStep;
		/** 目标Tab字色步长 */
		private float[] mNewTextColorStep;
		/** 原Tab字色步长 */
		private float[] mOldTextColorStep;
		/** 滚动条滚动步长 */
		private int mScrollerStep;

		/**
		 * 构造
		 * 
		 * @param target
		 * @param handler
		 */
		public MovingThread(int target, Handler handler) {
			mHandler = handler;
			int left = mTabContainerLayout.getChildAt(target).getLeft();
			mDx = left - mHintView.getLeft();
			mDistanceStep = mDx / mFps;
			mTimeStep = mSlideTime / mFps;

			mOldView = (TextView) mTabContainerLayout.getChildAt(mCurrent);
			mNewView = (TextView) mTabContainerLayout.getChildAt(target);

			mOldTextSizeStep = (mNormalTextSize - px2dip(mOldView.getTextSize()))
					/ mFps;
			mNewTextSizeStep = (mActiveTextSize - px2dip(mNewView.getTextSize()))
					/ mFps;

			int oldColor = mOldView.getTextColors().getDefaultColor();
			int newColor = mNewView.getTextColors().getDefaultColor();

			mOldTextColorStep = new float[] {
					(Color.red(mNormalTextColor) - Color.red(oldColor)) / mFps,
					(Color.green(mNormalTextColor) - Color.green(oldColor))
							/ mFps,
					(Color.blue(mNormalTextColor) - Color.blue(oldColor))
							/ mFps };
			mNewTextColorStep = new float[] {
					(Color.red(mActiveTextColor) - Color.red(newColor)) / mFps,
					(Color.green(mActiveTextColor) - Color.green(newColor))
							/ mFps,
					(Color.blue(mActiveTextColor) - Color.blue(newColor))
							/ mFps };

			/*
			 * 若目标在左侧，且当前不完全处于屏幕可显示位置
			 */
			if (mNewView.getLeft() < mHorizontalScrollView.getScrollX()) {
				float scrollerStep = (mNewView.getLeft() - mHorizontalScrollView
						.getScrollX()) / (float) mFps;
				/*
				 * ScrollView的scrollBy(int,int)方法，其中只能设置int类型，则若在步长小于一的情况下，按照最大绝对值取整
				 */
				mScrollerStep = (int) Math.floor(scrollerStep);
			}

			/*
			 * 若目标在右侧，且当前不完全处于屏幕可显示位置
			 */
			if (mNewView.getRight() > mHorizontalScrollView.getScrollX()
					+ mWidth) {
				float scrollerStep = (mNewView.getRight()
						- mHorizontalScrollView.getScrollX() - mWidth)
						/ (float) mFps;
				mScrollerStep = (int) Math.ceil(scrollerStep);
			}

			mCurrent = target;
		}

		@Override
		public void run() {
			synchronized (mMovingLock) {
				Runnable updatingRunnable = new Runnable() {
					@Override
					public void run() {
						mHintView.layout(mHintView.getLeft() + mDistanceStep,
								mHintView.getTop(), mHintView.getRight()
										+ mDistanceStep, mHintView.getBottom());

						// TextView的getTextSize()方法返回值为px单位，需要除以像素密度获取sp或dip单位
						// TextView的setTextSize()方法设置的为sp单位字号.
						// setTextSize(unit, size)方法可指定单位类型
						// TypedValue.COMPLEX_UNIT_PX : Pixels
						// TypedValue.COMPLEX_UNIT_SP : Scaled Pixels
						// TypedValue.COMPLEX_UNIT_DIP : Device Independent
						// Pixels
						mOldView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
								px2dip(mOldView.getTextSize())
										+ mOldTextSizeStep);
						mNewView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
								px2dip(mNewView.getTextSize())
										+ mNewTextSizeStep);

						int oldColor = mOldView.getTextColors()
								.getDefaultColor();
						int newColor = mNewView.getTextColors()
								.getDefaultColor();
						mOldView.setTextColor(getColor(oldColor,
								mOldTextColorStep));
						mNewView.setTextColor(getColor(newColor,
								mNewTextColorStep));

						/*
						 * 由于是按最大绝对值对步长取整，故每次都需要判断是否已经满足位移条件
						 */
						if (mNewView.getLeft() < mHorizontalScrollView
								.getScrollX()
								|| mNewView.getRight() > mHorizontalScrollView
										.getScrollX() + mWidth) {
							mHorizontalScrollView.smoothScrollBy(mScrollerStep,
									0);
						}
					}
				};
				while (mMoving) {
					mHandler.post(updatingRunnable);
					int oldDx = mDx;
					mDx -= mDistanceStep;
					if (oldDx * mDx <= 0) {
						break;
					}
					try {
						Thread.sleep(mTimeStep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				/*
				 * post到handler的消息队列可能执行顺序要比本线程晚，所以需要remove掉callback避免错位
				 */
				mHandler.removeCallbacks(updatingRunnable);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						View targetView = mTabContainerLayout
								.getChildAt(mCurrent);
						mHintView.layout(targetView.getLeft(),
								targetView.getTop(), targetView.getRight(),
								targetView.getBottom());

						mOldView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
								mNormalTextSize);
						mNewView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
								mActiveTextSize);

						mOldView.setTextColor(mNormalTextColor);
						mNewView.setTextColor(mActiveTextColor);

						if (mNewView.getLeft() < mHorizontalScrollView
								.getScrollX()) {
							mHorizontalScrollView.smoothScrollTo(
									mNewView.getLeft(), 0);
						}

						if (mNewView.getRight() > mHorizontalScrollView
								.getScrollX() + mWidth) {
							mHorizontalScrollView.smoothScrollTo(
									mNewView.getRight() - mWidth, 0);
						}

						mMoving = false;
					}
				});
			}
		}

		/**
		 * 根据原有颜色和变化差值返回新的颜色
		 * 
		 * @param color
		 * @param deltaColor
		 * @return newColor
		 */
		private int getColor(int color, float[] deltaColor) {
			int newRed = (int) (Color.red(color) + deltaColor[0]);
			int newGreen = (int) (Color.green(color) + deltaColor[1]);
			int newBlue = (int) (Color.blue(color) + deltaColor[2]);

			return Color.rgb(Math.max(0, Math.min(255, newRed)),
					Math.max(0, Math.min(255, newGreen)),
					Math.max(0, Math.min(255, newBlue)));
		}
	}

	/**
	 * 设置滑块滑动时间
	 * 
	 * @param slideTime
	 */
	public void setSlideTime(int slideTime) {
		mSlideTime = slideTime;
	}

	/**
	 * 设置动画帧数
	 * 
	 * @param fps
	 */
	public void setFps(int fps) {
		mFps = fps;
	}

	/**
	 * 设置Tab栏背景。 注：调用在addTabsByTitles之前
	 * 
	 * @param resid
	 */
	public void setBackground(int resid) {
		mBackgroundResId = resid;
		if (mBackgroundView != null) {
			mBackgroundView.setBackgroundResource(resid);
		}
	}

	/**
	 * 设置滑块背景。 注：调用在addTabsByTitles之前
	 * 
	 * @param resid
	 */
	public void setHintBackground(int resid) {
		mHintResId = resid;
		if (mHintView != null) {
			mHintView.setBackgroundResource(resid);
		}
	}

	/**
	 * 设置标准态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setNormalTextColor(int r, int g, int b) {
		int color = Color.rgb(r, g, b);
		// 若未设置特殊的激活态字色，则将其一并修改
		if (mNormalTextColor == DEFAULI_TEXTCOLOR) {
			mActiveTextColor = color;
		}
		mNormalTextColor = color;
	}

	/**
	 * 设置标准态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param resId
	 */
	public void setNormalTextColor(int resId) {
		int color = getResources().getColor(resId);
		// 若未设置特殊的激活态字色，则将其一并修改
		if (mNormalTextColor == DEFAULI_TEXTCOLOR) {
			mActiveTextColor = color;
		}
		mNormalTextColor = color;
	}

	/**
	 * 设置激活态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setActiveTextColor(int r, int g, int b) {
		int color = Color.rgb(r, g, b);
		mActiveTextColor = color;
	}

	/**
	 * 设置激活态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param resId
	 */
	public void setActiveTextColor(int resId) {
		int color = getResources().getColor(resId);
		mActiveTextColor = color;
	}

	/**
	 * 设置标准态字号。 注：调用在addTabsByTitles之前
	 * 
	 * @param size
	 */
	public void setNormalTextSize(int size) {
		// 若未设置特殊的激活态字号，则将其一并修改
		if (mNormalTextSize == DEFAULT_TEXTSIZE) {
			mActiveTextSize = size;
		}
		mNormalTextSize = size;
	}

	/**
	 * 从Dimen中设置标准态字号。 注：调用在addTabsByTitles之前
	 * 
	 * @param size
	 */
	public void setNormalTextSizeFromDimen(int size) {
		setNormalTextSize((int) px2dip(size));
	}

	/**
	 * 提供对当前分辨率下dip px的换算
	 * 
	 * @param dip
	 * @return
	 */
	private int dip2px(int dip) {
		return (int) (dip * mDensity);
	}

	/**
	 * 提供对当前分辨率下px dip的换算
	 * 
	 * @param px
	 * @return
	 */
	private float px2dip(float px) {
		return (px / mDensity);
	}

	/**
	 * 设置激活态字号。 注：调用在addTabsByTitles之前，需先设置{@link #setNormalTextSize(int)}
	 * 
	 * @param size
	 */
	public void setActiveTextSize(int size) {
		mActiveTextSize = size;
	}

	/**
	 * 从Dimen中设置激活态字号。 注：调用在addTabsByTitles之前，需先设置
	 * {@link #setNormalTextSize(int)}
	 * 
	 * @param size
	 */
	public void setActiveTextSizeFromDimen(int size) {
		setActiveTextSize((int) px2dip(size));
	}

	/**
	 * Tab点击事件监听类
	 * 
	 * @author XuWei
	 * 
	 */
	public interface OnTabSelectedListener {
		/**
		 * 选择对应的标签时
		 * 
		 * @param which
		 */
		public void onSelected(int which);
	}
}
