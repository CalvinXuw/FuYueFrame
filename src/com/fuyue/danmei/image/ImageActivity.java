package com.fuyue.danmei.image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.fuyue.danmei.BaseActivity;
import com.fuyue.danmei.read.BookToast;
import com.fuyue.frame.R;
import com.fuyue.util.Utility;
import com.fuyue.util.imagecache.ImageResizer;

/**
 * 书架页面
 * 
 * @author Calvin
 * 
 */
public class ImageActivity extends BaseActivity {

	/** key image */
	private static final String KEY_IMAGE = "image";

	/**
	 * 获取intent
	 * 
	 * @param activity
	 * @param uri
	 * @return
	 */
	public static Intent getIntent(Activity activity, String uri) {
		Intent intent = new Intent(activity, ImageActivity.class);
		intent.putExtra(KEY_IMAGE, uri);
		return intent;
	}

	/** uri */
	private String mUri;
	/** image */
	private ImageView mImageView;

	/** 底部控制条 */
	private View mController;
	/** 返回按钮 */
	private View mBackBtn;
	/** 保存按钮 */
	private View mSaveBtn;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.image);
		mUri = getIntent().getExtras().getString(KEY_IMAGE);

		mImageView = (ImageView) findViewById(R.id.image_image);
		mController = findViewById(R.id.layout_image_control);
		mBackBtn = findViewById(R.id.btn_image_control_back);
		mSaveBtn = findViewById(R.id.btn_image_control_save);

		mBackBtn.setOnClickListener(mBackOnCancelListener);
		mSaveBtn.setOnClickListener(mSaveOnClickListener);
		mImageView.setOnClickListener(mDismissOnClickListener);

		try {
			Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open(mUri));
			bitmap = ImageResizer.resizeBitmap(bitmap,
					(int) Utility.getScreenWidth(this),
					(int) Utility.getScreenHeight(this));
			mImageView.setImageBitmap(bitmap);
		} catch (Exception e) {
		}
	}

	/**
	 * 显示控制器
	 */
	private void showController() {
		mController.setVisibility(View.VISIBLE);
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
				mController.postInvalidate();
			}
		});
		mController.startAnimation(displayAnimation);
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
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mController.clearAnimation();
				mController.setVisibility(View.GONE);
			}
		});
		mController.startAnimation(dismissAnimation);
	}

	/** 控制器消失点击监听 */
	private OnClickListener mDismissOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mController.getVisibility() == View.GONE) {
				showController();
			} else {
				hideController();
			}
		}
	};

	/** 返回 */
	private OnClickListener mBackOnCancelListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};

	/** 保存图片点击监听 */
	private OnClickListener mSaveOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			BookToast.showSaveToast(ImageActivity.this);
			try {
				Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open(
						mUri));
				MediaStore.Images.Media.insertImage(getContentResolver(),
						bitmap, "myPhoto", "");

				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
						Uri.parse("file://"
								+ Environment.getExternalStorageDirectory())));
			} catch (Exception e) {
			}
		}
	};
}
