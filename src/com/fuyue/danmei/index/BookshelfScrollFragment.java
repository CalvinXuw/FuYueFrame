package com.fuyue.danmei.index;

import java.io.Serializable;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fuyue.danmei.BaseFragment;
import com.fuyue.danmei.image.ImageActivity;
import com.fuyue.danmei.index.model.BookshelfModel.BookshelfBookItem;
import com.fuyue.danmei.index.model.BookshelfModel.BookshelfImageItem;
import com.fuyue.danmei.index.model.BookshelfModel.IBookshelfItem;
import com.fuyue.danmei.read.BookItem;
import com.fuyue.danmei.read.BookToast;
import com.fuyue.danmei.read.BooksReadActivity;
import com.fuyue.danmei.read.NativeBookItem;
import com.fuyue.danmei.read.db.BooksReadProgressDao;
import com.fuyue.danmei.read.db.BooksReadProgressDao.BooksReadProgressItem;
import com.fuyue.frame.R;
import com.fuyue.util.SdkVersionUtils;
import com.fuyue.util.Utility;
import com.fuyue.util.imagecache.ImageResizer;
import com.fuyue.util.model.AbstractModel;
import com.fuyue.util.ui.scrollpager.ScrollPage;
import com.fuyue.util.ui.scrollpager.ScrollPageController;
import com.fuyue.util.ui.scrollpager.ScrollPageController.ScrollPageType;
import com.fuyue.util.ui.scrollpager.ScrollPageView;

/**
 * 幻灯片形式书架
 * 
 * @author Calvin
 * 
 */
public class BookshelfScrollFragment extends BaseFragment {

	/** key bookshelf item */
	private static final String KEY_BOOKSHELF = "bookshelf";

	/**
	 * 获取实例
	 * 
	 * @param books
	 * @return
	 */
	public static BookshelfScrollFragment getInstance(List<IBookshelfItem> items) {
		BookshelfScrollFragment instance = new BookshelfScrollFragment();
		Bundle extra = new Bundle();
		extra.putSerializable(KEY_BOOKSHELF, (Serializable) items);
		instance.setArguments(extra);
		return instance;
	}

	public BookshelfScrollFragment() {
	}

	/** 书籍列表 */
	private List<IBookshelfItem> mBookshelfItems;
	/** 幻灯片controller */
	private ScrollPageController mScrollPageController;
	private ScrollPageView mScrollPageView;

	/** 底部图书信息 */
	private View mBookInfoContainer;
	/** 底部图片信息 */
	private View mImageInfoContainer;

	/** 书名 */
	private TextView mBookName;
	/** 图片 */
	private TextView mImageName;

	/** 阅读进度 */
	private ProgressBar mBookProgressBar;
	private TextView mBookProgress;

