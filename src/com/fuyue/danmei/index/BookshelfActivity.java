package com.fuyue.danmei.index;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fuyue.BaseApplicaion;
import com.fuyue.danmei.BaseActivity;
import com.fuyue.danmei.config.ClientInfoConfig;
import com.fuyue.danmei.index.model.BookshelfModel;
import com.fuyue.danmei.index.model.BookshelfModel.IBookshelfItem;
import com.fuyue.danmei.update.ClientUpdateRequertor;
import com.fuyue.frame.R;
import com.fuyue.util.Utility;
import com.fuyue.util.model.AbstractModel;
import com.kyview.interstitial.AdInstlManager;
import com.kyview.interstitial.AdInstlTargeting;
import com.umeng.analytics.MobclickAgent;

/**
 * 书架页面
 * 
 * @author Calvin
 * 
 */
public class BookshelfActivity extends BaseActivity {

	/** 配置文件路径 */
	private static final String MANIFEST = "manifest.xml";

	/** layout */
	private FrameLayout mLayout;
	/** 闪屏view */
	private View mSplashView;
	/** 书架view */
	private View mBookshelfView;

	/** 书架model */
	private BookshelfModel mBookshelfModel;

	/** 列表形式书架 */
	private BookshelfListFragment mBookshelfListFragment;
	/** 幻灯片形式书架 */
	private BookshelfScrollFragment mBookshelfScrollFragment;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		mLayout = new FrameLayout(this);
		setContentView(mLayout);

		mSplashView = getLayoutInflater().inflate(R.layout.splash, null);
		mBookshelfView = getLayoutInflater().inflate(R.layout.bookshelf, null);
		mLayout.addView(mBookshelfView);
		mLayout.addView(mSplashView);

		initSplash();
		initBookshelf();
	}

	/**
	 * 初始化闪屏view
	 */
	private void initSplash() {
		TextView product = (TextView) mSplashView
				.findViewById(R.id.text_splash_product_name);
		TextView copyright = (TextView) mSplashView
				.findViewById(R.id.text_splash_copyright);
		ImageView logo = (ImageView) mSplashView
				.findViewById(R.id.image_splash_logo);

		product.setText(BaseApplicaion.sProductName);
		copyright.setText(BaseApplicaion.sCopyright);
		try {
			logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open(
					"splash_logo.png")));
			mSplashView.setBackgroundDrawable(new BitmapDrawable(BitmapFactory
					.decodeStream(getAssets().open("splash_bg.png"))));
		} catch (IOException e) {
		}

		final Animation dismissAnimation = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out);
		dismissAnimation.setFillAfter(true);
		dismissAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mLayout.removeView(mSplashView);
				mSplashView.setBackgroundDrawable(null);
				System.gc();

				AdInstlTargeting.setAdWidthHeight(
						(int) Utility.getScreenWidth(BookshelfActivity.this),
						(int) Utility.getScreenHeight(BookshelfActivity.this));
				AdInstlManager adInstlManager = new AdInstlManager(
						BookshelfActivity.this, Utility
								.getApplicationStringMetaData(
										BookshelfActivity.this,
										"ADVIEW_SDK_KEY"));
				adInstlManager.requestad();
			}
		});

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				mSplashView.startAnimation(dismissAnimation);
				initCheckUpdate();
			}
		}, 2000);
	}

	/**
	 * 检查更新
	 */
	private void initCheckUpdate() {
		ClientUpdateRequertor updateRequertor = new ClientUpdateRequertor(this,
				this);
		updateRequertor.checkUpdate();
		mModelManageQueue.addTaskModel("update", updateRequertor);
	}

	@Override
	public void onSuccess(AbstractModel model) {
		((ClientUpdateRequertor) mModelManageQueue.getTaskModel("update"))
				.showUpdateDialog();
	}

	/**
	 * 初始化书架view
	 */
	private void initBookshelf() {
		mBookshelfModel = new BookshelfModel();
		try {
			mBookshelfModel.parseData(Utility.getStringFromInput(getAssets()
					.open(MANIFEST)));

			List<IBookshelfItem> bookshelfItems = new LinkedList<IBookshelfItem>();
			if (mBookshelfModel.mBookItems != null) {
				bookshelfItems.addAll(mBookshelfModel.mBookItems);
			}
			if (mBookshelfModel.mImageItems != null) {
				bookshelfItems.addAll(mBookshelfModel.mImageItems);
			}

			mBookshelfListFragment = BookshelfListFragment
					.getInstance(bookshelfItems);
			mBookshelfScrollFragment = BookshelfScrollFragment
					.getInstance(bookshelfItems);
		} catch (IOException e) {
		}

		findViewById(R.id.btn_bookshelf_switch).setOnClickListener(
				mSwitchOnClickListener);

		Fragment bookshelf = null;
		if (ClientInfoConfig.getInstance(this).isListBookshelf()) {
			bookshelf = mBookshelfListFragment;
			((ImageView) findViewById(R.id.btn_bookshelf_switch))
					.setImageResource(R.drawable.btn_bookshelf_scroll);
		} else {
			bookshelf = mBookshelfScrollFragment;
			((ImageView) findViewById(R.id.btn_bookshelf_switch))
					.setImageResource(R.drawable.btn_bookshelf_list);
		}

		Fragment current = getSupportFragmentManager().findFragmentById(
				R.id.layout_content);
		if (current == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.layout_content, bookshelf).commit();
		} else {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.layout_content, bookshelf).commit();
		}
	}

	/** 页面切换按钮 */
	private OnClickListener mSwitchOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Fragment current = getSupportFragmentManager().findFragmentById(
					R.id.layout_content);
			if (current == mBookshelfListFragment) {
				getSupportFragmentManager().beginTransaction()
						.replace(R.id.layout_content, mBookshelfScrollFragment)
						.commit();
				((ImageView) v).setImageResource(R.drawable.btn_bookshelf_list);

				ClientInfoConfig.getInstance(getApplicationContext())
						.setIsListBookshelf(false);
			} else if (current == mBookshelfScrollFragment) {
				getSupportFragmentManager().beginTransaction()
						.replace(R.id.layout_content, mBookshelfListFragment)
						.commit();
				((ImageView) v)
						.setImageResource(R.drawable.btn_bookshelf_scroll);

				ClientInfoConfig.getInstance(getApplicationContext())
						.setIsListBookshelf(true);
			}
		}
	};

	/** 上一次点击返回键的时间记录 */
	private long mLastBackKeyDown;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 如果卡片处于展开状态，那么点击返回键时，卡片收起
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			// 连续点击退出客户端
			long now = System.currentTimeMillis();
			if (now - mLastBackKeyDown < 3000) {
				finish();
				return true;
			} else {
				mLastBackKeyDown = now;
				Toast.makeText(
						this,
						getString(R.string.exit_hint)
								+ Utility.getAppName(this), Toast.LENGTH_SHORT)
						.show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
}
