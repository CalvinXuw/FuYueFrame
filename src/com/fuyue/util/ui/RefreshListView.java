package com.fuyue.util.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fuyue.frame.R;

/**
 * 集成下拉刷新及滚动刷新的ListView，其中如需使用下拉刷新或滑动加载功能，请设置 {@link #setOnPullRefreshListener}
 * 或者 {@link #setOnScrollRefreshListener}
 * 
 * @author xuwei
 * 
 */
public class RefreshListView extends ListView implements OnScrollListener {

	/** Tag */
	private static final String TAG = "RefreshListView";

	/** 下拉刷新 */
	private static final String PULL_TO_REFRESH = "下拉刷新...";
	/** 松开刷新 */
	private static final String RELEASE_To_REFRESH = "释放刷新...";
	/** 正在刷新... */
	private static final String REFRESHING = "正在刷新...";
	/** 正在加载... */
	private static final String LOADING = "正在加载...";
	/** 数据读取失败，请重试 */
	private static final String REFRESHING_FAILED = "点击重新加载";
	/** 最近更新 : */
	private static final String RECENT_UPDATE = "最后更新: ";

	/**
	 * 下拉刷新状态
	 * 
	 * @see #RELEASE_To_REFRESH 释放后刷新
	 * @see #PULL_To_REFRESH 下拉后刷新
	 * @see #REFRESHING 正在刷新
	 * @see #DONE 正常状态
	 * 
	 * @author xuwei
	 * 
	 */
	private static enum PullRefreshState {
		RELEASE_To_REFRESH, PULL_To_REFRESH, REFRESHING, DONE
	}

	/**
	 * 滑动刷新状态
	 * 
	 * @see #REFRESHING 正在刷新
	 * @see #FAIL 加载失败
	 * @see #DONE 正常状态
	 * @see #NOMORE 无后续加载项
	 * 
	 * @author xuwei
	 * 
	 */
	private static enum ScrollRefreshState {
		REFRESHING, FAIL, DONE, NOMORE
	}

	/*
	 * 下拉刷新配置
	 */

	/**
	 * 实际手指滑动的距离与界面显示距离的偏移比，例如：手指画过300px距离，则只展示出100px的拉伸，橡皮效果。
	 */
	private final static int RATIO = 3;

	/** 下拉刷新的HeadView */
	private LinearLayout mPullRefreshHeadView;
	/** 下拉刷新的HeadView高度 */
	private int mPullRefreshHeadViewHeight;

	/** 下拉刷新的HeadView的状态提示 */
	private TextView mHeadTipsTextview;
	/** 下拉刷新的HeadView的更新时间 */
	private TextView mHeadLastUpdatedTextView;
	/** 下拉刷新的HeadView的箭头 */
	private ImageView mHeadArrowImageView;
	/** 下拉刷新的HeadView的等待图标 */
	private ProgressBar mHeadProgressBar;

	/** 箭头向下翻转动画 */
	private RotateAnimation mArrowAnimation;
	/** 箭头向上翻转动画 */
	private RotateAnimation mArrowReverseAnimation;

	/** 当前下拉刷新状态 */
	private PullRefreshState mCurrentPullRefreshState;
	/** 下拉刷新事件触发监听 */
	private OnPullRefreshListener mOnPullRefreshListener;
	/** 是否允许下拉刷新 */
	private boolean mIsPullRefreshable;

	/** 用于保证startY的值在一个完整的touch事件中只被记录一次 */
	private boolean isPullRefreshRecored;
	/** 起始Y坐标 */
	private int mStartY;
	/** 起始列表项索引 */
	private int mFirstItemIndex;
	/** 是否松起并返回正常态 */
	private boolean mIsBack;
	/** 上次下拉刷新时间 */
	private long mLastRefreshTime;

	/*
	 * 滑动刷新配置
	 */

	/** 滑动加载的FootView */
	private LinearLayout mScrollRefreshFootView;
	/** 滑动加载的FootView的高度 */
	private int mScrollRefreshFootViewHeight;

	/** 滑动加载的FootView的提示文字 */
	private TextView mFootTipsTextview;
	/** 滑动加载的FootView的提示图标 */
	private ImageView mFootWarningImageView;
	/** 滑动加载的FootView的等待图标 */
	private ProgressBar mFootProgressBar;

