package com.fuyue.danmei.read.db;

import java.util.LinkedList;
import java.util.List;
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
 * 图书章节阅读记录管理类
 * 
 * @author Calvin
 * 
 */
public class BooksReadChapterDao {

	/** log tag. */
	private final String TAG = getClass().getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/*
	 * 更新数据库字段时，需要重新更改索引
	 */
	/** column index bookid */
	private static final int INDEX_BOOKID = 0;
	/** column index readprogress */
	private static final int INDEX_CHPATER = 1;

	/** instance */
	private static BooksReadChapterDao sBooksReadChapterDao;
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
	public static BooksReadChapterDao getInstance(Context context) {
		if (sBooksReadChapterDao == null) {
			synchronized (BooksReadChapterDao.class) {
				sBooksReadChapterDao = new BooksReadChapterDao(context);
			}
		}
		return sBooksReadChapterDao;
	}

	/**
	 * 构造
	 * 
	 * @param context
	 */
	private BooksReadChapterDao(Context context) {
		mContext = context;
		mExecutor = Executors.newSingleThreadExecutor();
	}

	/**
	 * 添加一本书的章节阅读记录
	 * 
	 * @param bookId
	 * @param chapterId
	 */
	public void insertChapterRecord(final int bookId, int chapterId) {
		if (DEBUG) {
			Log.d(TAG, "add reading record to " + bookId + " chapter "
					+ chapterId);
		}

		if (queryRecordChapterExistByChapterId(bookId, chapterId)) {
			return;
		}

		final ContentValues chapterValues = new ContentValues();
		chapterValues.put(BooksDBHelper.COLUMN_BOOKID, bookId);
		chapterValues.put(BooksDBHelper.COLUMN_CHAPTER, chapterId);

		runTransactionAsync(new SQLiteTransaction() {

			@Override
			protected boolean performTransaction(ContentResolver cr) {
				cr.insert(BooksProvider.BOOKS_READ_CHAPTER_URI, chapterValues);
				return true;
			}
		}, false);
	}

	/**
	 * 获取一本书的全部章节阅读记录
	 * 
	 * @param bookId
	 * @return
	 */
	public List<Integer> queryAllRecordChapterByBookId(int bookId) {
		if (DEBUG) {
			Log.d(TAG, "get chapter record of " + bookId);
		}

		List<Integer> chapters = new LinkedList<Integer>();
		Cursor cursor = mContext.getContentResolver().query(
				BooksProvider.BOOKS_READ_CHAPTER_URI, null,
				BooksDBHelper.COLUMN_BOOKID + "=?",
				new String[] { bookId + "" }, null);

		if (cursor == null) {
			return chapters;
		}

		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			chapters.add(cursor.getInt(INDEX_CHPATER));
		}

		if (cursor != null) {
			cursor.close();
		}

		return chapters;
	}

	/**
	 * 检验指定bookId下的指定chapterId是否已经记录
	 * 
	 * @param bookId
	 * @param chapterId
	 * @return
	 */
	public boolean queryRecordChapterExistByChapterId(final int bookId,
			int chapterId) {
		if (DEBUG) {
			Log.d(TAG, "check reading record to " + bookId + " chapter "
					+ chapterId);
		}

		boolean isExist = false;
		Cursor cursor = mContext.getContentResolver().query(
				BooksProvider.BOOKS_READ_CHAPTER_URI,
				null,
				BooksDBHelper.COLUMN_BOOKID + "=? and "
						+ BooksDBHelper.COLUMN_CHAPTER + "=?",
				new String[] { bookId + "", chapterId + "" }, null);
		if (cursor != null && cursor.moveToFirst()) {
			isExist = true;
		}

		if (cursor != null) {
			cursor.close();
		}

		return isExist;
	}

	/**
	 * 删除一本书的章节阅读进度
	 * 
	 * @param bookId
	 */
	public void deleteChapterRecordByBookId(final int bookId) {
		if (DEBUG) {
			Log.d(TAG, "delete chapter record of " + bookId);
		}

		runTransactionAsync(new SQLiteTransaction() {

			@Override
			protected boolean performTransaction(ContentResolver cr) {
				cr.delete(BooksProvider.BOOKS_READ_CHAPTER_URI,
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
}
