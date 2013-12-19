package com.fuyue.danmei.read;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;

import com.fuyue.util.logging.Log;
import com.fuyue.util.net.parser.AbstractIfengXMLItem;

/**
 * 书籍item
 * 
 * @author Calvin
 * 
 */
public abstract class BookItem extends AbstractIfengXMLItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2833498846312513637L;

	/** 图书id */
	public int mBookId;
	/** 图书名 */
	public String mBookName;
	/** 书籍简介 */
	public String mBookDesc;
	/** 章节item */
	public LinkedList<ChapterItem> mChapterItems;
	/** 缩略图 */
	public String mThumbImage;
	/** 封面 */
	public String mCoverImage;
	/** 封底 */
	public String mBackCoverImage;

	/**
	 * 获取封面
	 * 
	 * @param context
	 * @return
	 */
	public abstract Bitmap getCoverImage(Context context);

	/**
	 * 获取封底
	 * 
	 * @param context
	 * @return
	 */
	public abstract Bitmap getBackCoverImage(Context context);

	/**
	 * 嵌套解析的章节item
	 * 
	 * @author Calvin
	 * 
	 */
	public static abstract class ChapterItem extends AbstractIfengXMLItem {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5335314746355686286L;
		/** 章节标题 */
		public String mChapterName;
		/** 章节文件所在 */
		public String mFileName;
		/** 章节序号 */
		public int mChapterId;

		/** 获取章节全部内容 */
		public ByteBuffer getChapterByteBuffer(Context context) {
			try {
				File bookFile = new File(mFileName);
				long lLen = bookFile.length();

				return new RandomAccessFile(bookFile, "r").getChannel().map(
						FileChannel.MapMode.READ_ONLY, 0, lLen);

			} catch (Exception e) {
				if (DEBUG) {
					Log.w(TAG, e);
				}
			}
			return null;
		}

		/**
		 * 获取指定位置长度的内容片断
		 * 
		 * @param context
		 * @param start
		 * @param length
		 * @return
		 */
		public ByteBuffer getChapterByteBuffer(Context context, int start,
				int length) {
			try {
				File bookFile = new File(mFileName);
				return new RandomAccessFile(bookFile, "r").getChannel().map(
						FileChannel.MapMode.READ_ONLY, start, length);

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
				File book_file = new File(mFileName);
				return book_file.length();
			} catch (Exception e) {
				if (DEBUG) {
					Log.w(TAG, e);
				}
			}
			return 0;
		}
	}
}