	@Override
	public void onResume() {
		mScrollPageController.onCurrentViewChanged(mScrollPageController
				.getCurrentPage());
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mScrollPageController != null) {
			mScrollPageController.destory();
		}
	}

	@Override
	public void onDestroyView() {
		if (mScrollPageController != null) {
			mScrollPageController.destory();
		}
		((ViewGroup) getView()).removeAllViews();
		super.onDestroyView();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mBookshelfItems = (List<IBookshelfItem>) getArguments()
				.getSerializable(KEY_BOOKSHELF);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.bookshelf_scroll, container,
				false);

		mBookInfoContainer = layout
				.findViewById(R.id.layout_bookshelf_bookinfo);
		mImageInfoContainer = layout
				.findViewById(R.id.layout_bookshelf_imageinfo);
		mBookName = (TextView) layout.findViewById(R.id.text_bookshelf_name);
		mImageName = (TextView) layout.findViewById(R.id.text_image_name);
		mBookProgressBar = (ProgressBar) layout
				.findViewById(R.id.progress_bookshelf);
		mBookProgress = (TextView) layout
				.findViewById(R.id.text_bookshelf_progress);

		final BookshelfPage[] pages = new BookshelfPage[mBookshelfItems.size()];
		for (int i = 0; i < mBookshelfItems.size(); i++) {
			pages[i] = new BookshelfPage(mBookshelfItems.get(i));
		}

		// 初始化幻灯片控件
		mScrollPageView = (ScrollPageView) layout.findViewById(R.id.scrollview);
		int height = (int) (Utility.getScreenWidth(getActivity()) - Utility
				.getDensity(getActivity()) * (60 + 60 + 15 + 15 - 3 * 9)) / 2 * 3;
		mScrollPageView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, height));
		mScrollPageController = new ScrollPageController(ScrollPageType.MARGIN,
				(int) (Utility.getDensity(getActivity()) * 60), pages) {

			@Override
			public void onUserScrollStart() {
			}

			@Override
			public void onUserScrollEnd() {
			}

			@Override
			public void onScroll(int currentView, float percent) {
			}

			@Override
			public void onInitSuccess() {
				mScrollPageView.startWithView(0);
				pages[0].onSelected();
			}

			@Override
			public void onCurrentViewChanged(int currentView) {
				pages[currentView].onSelected();
			}
		};
		mScrollPageController.setSpacing((int) Utility
				.getDensity(getActivity()) * 10);
		mScrollPageController.setBounces(false);
		mScrollPageController.setCircle(false);
		mScrollPageView.setScrollPageController(mScrollPageController);
		return layout;
	}

	/**
	 * 书架幻灯片页面
	 * 
	 * @author Calvin
	 * 
	 */
	private class BookshelfPage extends ScrollPage {

		/** bookshelf item */
		private IBookshelfItem mBookshelfItem;

		/** image uri */
		private String mImageUri;

		/** image */
		private ImageView mImageView;

		/**
		 * 构造
		 * 
		 * @param item
		 */
		public BookshelfPage(IBookshelfItem item) {
			mBookshelfItem = item;
			if (mBookshelfItem instanceof BookshelfBookItem) {
				mImageUri = ((BookshelfBookItem) mBookshelfItem).mBookThumbImage;
			} else if (mBookshelfItem instanceof BookshelfImageItem) {
				mImageUri = ((BookshelfImageItem) mBookshelfItem).mImageFilePath;
			}
		}

		/**
		 * 选中时
		 */
		public void onSelected() {
			if (mBookshelfItem instanceof BookshelfBookItem) {
				mBookInfoContainer.setVisibility(View.VISIBLE);
				mImageInfoContainer.setVisibility(View.GONE);

				BookItem bookItem = new NativeBookItem(getActivity(),
						(BookshelfBookItem) mBookshelfItem);
				mBookName.setText(bookItem.mBookName);
				BooksReadProgressItem progressItem = BooksReadProgressDao
						.getInstance(getActivity()).queryProgressByBookId(
								bookItem.mBookId);
				if (progressItem != null) {
					mBookProgress.setText("已读：" + progressItem.mProgressInBook
							+ "%");
					mBookProgressBar
							.setProgress((int) progressItem.mProgressInBook);
				} else {
					mBookProgress.setText("未读");
					mBookProgressBar.setProgress(0);
				}
			} else if (mBookshelfItem instanceof BookshelfImageItem) {
				mBookInfoContainer.setVisibility(View.GONE);
				mImageInfoContainer.setVisibility(View.VISIBLE);

				mImageName
						.setText(((BookshelfImageItem) mBookshelfItem).mImageName);
				mImageInfoContainer.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						BookToast.showSaveToast(getActivity());
						try {
							Bitmap bitmap = BitmapFactory
									.decodeStream(getActivity().getAssets()
											.open(mImageUri));
							MediaStore.Images.Media.insertImage(getActivity()
									.getContentResolver(), bitmap, "myPhoto",
									"");
							getActivity()
									.sendBroadcast(
											new Intent(
													Intent.ACTION_MEDIA_MOUNTED,
													Uri.parse("file://"
															+ Environment
																	.getExternalStorageDirectory())));
						} catch (Exception e) {
						}
					}
				});
			}
		}

		/**
		 * 设置图片
		 */
		private void setImage() {
			int height = (int) (Utility.getScreenWidth(getActivity()) - Utility
					.getDensity(getActivity()) * (60 + 60 + 15 + 15)) / 2 * 3;
			Bitmap bitmap = ImageResizer.decodeSampledBitmapFromAsset(
					getActivity(), mImageUri, height);
			bitmap = ImageResizer.getRoundedCornerBitmap(getActivity(), bitmap);
			mImageView.setImageBitmap(bitmap);
		}

		@Override
		public void onStart() {
			setImage();
		}

		@Override
		public void onResume() {
			setImage();
		}

		@Override
		public void onPause() {
			mImageView.setImageBitmap(null);
		}

		@Override
		public void onDestory() {
			mImageView.setImageBitmap(null);
		}

		@Override
		public void onCreate() {
			mImageView = new ImageView(getActivity());
			mImageView.setScaleType(ScaleType.FIT_XY);
			int padding = (int) (Utility.getDensity(getActivity()) * 6);
			mImageView.setPadding(padding, padding, padding, padding);
			mImageView.setBackgroundResource(R.drawable.bg_cover_shadow_large);
			mImageView.setLayoutParams(new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

			mImageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mBookshelfItem instanceof BookshelfBookItem) {
						getActivity()
								.startActivity(
										BooksReadActivity
												.getIntent(
														getActivity(),
														new NativeBookItem(
																getActivity(),
																(BookshelfBookItem) mBookshelfItem),
														0));
					} else if (mBookshelfItem instanceof BookshelfImageItem) {
						startActivity(ImageActivity.getIntent(getActivity(),
								mImageUri));
					}
				}
			});

			setView(mImageView);
		}

	}

	@Override
	public void onSuccess(AbstractModel model) {
	}

	@Override
	public void onFailed(AbstractModel model, int errorCode) {
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		if (SdkVersionUtils.hasFroyo()) {
			getActivity().overridePendingTransition(R.anim.image_anim_in,
					R.anim.image_anim_out);
		}
	}
}
