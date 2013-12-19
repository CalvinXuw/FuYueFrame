package com.fuyue.danmei.read.db;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.fuyue.BaseApplicaion;
import com.fuyue.util.database.SQLiteTransaction;
import com.fuyue.util.logging.Log;

/**
 * 阅读进度的管理类
 * 
 * @author Calvin
 * 
 */
public class BooksReadProgressDao {

	/** log tag. */
	private final String TAG = getClass().getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/*
	 * 更新数据库字段时，需要重新更改索引
	 */
	/** column index bookid */
	private static final int INDEX_BOOKID = 0;
	/** column index chapter */
	private static final int INDEX_CHAPTER = 1;
	/** column index readprogress */
	private static final int INDEX_READPROGRESS = 2;
	/** column index readprogress float */
	private static final int INDEX_READPROGRESS_FLOAT = 3;

	/** instance */
	private static BooksReadProgressDao sBookReadProgressDao;
	/** context */
	private Context mContext;
	/** 用于执行数据库操作 */
	private ExecutorService mExecutor;

	/**
	 * 获取一个实例，单例
	 * 
	 * @param context
	 * @return
	 */
	public static BooksReadProgressDao getInstance(Context context) {
		if (sBookReadProgressDao == null) {
			synchronized (BooksReadProgressDao.class) {
				sBookReadProgressDao = new BooksReadProgressDao(context);
			}
		}
		return sBookReadProgressDao;
	}

	/**
	 * 构造
	 * 
	 * @param context
	 */
	private BooksReadProgressDao(Context context) {
		mContext = context.getApplicationContext();
		mExecutor = Executors.newSingleThreadExecutor();
	}

	/**
	 * 添加一本书的阅读记录
	 * 
	 * @param bookId
	 * @param progress
	 */
	public void insertProgress(final int bookId, int chapter, int progress,
			float progressInBook) {
		if (DEBUG) {
			Log.d(TAG, "add reading progress to " + bookId + " chapter "
					+ chapter + " progress " + progress);
		}

		boolean isUpdate = false;
		Cursor cursor = mContext.getContentResolver().query(
				BooksProvider.BOOKS_READ_PROGRESS_URI,
				new String[] { BooksDBHelper.COLUMN_READPROGRESS },
				BooksDBHelper.COLUMN_BOOKID + "=?",
				new String[] { bookId + "" }, null);
		if (cursor != null && cursor.moveToFirst()) {
			isUpdate = true;
		}

		if (cursor != null) {
			cursor.close();
		}

		final ContentValues progressValues = new ContentValues();
		progressValues.put(BooksDBHelper.COLUMN_BOOKID, bookId);
		progressValues.put(BooksDBHelper.COLUMN_CHAPTER, chapter);
		progressValues.put(BooksDBHelper.COLUMN_READPROGRESS, progress);
		progressValues.put(BooksDBHelper.COLUMN_READPROGRESS_FLOAT,
				progressInBook);

		final boolean update = isUpdate;
		runTransactionAsync(new SQLiteTransaction() {

			@Override
			protected boolean performTransaction(ContentResolver cr) {
				if (update) {
					cr.update(BooksProvider.BOOKS_READ_PROGRESS_URI,
							progressValues, BooksDBHelper.COLUMN_BOOKID + "=?",
							new String[] { bookId + "" });
				} else {
					cr.insert(BooksProvider.BOOKS_READ_PROGRESS_URI,
							progressValues);
				}
				return true;
			}
		}, false);
	}

	/**
	 * 获取一本书的阅读记录
	 * 
	 * @param bookId
	 * @return
	 */
	public BooksReadProgressItem queryProgressByBookId(int bookId) {
		if (DEBUG) {
			Log.d(TAG, "get reading progress of " + bookId);
		}

		BooksReadProgressItem item = new BooksReadProgressItem();
		Cursor cursor = mContext.getContentResolver().query(
				BooksProvider.BOOKS_READ_PROGRESS_URI, null,
				BooksDBHelper.COLUMN_BOOKID + "=?",
				new String[] { bookId + "" }, null);
		boolean result = false;

		if (cursor != null && cursor.moveToFirst()) {
			item.mChapterId = cursor.getInt(INDEX_CHAPTER);
			item.mProgress = cursor.getInt(INDEX_READPROGRESS);
			item.mProgressInBook = cursor.getFloat(INDEX_READPROGRESS_FLOAT);
			result = true;
		}

		if (cursor != null) {
			cursor.close();
		}

		return result ? item : null;
	}

	/**
	 * 删除一本书的阅读进度
	 * 
	 * @param bookId
	 */
	public void deleteProgressByBookId(final int bookId) {
		if (DEBUG) {
			Log.d(TAG, "delete progress of " + bookId);
		}

		runTransactionAsync(new SQLiteTransaction() {

			@Override
			protected boolean performTransaction(ContentResolver cr) {
				cr.delete(BooksProvider.BOOKS_READ_PROGRESS_URI,
						BooksDBHelper.COLUMN_BOOKID + "=?",
						new String[] { bookId + "" });
				return true;
			}
		}, false);
	}

	/**
	 * 异步执行数据库的更新，删除，增加操作，由于查找需要返回数据，所以不采用本方法
	 * 
	 * @param transaction
	 * @param async
	 */
	private void runTransactionAsync(final SQLiteTransaction transaction,
			boolean async) {
		if (async) {
			mExecutor.execute(new Runnable() {

				@Override
				public void run() {
					transaction.run(mContext.getContentResolver());
				}
			});
		} else {
			transaction.run(mContext.getContentResolver());
		}
	}

	/**
	 * 图书阅读进度item
	 * 
	 * @author Calvin
	 * 
	 */
	public class BooksReadProgressItem {
		/** chapter id */
		public int mChapterId;
		/** progress */
		public int mProgress;
		/** progress float */
		public float mProgressInBook;
	}
}
