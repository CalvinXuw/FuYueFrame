package com.fuyue.danmei.index;

import java.io.Serializable;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.fuyue.danmei.BaseFragment;
import com.fuyue.danmei.image.ImageActivity;
import com.fuyue.danmei.index.model.BookshelfModel.BookshelfBookItem;
import com.fuyue.danmei.index.model.BookshelfModel.BookshelfImageItem;
import com.fuyue.danmei.index.model.BookshelfModel.IBookshelfItem;
import com.fuyue.danmei.read.BookItem;
import com.fuyue.danmei.read.BooksReadActivity;
import com.fuyue.danmei.read.NativeBookItem;
import com.fuyue.danmei.read.db.BooksReadProgressDao;
import com.fuyue.danmei.read.db.BooksReadProgressDao.BooksReadProgressItem;
import com.fuyue.frame.R;
import com.fuyue.util.Utility;
import com.fuyue.util.imagecache.ImageResizer;
import com.fuyue.util.model.AbstractModel;

/**
 * 列表形式书架
 * 
 * @author Calvin
 * 
 */
public class BookshelfListFragment extends BaseFragment {

	/** key bookshelf item */
	private static final String KEY_BOOKSHELF = "bookshelf";

	/**
	 * 获取实例
	 * 
	 * @param books
	 * @return
	 */
	public static BookshelfListFragment getInstance(List<IBookshelfItem> items) {
		BookshelfListFragment instance = new BookshelfListFragment();
		Bundle extra = new Bundle();
		extra.putSerializable(KEY_BOOKSHELF, (Serializable) items);
		instance.setArguments(extra);
		return instance;
	}

	public BookshelfListFragment() {
	}

	/** 书籍列表 */
	private List<IBookshelfItem> mBookshelfItems;
	/** adapter */
	private BookshelfAdapter mBookshelfAdapter;

	@Override
	public void onResume() {
		if (mBookshelfAdapter != null) {
			mBookshelfAdapter.notifyDataSetChanged();
		}
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		((ViewGroup) getView()).removeAllViews();
		super.onDestroyView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mBookshelfItems = (List<IBookshelfItem>) getArguments()
				.getSerializable(KEY_BOOKSHELF);
		View layout = inflater.inflate(R.layout.bookshelf_list, container,
				false);
		ListView bookshelfListView = (ListView) layout
				.findViewById(R.id.list_refresh);
		mBookshelfAdapter = new BookshelfAdapter();
		bookshelfListView.setAdapter(mBookshelfAdapter);

		return layout;
	}

	/**
	 * 书架adapter
	 * 
	 * @author Calvin
	 * 
	 */
	private class BookshelfAdapter extends BaseAdapter {

		/** 列数 */
		private static final int COLUMNCOUNT = 3;

		private int mImageHeight;

		@Override
		public int getCount() {
			return (int) Math.ceil((float) mBookshelfItems.size() / 3);
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BookItemHolder holder = null;
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						R.layout.bookshelf_list_item, null);
				holder = new BookItemHolder();

				holder.mLeftContainer = convertView
						.findViewById(R.id.layout_item_left);
				holder.mMiddleContainer = convertView
						.findViewById(R.id.layout_item_middle);
				holder.mRightContainer = convertView
						.findViewById(R.id.layout_item_right);

				holder.mLeftCover = (ImageView) convertView
						.findViewById(R.id.image_bookshelf_left);
				holder.mMiddleCover = (ImageView) convertView
						.findViewById(R.id.image_bookshelf_middle);
				holder.mRightCover = (ImageView) convertView
						.findViewById(R.id.image_bookshelf_right);

				holder.mLeftName = (TextView) convertView
						.findViewById(R.id.text_bookshelf_name_left);
				holder.mMiddleName = (TextView) convertView
						.findViewById(R.id.text_bookshelf_name_middle);
				holder.mRightName = (TextView) convertView
						.findViewById(R.id.text_bookshelf_name_right);

				holder.mLeftProgress = (TextView) convertView
						.findViewById(R.id.text_bookshelf_progress_left);
				holder.mMiddleProgress = (TextView) convertView
						.findViewById(R.id.text_bookshelf_progress_middle);
				holder.mRightProgress = (TextView) convertView
						.findViewById(R.id.text_bookshelf_progress_right);

				mImageHeight = (int) ((Utility.getScreenWidth(getActivity()) - (10 + 10 + 10 + 10 + 1 * 6)
						* Utility.getDensity(getActivity())) / 3 / 2 * 3);
				LinearLayout.LayoutParams params = new LayoutParams(
						LayoutParams.FILL_PARENT, mImageHeight);
				holder.mLeftCover.setLayoutParams(params);
				holder.mMiddleCover.setLayoutParams(params);
				holder.mRightCover.setLayoutParams(params);

