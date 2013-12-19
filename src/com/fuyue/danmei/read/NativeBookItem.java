package com.fuyue.danmei.read;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.fuyue.danmei.index.model.BookshelfModel.BookshelfBookItem;
import com.fuyue.util.logging.Log;

/**
 * 本地书籍item
 * 
 * @author Calvin
 * 
 */
public class NativeBookItem extends BookItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2803044699868952609L;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param shelfItem
	 */
	public NativeBookItem(Context context, BookshelfBookItem shelfItem) {
		mBookName = shelfItem.mBookName;
		mBookId = shelfItem.mBookId;
		mChapterItems = new LinkedList<BookItem.ChapterItem>();
		mChapterItems.add(new NativeChapterItem("全本", shelfItem.mBookFilePath,
				1));
		mCoverImage = shelfItem.mCover;
		mBackCoverImage = shelfItem.mBackCover;
	}

	/**
	 * 嵌套解析的章节item
	 * 
	 * @author Calvin
	 * 
	 */
	public static class NativeChapterItem extends ChapterItem {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1542726167053563813L;

		/**
		 * 构造
		 * 
		 * @param title
		 * @param filename
		 * @param chapterid
		 */
		public NativeChapterItem(String title, String filename, int chapterid) {
			mChapterName = title;
			mFileName = filename;
			mChapterId = chapterid;
		}

		/** 获取章节内容 */
		public ByteBuffer getChapterByteBuffer(Context context) {
			try {
				byte[] bytes = new byte[(int) getChapterLength(context)];
				context.getAssets().open(mFileName).read(bytes);
				return ByteBuffer.wrap(bytes);
			} catch (Exception e) {
				if (DEBUG) {
					Log.w(TAG, e);
				}
			}
			return null;
		}

		@Override
		public ByteBuffer getChapterByteBuffer(Context context, int start,
				int length) {
			try {
				byte[] bytes = new byte[length];
				InputStream assetInputStream = context.getAssets().open(
						mFileName);
				assetInputStream.skip(start);
				assetInputStream.read(bytes);
				return ByteBuffer.wrap(bytes);
			} catch (Exception e) {
				if (DEBUG) {
					Log.w(TAG, e);
				}
			}
			return null;
		}

		/** 获取章节内容长度 */
		public long getChapterLength(Context context) {
			try {
				InputStream is = context.getAssets().open(mFileName);
				return is.available();
			} catch (Exception e) {
				if (DEBUG) {
					Log.w(TAG, e);
				}
			}
			return 0;
		}
	}

	@Override
	public Bitmap getCoverImage(Context context) {
		try {
			return BitmapFactory.decodeStream(context.getAssets().open(
					mCoverImage));
		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
		}
		return null;
	}

	@Override
	public Bitmap getBackCoverImage(Context context) {
		try {
			return BitmapFactory.decodeStream(context.getAssets().open(
					mBackCoverImage));
		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
		}
		return null;
	}
}