	/** 当前滑动加载的状态 */
	private ScrollRefreshState mCurrentScrollRefreshState;
	/** 滑动加载的事件触发监听 */
	private OnScrollRefreshListener mOnScrollRefreshListener;
	/** 是否允许滑动加载 */
	private boolean mIsScrollRefreshable;

	/** 当前回弹动画Runnable */
	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public RefreshListView(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 初始化下拉刷新和滑动加载的View
	 */
	private void init() {
		setCacheColorHint(Color.TRANSPARENT);
		LayoutInflater inflater = LayoutInflater.from(getContext());

		mPullRefreshHeadView = (LinearLayout) inflater.inflate(
				R.layout.common_list_refresh_head, null);
		mScrollRefreshFootView = (LinearLayout) inflater.inflate(
				R.layout.common_list_refresh_foot, null);

		mHeadArrowImageView = (ImageView) mPullRefreshHeadView
				.findViewById(R.id.head_arrow);
		mHeadProgressBar = (ProgressBar) mPullRefreshHeadView
				.findViewById(R.id.head_progressBar);
		mHeadTipsTextview = (TextView) mPullRefreshHeadView
				.findViewById(R.id.head_tips);
		mHeadLastUpdatedTextView = (TextView) mPullRefreshHeadView
				.findViewById(R.id.head_lastupdate);

		mFootWarningImageView = (ImageView) mScrollRefreshFootView
				.findViewById(R.id.foot_warning);
		mFootProgressBar = (ProgressBar) mScrollRefreshFootView
				.findViewById(R.id.foot_progressBar);
		mFootTipsTextview = (TextView) mScrollRefreshFootView
				.findViewById(R.id.foot_tips);

		measureView(mPullRefreshHeadView);
		measureView(mScrollRefreshFootView);
		mPullRefreshHeadViewHeight = mPullRefreshHeadView.getMeasuredHeight();
		mScrollRefreshFootViewHeight = mScrollRefreshFootView
				.getMeasuredHeight();

		mPullRefreshHeadView.setPadding(0, -mPullRefreshHeadViewHeight, 0, 0);
		mScrollRefreshFootView.setPadding(0, -mScrollRefreshFootViewHeight, 0,
				0);
		mPullRefreshHeadView.invalidate();
		mScrollRefreshFootView.invalidate();

		addHeaderView(mPullRefreshHeadView, null, false);
		addFooterView(mScrollRefreshFootView, null, false);
		setOnScrollListener(this);

		mArrowAnimation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mArrowAnimation.setInterpolator(new LinearInterpolator());
		mArrowAnimation.setDuration(250);
		mArrowAnimation.setFillAfter(true);

		mArrowReverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mArrowReverseAnimation.setInterpolator(new LinearInterpolator());
		mArrowReverseAnimation.setDuration(200);
		mArrowReverseAnimation.setFillAfter(true);

		mCurrentPullRefreshState = PullRefreshState.DONE;
		mCurrentScrollRefreshState = ScrollRefreshState.DONE;
		mIsPullRefreshable = false;
	}

	/**
	 * 监听滑动事件，记录当前第一条可见列表项的索引值，捕捉页面滚动至最下方的事件
	 */
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mFirstItemIndex = firstVisibleItem;
		/*
		 * 若允许滑动加载，且当前状态在常态下，且滑至页面底部，并加入额外判断
		 */
		if (mIsScrollRefreshable
				&& mCurrentScrollRefreshState == ScrollRefreshState.DONE
				&& firstVisibleItem + visibleItemCount == totalItemCount
				&& canAccessScrollToRefresh()) {
			mCurrentScrollRefreshState = ScrollRefreshState.REFRESHING;
			changeFooterViewByState();
			onScrollRefresh();
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mExtraOnScrollListener != null) {
			mExtraOnScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	/** 设定额外的OnScrollListener */
	private OnScrollListener mExtraOnScrollListener;

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		super.setOnScrollListener(this);
		if (l != this) {
			mExtraOnScrollListener = l;
		}
	}

	/**
	 * 
	 */
	public boolean onTouchEvent(final MotionEvent event) {

		if (mIsPullRefreshable) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				countLastRefreshHintText();
				if (mFirstItemIndex == 0 && !isPullRefreshRecored) {
					isPullRefreshRecored = true;
					mStartY = (int) event.getY()
							- RATIO
							* (mPullRefreshHeadViewHeight + mPullRefreshHeadView
									.getPaddingTop());
					Log.i(TAG, "Down Recored");
				}
				break;

			case MotionEvent.ACTION_UP:
				if (mCurrentPullRefreshState != PullRefreshState.REFRESHING) {
					if (mCurrentPullRefreshState == PullRefreshState.DONE) {
						// 什么都不做
					}
					if (mCurrentPullRefreshState == PullRefreshState.PULL_To_REFRESH) {
						mCurrentPullRefreshState = PullRefreshState.DONE;
						changeHeaderViewByState();
						Log.i(TAG, "Change from PULL_To_REFRESH to DONE");
					}
					if (mCurrentPullRefreshState == PullRefreshState.RELEASE_To_REFRESH) {
						mCurrentPullRefreshState = PullRefreshState.REFRESHING;
						changeHeaderViewByState();
						onPullRefresh();
						Log.i(TAG,
								"Change from RELEASE_To_REFRESH to REFRESHING");
					}
				}

				isPullRefreshRecored = false;
				mIsBack = false;

				break;

			case MotionEvent.ACTION_MOVE:
				if (mCurrentSmoothScrollRunnable != null) {
					mCurrentSmoothScrollRunnable.stop();
				}

				int tempY = (int) event.getY();

				if (!isPullRefreshRecored && mFirstItemIndex == 0) {
					isPullRefreshRecored = true;
					mStartY = tempY;
					countLastRefreshHintText();
					Log.i(TAG, "Move Recored");
				}

				if (mCurrentPullRefreshState != PullRefreshState.REFRESHING
						&& isPullRefreshRecored) {

					// 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动

					// 可以松手去刷新了
					if (mCurrentPullRefreshState == PullRefreshState.RELEASE_To_REFRESH) {

						setSelection(0);

						// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
						if (((tempY - mStartY) / RATIO < mPullRefreshHeadViewHeight)
								&& (tempY - mStartY) > 0) {
							mCurrentPullRefreshState = PullRefreshState.PULL_To_REFRESH;
							changeHeaderViewByState();
							Log.i(TAG,
									"Change from RELEASE_To_REFRESH to PULL_To_REFRESH");
						}
						// 一下子推到顶了
						else if (tempY - mStartY <= 0) {
							mCurrentPullRefreshState = PullRefreshState.DONE;
							changeHeaderViewByState();
							Log.i(TAG, "Change from RELEASE_To_REFRESH to DONE");
						}
						// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
						else {
							// 不用进行特别的操作，只用更新paddingTop的值就行了
						}
					}
					// 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
					if (mCurrentPullRefreshState == PullRefreshState.PULL_To_REFRESH) {

						setSelection(0);

						// 下拉到可以进入RELEASE_TO_REFRESH的状态，并增加额外条件判断canAccessPullToRefresh()
						if ((tempY - mStartY) / RATIO >= mPullRefreshHeadViewHeight
								&& canAccessPullToRefresh()) {
							mCurrentPullRefreshState = PullRefreshState.RELEASE_To_REFRESH;
							mIsBack = true;
							changeHeaderViewByState();
							Log.i(TAG,
									"Change from PULL_To_REFRESH to RELEASE_To_REFRESH");
						}
						// 上推到顶了
						else if (tempY - mStartY <= 0) {
							mCurrentPullRefreshState = PullRefreshState.DONE;
							changeHeaderViewByState();
							Log.i(TAG, "Change from PULL_To_REFRESH to DONE");
						}
					}

					// done状态下
					if (mCurrentPullRefreshState == PullRefreshState.DONE) {
						if (tempY - mStartY > 0) {
							mCurrentPullRefreshState = PullRefreshState.PULL_To_REFRESH;
							changeHeaderViewByState();
							Log.i(TAG, "Change from DONE to PULL_To_REFRESH");
						}
					}

					// 更新headView的size
					if (mCurrentPullRefreshState == PullRefreshState.PULL_To_REFRESH) {
						mPullRefreshHeadView.setPadding(0, -1
								* mPullRefreshHeadViewHeight
								+ (tempY - mStartY) / RATIO, 0, 0);
					}

					// 更新headView的paddingTop
					if (mCurrentPullRefreshState == PullRefreshState.RELEASE_To_REFRESH) {
						mPullRefreshHeadView.setPadding(0, (tempY - mStartY)
								/ RATIO - mPullRefreshHeadViewHeight, 0, 0);
					}

				}

				break;
			}
		}