				convertView.setTag(holder);
				convertView.setOnClickListener(null);
			} else {
				holder = (BookItemHolder) convertView.getTag();
			}

			int positionLeft = position * COLUMNCOUNT;
			int positionMiddle = position * COLUMNCOUNT + 1;
			int positionRight = position * COLUMNCOUNT + 2;

			setSubItemView(holder.mLeftContainer, holder.mLeftCover,
					holder.mLeftName, holder.mLeftProgress, positionLeft);

			setSubItemView(holder.mMiddleContainer, holder.mMiddleCover,
					holder.mMiddleName, holder.mMiddleProgress, positionMiddle);

			setSubItemView(holder.mRightContainer, holder.mRightCover,
					holder.mRightName, holder.mRightProgress, positionRight);

			return convertView;
		}

		/**
		 * 设置子视图
		 * 
		 * @param container
		 * @param cover
		 * @param name
		 * @param progress
		 * @param item
		 */
		private void setSubItemView(View container, ImageView cover,
				TextView name, TextView progress, int position) {
			if (position < mBookshelfItems.size()) {
				container.setVisibility(View.VISIBLE);

				IBookshelfItem bookshelfItem = mBookshelfItems.get(position);
				if (bookshelfItem instanceof BookshelfBookItem) {
					BookshelfBookItem bookshelfBookItem = (BookshelfBookItem) bookshelfItem;
					BookItem bookItem = new NativeBookItem(getActivity(),
							bookshelfBookItem);

					Integer tag = (Integer) cover.getTag();
					if (tag == null || tag.intValue() != position) {
						Bitmap bitmap = ImageResizer
								.decodeSampledBitmapFromAsset(getActivity(),
										bookshelfBookItem.mBookThumbImage,
										mImageHeight);
						bitmap = ImageResizer.getRoundedCornerBitmap(
								getActivity(), bitmap);
						cover.setImageBitmap(bitmap);
					}

					name.setText(bookshelfBookItem.mBookName);

					BooksReadProgressItem progressItem = BooksReadProgressDao
							.getInstance(getActivity()).queryProgressByBookId(
									bookItem.mBookId);
					if (progressItem != null) {
						progress.setText("已读：" + progressItem.mProgressInBook
								+ "%");
					} else {
						progress.setText("未读");
					}

					container.setOnClickListener(new BookReadOnClickListener(
							bookItem));
				} else if (bookshelfItem instanceof BookshelfImageItem) {
					final BookshelfImageItem bookshelfImageItem = (BookshelfImageItem) bookshelfItem;

					Integer tag = (Integer) cover.getTag();
					if (tag == null || tag.intValue() != position) {
						Bitmap bitmap = ImageResizer
								.decodeSampledBitmapFromAsset(getActivity(),
										bookshelfImageItem.mImageFilePath,
										mImageHeight);
						bitmap = ImageResizer.getRoundedCornerBitmap(
								getActivity(), bitmap);
						cover.setImageBitmap(bitmap);
					}

					name.setText(bookshelfImageItem.mImageName);
					progress.setText("");

					container.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							startActivity(ImageActivity.getIntent(
									getActivity(),
									bookshelfImageItem.mImageFilePath));
						}
					});
				}

				cover.setTag(Integer.valueOf(position));
			} else {
				container.setVisibility(View.GONE);
			}
		}

		/**
		 * 单个书籍view holder
		 * 
		 * @author Calvin
		 * 
		 */
		private class BookItemHolder {
			/** 左 容器 */
			public View mLeftContainer;
			/** 中 容器 */
			public View mMiddleContainer;
			/** 右 容器 */
			public View mRightContainer;

			/** 书籍封面 */
			public ImageView mLeftCover;
			/** 书籍名称 */
			public TextView mLeftName;
			/** 书籍进度 */
			public TextView mLeftProgress;

			/** 书籍封面 */
			public ImageView mMiddleCover;
			/** 书籍名称 */
			public TextView mMiddleName;
			/** 书籍进度 */
			public TextView mMiddleProgress;

			/** 书籍封面 */
			public ImageView mRightCover;
			/** 书籍名称 */
			public TextView mRightName;
			/** 书籍进度 */
			public TextView mRightProgress;
		}
	}

	/**
	 * 图书点击事件
	 * 
	 * @author Calvin
	 * 
	 */
	private class BookReadOnClickListener implements OnClickListener {

		/** bookitem */
		private BookItem mBookItem;

		/**
		 * 构造
		 * 
		 * @param item
		 */
		public BookReadOnClickListener(BookItem item) {
			mBookItem = item;
		}

		@Override
		public void onClick(View v) {
			getActivity().startActivity(
					BooksReadActivity.getIntent(getActivity(), mBookItem, 0));
		}

	}

	@Override
	public void onSuccess(AbstractModel model) {
	}

	@Override
	public void onFailed(AbstractModel model, int errorCode) {
	}

}
