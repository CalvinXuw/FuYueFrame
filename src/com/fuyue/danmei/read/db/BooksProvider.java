package com.fuyue.danmei.read.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.fuyue.BaseApplicaion;

/**
 * books db provider
 * 
 * @author Calvin
 * 
 */
public class BooksProvider extends ContentProvider {

	static final String PACKAGENAME = BaseApplicaion.sPackageName;

	/** log tag. */
	private final String TAG = getClass().getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/** MIME type for the books read chapter */
	private static final String BOOKS_READ_CHAPTER_TYPE = "vnd.android.cursor.bookreadchapter";
	/** MIME type for the books read progress */
	private static final String BOOKS_READ_PROGRESS_TYPE = "vnd.android.cursor.bookreadprogress";

	/** URI matcher used to recognize URIs sent by applications */
	private static UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	/** URI matcher constant for the URI of 阅读过的章节记录 */
	private static final int BOOKS_READ_CHAPTER = 1;
	/** URI matcher constant for the URI of 阅读进度 */
	private static final int BOOKS_READ_PROGRESS = 2;

	/** AUTHORITY. */
	public static final String AUTHORITY = PACKAGENAME + ".books";

	/** uri 书籍章节记录 */
	public static final Uri BOOKS_READ_CHAPTER_URI = Uri.parse("content://"
			+ AUTHORITY + "/bookreadchapter");
	/** uri 书籍进度记录 */
	public static final Uri BOOKS_READ_PROGRESS_URI = Uri.parse("content://"
			+ AUTHORITY + "/bookreadprogress");

	static {
		sURIMatcher.addURI(AUTHORITY, "bookreadchapter", BOOKS_READ_CHAPTER);
		sURIMatcher.addURI(AUTHORITY, "bookreadprogress", BOOKS_READ_PROGRESS);
	}

	/** DBHelper */
	private BooksDBHelper mDbHelper;

	@Override
	public boolean onCreate() {
		mDbHelper = new BooksDBHelper(getContext());
		return false;
	}

	@Override
	public String getType(Uri uri) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case BOOKS_READ_CHAPTER:
			return BOOKS_READ_CHAPTER_TYPE;
		case BOOKS_READ_PROGRESS:
			return BOOKS_READ_PROGRESS_TYPE;
		}
		throw new IllegalArgumentException("unknow uri:" + uri);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table = getTableNameFromUri(uri);
		long rowId = mDbHelper.getWritableDatabase()
				.insert(table, null, values);
		if (rowId == -1) {
			Log.d(TAG, "insert into book database failed");
			return null;
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return ContentUris.withAppendedId(uri, rowId);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table = getTableNameFromUri(uri);
		int count = mDbHelper.getWritableDatabase().delete(table, selection,
				selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		String table = getTableNameFromUri(uri);
		int count = mDbHelper.getWritableDatabase().update(table, values,
				selection, selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String table = getTableNameFromUri(uri);
		Cursor cursor = mDbHelper.getReadableDatabase().query(table,
				projection, selection, selectionArgs, null, null, sortOrder);
		return cursor;
	}

	/**
	 * 从uri中通过matcher匹配获取tablename
	 * 
	 * @param uri
	 * @return
	 */
	private String getTableNameFromUri(Uri uri) {
		String table = null;

		int match = sURIMatcher.match(uri);
		switch (match) {
		case BOOKS_READ_CHAPTER:
			table = BooksDBHelper.TABLE_BOOKCHAPTER;
			break;
		case BOOKS_READ_PROGRESS:
			table = BooksDBHelper.TABLE_BOOKPROGRESS;
			break;
		default:
			throw new IllegalArgumentException("unknow uri:" + uri);
		}
		return table;
	}

}