		return super.onTouchEvent(event);
	}

	/**
	 * 偏移指定距离创建补间动画
	 * 
	 * @param offSet
	 */
	private void smoothScrollByOffsetY(final int offSet) {
		final Handler updatePadding = new Handler();
		if (null != mCurrentSmoothScrollRunnable)
			mCurrentSmoothScrollRunnable.stop();

		this.mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(
				updatePadding, offSet);
		updatePadding.post(mCurrentSmoothScrollRunnable);
	}

	/**
	 * 是否可以进行下拉刷新的附加条件判断，例如：当前正在进行滑动加载时及返回false。
	 * 
	 * @return
	 */
	private boolean canAccessPullToRefresh() {
		if (mCurrentScrollRefreshState == ScrollRefreshState.REFRESHING) {
			return false;
		}
		return true;
	}

	/**
	 * 是否可以进行滑动加载的附加条件判断，例如：当前正在进行下拉刷新时及返回false。
	 * 
	 * @return
	 */
	private boolean canAccessScrollToRefresh() {
		if (mCurrentPullRefreshState == PullRefreshState.REFRESHING) {
			return false;
		}
		return true;
	}

	/**
	 * 补间动画Runnable
	 * 
	 * @author xuwei
	 * 
	 */
	final class SmoothScrollRunnable implements Runnable {

		/** 持续时间 */
		static final int ANIMATION_DURATION_MS = 250;
		/** 帧数 */
		static final int ANIMATION_FPS = 10;

		/** 实现动画的变化率 */
		private final Interpolator mInterpolator;
		/** handler */
		private final Handler mHandler;
		/** 距离差 */
		private int mOffSet;
		/** 是佛继续执行 */
		private boolean mContinueRunning = true;
		/** 动画起始时间，用以计算修正 持续时间 */
		private long mStartTime = -1;

		/**
		 * 构造
		 * 
		 * @param handler
		 * @param offSet
		 */
		public SmoothScrollRunnable(Handler handler, int offSet) {
			mHandler = handler;
			mInterpolator = new AccelerateDecelerateInterpolator();
			mOffSet = offSet;
		}

		@Override
		public void run() {

			/**
			 * Only set startTime if this is the first time we're starting, else
			 * actually calculate the Y delta
			 */
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {

				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime))
						/ ANIMATION_DURATION_MS;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mOffSet - mPullRefreshHeadView
						.getPaddingTop())
						* mInterpolator
								.getInterpolation(normalizedTime / 1000f));
				if (mPullRefreshHeadView.getPaddingTop() != mOffSet)
					mPullRefreshHeadView.setPadding(0, deltaY
							+ mPullRefreshHeadView.getPaddingTop(), 0, 0);
			}

			// If we're not at the target Y, keep going...
			if (mContinueRunning
					&& mPullRefreshHeadView.getPaddingTop() != mOffSet) {
				mHandler.postDelayed(this, ANIMATION_FPS);
			}
		}

		/**
		 * 停止
		 */
		public void stop() {
			mContinueRunning = false;
			mHandler.removeCallbacks(this);
		}
	};

	/**
	 * 当状态改变时候，调用该方法，以更新下拉刷新界面
	 */
	private void changeHeaderViewByState() {
		switch (mCurrentPullRefreshState) {
		case RELEASE_To_REFRESH:
			mHeadArrowImageView.setVisibility(View.VISIBLE);
			mHeadProgressBar.setVisibility(View.GONE);
			mHeadTipsTextview.setVisibility(View.VISIBLE);
			mHeadLastUpdatedTextView.setVisibility(View.VISIBLE);

			mHeadArrowImageView.clearAnimation();
			mHeadArrowImageView.startAnimation(mArrowAnimation);

			mHeadTipsTextview.setText(RELEASE_To_REFRESH);

			Log.i(TAG, "Set RELEASE_To_REFRESH");
			break;
		case PULL_To_REFRESH:
			mHeadProgressBar.setVisibility(View.GONE);
			mHeadTipsTextview.setVisibility(View.VISIBLE);
			mHeadLastUpdatedTextView.setVisibility(View.VISIBLE);
			mHeadArrowImageView.clearAnimation();
			mHeadArrowImageView.setVisibility(View.VISIBLE);
			// 是由RELEASE_To_REFRESH状态转变来的
			if (mIsBack) {
				mIsBack = false;
				mHeadArrowImageView.clearAnimation();
				mHeadArrowImageView.startAnimation(mArrowReverseAnimation);

				mHeadTipsTextview.setText(PULL_TO_REFRESH);
			} else {
				mHeadTipsTextview.setText(PULL_TO_REFRESH);
			}
			Log.i(TAG, "Set PULL_To_REFRESH");
			break;

		case REFRESHING:
			smoothScrollByOffsetY(0);

			mHeadProgressBar.setVisibility(View.VISIBLE);
			mHeadArrowImageView.clearAnimation();
			mHeadArrowImageView.setVisibility(View.GONE);
			mHeadTipsTextview.setText(REFRESHING);
			mHeadLastUpdatedTextView.setVisibility(View.VISIBLE);

			Log.i(TAG, "Set REFRESHING");
			break;
		case DONE:
			smoothScrollByOffsetY(-mPullRefreshHeadViewHeight);

			mHeadProgressBar.setVisibility(View.GONE);
			mHeadArrowImageView.clearAnimation();
			mHeadArrowImageView
					.setImageResource(R.drawable.image_head_pullrefresh);
			mHeadArrowImageView.setVisibility(View.VISIBLE);
			mHeadTipsTextview.setText(PULL_TO_REFRESH);
			mHeadLastUpdatedTextView.setVisibility(View.VISIBLE);
			Log.i(TAG, "Set DONE");
			break;
		}
	}

	/**
	 * 当状态改变时候，调用该方法，以更新滑动加载界面
	 */
	private void changeFooterViewByState() {
		switch (mCurrentScrollRefreshState) {
		case REFRESHING:
			mFootProgressBar.setVisibility(View.VISIBLE);
			mFootWarningImageView.setVisibility(View.GONE);
			mFootTipsTextview.setText(LOADING);
			mScrollRefreshFootView.setOnClickListener(null);
			break;
		case DONE:
			mScrollRefreshFootView.setPadding(0, 0, 0, 0);
			mFootProgressBar.setVisibility(View.VISIBLE);
			mFootWarningImageView.setVisibility(View.GONE);
			mFootTipsTextview.setText(LOADING);
			mScrollRefreshFootView.setOnClickListener(null);
			break;
		case FAIL:
			mFootProgressBar.setVisibility(View.GONE);
			mFootWarningImageView.setVisibility(View.VISIBLE);
			mFootTipsTextview.setText(REFRESHING_FAILED);
			mScrollRefreshFootView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mCurrentScrollRefreshState = ScrollRefreshState.REFRESHING;
					changeFooterViewByState();
					onScrollRefresh();
				}
			});
			break;
		case NOMORE:
			mScrollRefreshFootView.setPadding(0, -mScrollRefreshFootViewHeight,
					0, 0);
			break;
		}
	}

	/**
	 * 进行一次下拉刷新
	 */
	public void makePullRefresh() {
		if (mCurrentPullRefreshState != PullRefreshState.DONE) {
			return;
		}

		countLastRefreshHintText();
		mPullRefreshHeadView.setPadding(0, 0, 0, 0);
		mCurrentPullRefreshState = PullRefreshState.REFRESHING;
		changeHeaderViewByState();
		setSelection(0);
		scrollTo(0, 0);
		mOnPullRefreshListener.onPullRefresh();
	}

	/**
	 * 添加下拉刷新事件监听
	 * 
	 * @param mOnPullRefreshListener
	 */
	public void setOnPullRefreshListener(
			OnPullRefreshListener mOnPullRefreshListener) {
		this.mOnPullRefreshListener = mOnPullRefreshListener;
		mIsPullRefreshable = true;
	}

	/**
	 * 添加滑动加载事件监听
	 * 
	 * @param mOnScrollRefreshListener
	 */
	public void setOnScrollRefreshListener(
			OnScrollRefreshListener mOnScrollRefreshListener) {
		this.mOnScrollRefreshListener = mOnScrollRefreshListener;
		mIsScrollRefreshable = true;
		mScrollRefreshFootView.setPadding(0, 0, 0, 0);
	}

	/**
	 * 通知ListView下拉刷新完成
	 */
	public void onPullRefreshComplete(boolean reset) {
		mCurrentPullRefreshState = PullRefreshState.DONE;
		mLastRefreshTime = System.currentTimeMillis();
		changeHeaderViewByState();
		invalidateViews();
		setSelection(0);

		if (reset) {
			onScrollRefreshComplete();
		}
	}

	/**
	 * 计算上次更新时间的提示文字
	 */
	private void countLastRefreshHintText() {
		long dTime = System.currentTimeMillis() - mLastRefreshTime;
		// 15分钟
		if (dTime < 15 * 60 * 1000) {
			mHeadLastUpdatedTextView.setText(RECENT_UPDATE + "刚刚");
		} else if (dTime < 60 * 60 * 1000) {
			// 一小时
			mHeadLastUpdatedTextView.setText(RECENT_UPDATE + "一小时内");
		} else if (dTime < 24 * 60 * 60 * 1000) {
			mHeadLastUpdatedTextView.setText(RECENT_UPDATE
					+ (int) (dTime / (60 * 60 * 1000)) + "小时前");
		} else {
			mHeadLastUpdatedTextView.setText(RECENT_UPDATE
					+ DateFormat.format("MM-dd kk:mm",
							System.currentTimeMillis()).toString());
		}
	}

	/**
	 * 通知ListView下拉刷新完成
	 */
	public void onScrollRefreshComplete() {
		mCurrentScrollRefreshState = ScrollRefreshState.DONE;
		changeFooterViewByState();
		invalidateViews();
	}

	/**
	 * 通知ListView下滑动加载失败
	 */
	public void onScrollRefreshFail() {
		mCurrentScrollRefreshState = ScrollRefreshState.FAIL;
		changeFooterViewByState();
		invalidateViews();
	}

	/**
	 * 通知ListView下滑动加载无后续可加载项
	 */
	public void onScrollRefreshNoMore() {
		mCurrentScrollRefreshState = ScrollRefreshState.NOMORE;
		changeFooterViewByState();
		invalidateViews();
	}

	/**
	 * 获取当前ListView状态
	 * 
	 * @return
	 */
	public RefreshListViewState getCurrentState() {
		if (mCurrentPullRefreshState == PullRefreshState.REFRESHING) {
			return RefreshListViewState.WAITING_PULLREFRESH_RESULT;
		} else if (mCurrentScrollRefreshState == ScrollRefreshState.REFRESHING) {
			return RefreshListViewState.WAITING_SCROLLREFRESH_RESULT;
		}
		return RefreshListViewState.NORMAL;
	}

	/**
	 * 通知下拉刷新
	 */
	private void onPullRefresh() {
		if (mOnPullRefreshListener != null) {
			mOnPullRefreshListener.onPullRefresh();
		}
	}

	/**
	 * 通知滑动加载
	 */
	private void onScrollRefresh() {
		if (mOnScrollRefreshListener != null) {
			mOnScrollRefreshListener.onScrollRefresh();
		}
	}

	/**
	 * 计算headView的width以及height
	 * 
	 * @param child
	 */
	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	/**
	 * 初始化更新时间
	 * 
	 * @param adapter
	 */
	public void setAdapter(BaseAdapter adapter) {
		mLastRefreshTime = System.currentTimeMillis();
		super.setAdapter(adapter);
	}

	/**
	 * 下拉刷新事件监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnPullRefreshListener {
		public void onPullRefresh();
	}

	/**
	 * 滑动加载事件监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnScrollRefreshListener {
		public void onScrollRefresh();
	}

	/**
	 * 获取当前列表状态
	 * 
	 * @author Calvin
	 * 
	 */
	public enum RefreshListViewState {
		WAITING_PULLREFRESH_RESULT, WAITING_SCROLLREFRESH_RESULT, NORMAL
	}
}
