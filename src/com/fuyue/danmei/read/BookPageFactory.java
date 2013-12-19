package com.fuyue.danmei.read;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.Point;

import com.fuyue.BaseApplicaion;
import com.fuyue.danmei.read.BookConstants.ReadStyle;
import com.fuyue.danmei.read.BookItem.ChapterItem;
import com.fuyue.util.Utility;
import com.fuyue.util.imagecache.ImageResizer;
import com.fuyue.util.logging.Log;

/**
 * 阅读页面绘制类
 * 
 * @author Calvin
 * 
 */
public class BookPageFactory {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/** 字符集 */
	private static final String CHARSET = "UTF-8";
	/** context */
	private Context mContext;

	/** 屏幕上下留白 */
	private static final int MARGIN_HEIGHT = 30;
	/** 屏幕左右留白 */
	private static final int MARGIN_WIDTH = 20;
	/** 顶部信息文字大小 */
	private static final int FONT_SIZE_INFO = 14;

	/** 缓冲区大小50k */
	private static final int TEXT_BUFFER_SIZE = 50 * 1024;

	/** 当前阅读的章节类型，用于提供显示封面封底使用 */
	private enum ChapterType {
		FIRST, LAST, NORMAL, SIINGLE
	}

	// 绘制配置信息
	/** 页面宽度 */
	private int mWidth;
	/** 页面高度 */
	private int mHeight;
	/** 文字内容的纵向margin */
	private int mMarginHeight;
	/** 文字内容的横向margin */
	private int mMarginWidth;
	/** 文字内容的宽度 */
	private int mContentWidth;
	/** 文字内容的高度 */
	private int mContentHeight;

	/** 每页可以显示的行数 */
	private int mLineCount;
	/** 正文笔刷 */
	private Paint mContentPaint;
	/** 脚标笔刷 */
	private Paint mInfoPaint;

	/** 绘制的的背景颜色 */
	private int mBackgroundColor;
	/** 绘制的背景图 */
	private Bitmap mBackgroundBitmap;
	/** 内容字号 */
	private int mContentFontSize;
	/** 信息字号 */
	private int mInfoFontSize;
	/** 内容字色 */
	private int mContentFontColor;
	/** 信息字色 */
	private int mInfoFontColor;

	// 绘制内容信息
	/** 当前内容文本buffer 起始位置 */
	private int mTextBufferStart;
	/** 当前章节全部内容文本 */
	private ByteBuffer mTextBuffer = null;
	/** 当前章节总长度 */
	private int mChapterLength = 0;
	/** 当前绘制页面的内容 */
	private Vector<String> mCurrentLines = new Vector<String>();
	/** 当前页起始位置 */
	private int mCurrentPageBegin = 0;
	/** 当前页终止位置 */
	private int mCurrentPageEnd = 0;

	// 书籍信息
	/** 书籍item */
	private BookItem mBookItem;
	/** 章节item */
	private ChapterItem mChapterItem;
	/** 章节类型 */
	private ChapterType mChapterType;

	/** 进度相关callback */
	private ChapterProgressCallback mChapterProgressCallback;

	public BookPageFactory(Context context, int fontSize, ReadStyle readStyle) {
		mContext = context;
		updateFactory(fontSize, readStyle);
	}

	/**
	 * 更新工厂绘制配置参数
	 * 
	 * @param fontSize
	 * @param readStyle
	 */
	public void updateFactory(int fontSize, ReadStyle readStyle) {
		mWidth = (int) Utility.getScreenWidth(mContext);
		mHeight = (int) ((int) Utility.getScreenHeight(mContext) - 50 * Utility
				.getDensity(mContext));// 广告高度

		if (readStyle == ReadStyle.LIGHT) {
			mContentFontColor = Color
					.parseColor(BookConstants.TEXT_COLOR_LIGHT);
			mInfoFontColor = Color
					.parseColor(BookConstants.TEXT_COLOR_LIGHT_INFO);
			mBackgroundColor = Color.parseColor(BookConstants.BACKGROUND_LIGHT);
		} else {
			mContentFontColor = Color.parseColor(BookConstants.TEXT_COLOR_DARK);
			mInfoFontColor = Color
					.parseColor(BookConstants.TEXT_COLOR_DARK_INFO);
			mBackgroundColor = Color.parseColor(BookConstants.BACKGROUND_DARK);
		}

		mContentFontSize = (int) (fontSize * Utility.getDensity(mContext));
		mInfoFontSize = (int) (FONT_SIZE_INFO * Utility.getDensity(mContext));

		mMarginHeight = (int) (MARGIN_HEIGHT * Utility.getDensity(mContext));
		mMarginWidth = (int) (MARGIN_WIDTH * Utility.getDensity(mContext));

		mContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 画笔
		mContentPaint.setTextAlign(Align.LEFT);// 左对齐
		mContentPaint.setTextSize(mContentFontSize);// 字体大小
		mContentPaint.setColor(mContentFontColor);// 字体颜色

		mInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mInfoPaint.setTextAlign(Align.LEFT);
		mInfoPaint.setTextSize(mInfoFontSize);
		mInfoPaint.setColor(mInfoFontColor);
		mContentWidth = mWidth - mMarginWidth * 2;
		mContentHeight = mHeight - mMarginHeight * 2;
		// 1.5倍行距
		mLineCount = (int) (mContentHeight / (mContentFontSize * 1.5));
	}

	/**
	 * 
	 * @param chapterItem
	 * @param begin
	 *            表示进度，读取进度时，将begin值给 {@link #mCurrentPageBegin}作为开始位置进度记录
	 * @throws IOException
	 */
	public void openbook(BookItem bookItem, ChapterItem chapterItem, int begin)
			throws IOException {
		mBookItem = bookItem;
		mChapterItem = chapterItem;

		// 若为单章阅读
		if (mBookItem.mChapterItems.size() == 1) {
			if (mBookItem.mCoverImage != null
					&& mBookItem.mBackCoverImage != null) {
				// 有封面封底
				mChapterType = ChapterType.SIINGLE;
			} else if (mBookItem.mCoverImage != null) {
				// 仅有封面
				mChapterType = ChapterType.FIRST;
			} else if (mBookItem.mBackCoverImage != null) {
				// 仅有封底
				mChapterType = ChapterType.LAST;
			} else {
				mChapterType = ChapterType.NORMAL;
			}
		} else if (mBookItem.mChapterItems.getFirst() == chapterItem) {
			// 若为第一章
			if (mBookItem.mCoverImage != null) {
				// 有封面
				mChapterType = ChapterType.FIRST;
			} else {
				mChapterType = ChapterType.NORMAL;
			}
		} else if (mBookItem.mChapterItems.getLast() == chapterItem) {
			// 若为最后第一章
			if (mBookItem.mBackCoverImage != null) {
				// 有封底
				mChapterType = ChapterType.LAST;
			} else {
				mChapterType = ChapterType.NORMAL;
			}
		}

		mChapterLength = (int) chapterItem.getChapterLength(mContext);

		if (!hasCover() && begin == -1) {
			begin = 0;
		}
		if (!hasBackCover() && begin == mChapterLength + 1) {
			begin = mChapterLength - 1;
		}

		mCurrentPageBegin = begin;
		mCurrentPageEnd = begin;

		loadTextBuffer(begin);
	}

	/**
	 * 读取文本内容到缓冲区
	 */
	private void loadTextBuffer(int start) {
		start = Math.max(0, Math.min(start, mChapterLength));
		int edgeWarningBuffer = 5 * 1024;// 5kb的边界保护，防止加载越界
		if (mChapterLength <= TEXT_BUFFER_SIZE) {
			if (mTextBuffer == null) {
				mTextBuffer = mChapterItem.getChapterByteBuffer(mContext);
			}
		} else {
			boolean needReload = false;
			if (mTextBuffer == null) {
				needReload = true;
			} else {
				if (start < mTextBufferStart + edgeWarningBuffer
						&& mTextBufferStart != 0) {
					needReload = true;
					mTextBuffer.clear();
				} else if (start > mTextBufferStart + mTextBuffer.capacity()
						- edgeWarningBuffer
						&& mTextBufferStart + mTextBuffer.capacity() != mChapterLength) {
					needReload = true;
					mTextBuffer.clear();
				}
			}

			if (needReload) {
				int leftEdge = (start - TEXT_BUFFER_SIZE / 2) > 0 ? (start - TEXT_BUFFER_SIZE / 2)
						: 0;
				int rightEdge = (leftEdge + TEXT_BUFFER_SIZE < mChapterLength) ? (leftEdge + TEXT_BUFFER_SIZE)
						: mChapterLength;
				mTextBufferStart = leftEdge;
				mTextBuffer = mChapterItem.getChapterByteBuffer(mContext,
						leftEdge, rightEdge - leftEdge);
			}

		}
	}

	/**
	 * 添加回调
	 * 
	 * @param callback
	 */
	public void setChapterProgressCallback(ChapterProgressCallback callback) {
		mChapterProgressCallback = callback;
	}

	/**
	 * 是否为首页，用于外部判断是否还有下一页
	 * 
	 * @return
	 */
	public boolean isFirstPage() {
		if (hasCover()) {
			return isCover();
		}
		return isFirstContent();
	}

	/**
	 * 是否末页，用于外部判断是否还有下一页
	 * 
	 * @return
	 */
	public boolean isLastPage() {
		if (hasBackCover()) {
			return isBackCover();
		}
		return isLastContent();
	}

	/**
	 * 是否可显示封面
	 * 
	 * @return
	 */
	private boolean hasCover() {
		return mChapterType == ChapterType.FIRST
				|| mChapterType == ChapterType.SIINGLE;
	}

	/**
	 * 是否可显示封底
	 * 
	 * @return
	 */
	private boolean hasBackCover() {
		return mChapterType == ChapterType.LAST
				|| mChapterType == ChapterType.SIINGLE;
	}

	/**
	 * 是否为封面
	 * 
	 * @return
	 */
	private boolean isCover() {
		if (hasCover()) {
			return mCurrentPageBegin <= -1;
		}
		return false;
	}

	/**
	 * 是否为封底
	 * 
	 * @return
	 */
	private boolean isBackCover() {
		if (hasBackCover()) {
			return mCurrentPageEnd >= mChapterLength + 1;
		}
		return false;
	}

	/**
	 * 是否为文字内容第一页
	 * 
	 * @return
	 */
	private boolean isFirstContent() {
		return mCurrentPageBegin <= 0 && !isLastPage();
	}

	/**
	 * 是否为文字内容最后一页
	 * 
	 * @return
	 */
	private boolean isLastContent() {
		return mCurrentPageEnd >= mChapterLength && !isFirstPage();
	}

	/**
	 * 重新计算当前页面内容
	 */
	public void reCalculateCurrentPage() {
		mCurrentLines.clear();
		mCurrentPageEnd = mCurrentPageBegin;
	}

	/**
	 * 向前翻页
	 */
	public void turePrePage() {
		if (isFirstContent()) {
			if (hasCover()) {
				mCurrentPageBegin = -1;
			}
			return;
		}

		if (hasBackCover() && isLastPage()) {
			mCurrentPageEnd = mChapterLength - 1;
			turePrePage();
			tureNextPage();
			return;
		}

		mCurrentLines.clear();
		mCurrentPageBegin = getPrePageBegin();
		mCurrentLines = getPageContent();
	}

	/**
	 * 向后翻页
	 */
	public void tureNextPage() {
		if (isLastContent()) {
			if (hasBackCover()) {
				mCurrentPageEnd = mChapterLength + 1;
			}
			return;
		}

		if (hasCover() && isFirstPage()) {
			mCurrentPageBegin = mCurrentPageEnd = 0;
		}

		mCurrentLines.clear();
		mCurrentPageBegin = mCurrentPageEnd;// 下一页页起始位置=当前页结束位置
		mCurrentLines = getPageContent();
	}

	/**
	 * 翻到上一章
	 */
	public void turnPreChapter() {
		if (mCurrentPageBegin <= 0) {
			return;
		}
		mCurrentLines.clear();
		mCurrentPageBegin = mCurrentPageEnd = getLastPageBegin();
		mCurrentLines = getPageContent();
	}

	/**
	 * 翻到下一章
	 */
	public void turnNextChapter() {
		mCurrentLines.clear();
		mCurrentPageBegin = mCurrentPageEnd = 0;
		mCurrentLines = getPageContent();
	}

	/**
	 * 绘制当前页面
	 * 
	 * @param c
	 */
	public void drawPage(Canvas c) {
		// 绘制封面
		if (hasCover() && isCover()) {
			if (mBackgroundBitmap == null) {
				c.drawColor(mBackgroundColor);
			} else {
				c.drawBitmap(mBackgroundBitmap, 0, 0, null);
			}
			Bitmap cover = mBookItem.getCoverImage(mContext);
			cover = ImageResizer.resizeBitmap(cover, mWidth, mHeight);
			c.drawBitmap(cover, (mWidth - cover.getWidth()) / 2,
					(mHeight - cover.getHeight()) / 2, null);
			return;
			// 绘制封底
		} else if (hasBackCover() && isBackCover()) {
			if (mBackgroundBitmap == null) {
				c.drawColor(Color.parseColor(BookConstants.BACKGROUND_DARK));
				// c.drawColor(mBackgroundColor);
			} else {
				c.drawBitmap(mBackgroundBitmap, 0, 0, null);
			}
			Bitmap cover = mBookItem.getBackCoverImage(mContext);
			cover = ImageResizer.resizeBitmap(cover, mWidth, mHeight);
			c.drawBitmap(cover, (mWidth - cover.getWidth()) / 2,
					(mHeight - cover.getHeight()) / 2, null);
			return;
		}

		// 绘制正文
		if (mCurrentLines.size() == 0) {
			mCurrentLines = getPageContent();
		}
		if (mCurrentLines.size() > 0) {
			if (mBackgroundBitmap == null) {
				c.drawColor(mBackgroundColor);
			} else {
				c.drawBitmap(mBackgroundBitmap, 0, 0, null);
			}
			int y = (int) (mMarginHeight / 2 + mInfoFontSize);
			for (String strLine : mCurrentLines) {
				y += (mContentFontSize * 1.5);
				c.drawText(strLine, mMarginWidth, y, mContentPaint);
			}
		}

		// 计算书名
		// String bookName = null;
		// if (mBookItem.mBookName.length() > 12) {
		// bookName = mBookItem.mBookName.substring(0, 5)
		// + "..."
		// + mBookItem.mBookName.substring(
		// mBookItem.mBookName.length() - 5,
		// mBookItem.mBookName.length());
		// } else {
		// bookName = mBookItem.mBookName;
		// }

		// 原有书名，章节名，进度绘制
		// int nPercentWidth = (int) mContentPaint.measureText("999.9%") + 1;
		// c.drawText(strPercent, mWidth - nPercentWidth,
		// (float) (mHeight - mMarginHeight / 2), mInfoPaint);
		// c.drawText(mChapterItem.mChapterName, mWidth / 2,
		// (float) (mHeight - mMarginHeight / 2), mInfoPaint);
		// c.drawText(bookName, mWidth / 2,
		// (float) (mMarginHeight / 2 + mInfoFontSize), mInfoPaint);
		drawProgress(c);
		drawTimeAndBattery(c);
	}

	/**
	 * 绘制进度
	 * 
	 * @param c
	 */
	private void drawProgress(Canvas c) {
		// 计算阅读进度
		float current = mCurrentPageBegin
				+ mChapterProgressCallback.getChapterStartProgress();
		int total = mChapterProgressCallback.getBookTotalLength();
		float fPercent = current / total;
		DecimalFormat df = new DecimalFormat("#0.0");
		String strPercent = df.format(fPercent * 100) + "%";
		FontMetrics fm = mInfoPaint.getFontMetrics();
		c.drawText(strPercent, mMarginWidth, mMarginHeight / 2
				+ (fm.bottom + fm.ascent - fm.top) / 1, mInfoPaint);
	}

	/**
	 * 绘制时间以及电池电量
	 * 
	 * @param c
	 */
	private void drawTimeAndBattery(Canvas c) {
		// 绘制电池电量，电池尺寸为60*132，其中尾部尺寸60*120,头部为30*12
		Path battery = new Path();
		mInfoPaint.setStyle(Paint.Style.STROKE);
		mInfoPaint.setStrokeWidth(Utility.getDensity(mContext));

		float scale = 0.2f * Utility.getScaledDensity(mContext);
		Point orginPoint = new Point(
				(int) (mWidth - mMarginWidth - scale * 132),
				(int) ((mMarginHeight - scale * 60) / 2));
		// 外框
		battery.moveTo(orginPoint.x, orginPoint.y);
		battery.lineTo(orginPoint.x + 120 * scale, orginPoint.y);
		battery.lineTo(orginPoint.x + 120 * scale, orginPoint.y + 15 * scale);
		battery.lineTo(orginPoint.x + 132 * scale, orginPoint.y + 15 * scale);
		battery.lineTo(orginPoint.x + 132 * scale, orginPoint.y + 45 * scale);
		battery.lineTo(orginPoint.x + 120 * scale, orginPoint.y + 45 * scale);
		battery.lineTo(orginPoint.x + 120 * scale, orginPoint.y + 60 * scale);
		battery.lineTo(orginPoint.x, orginPoint.y + 60 * scale);
		battery.lineTo(orginPoint.x, orginPoint.y);
		c.drawPath(battery, mInfoPaint);

		mInfoPaint.setStyle(Paint.Style.FILL);
		mInfoPaint.setStrokeWidth(1);
		// 填充电量
		c.drawRect(orginPoint.x + 10 * scale, orginPoint.y + 10 * scale,
				orginPoint.x + (120 - 10) * Utility.getBattery(mContext)
						* scale, orginPoint.y + (60 - 10) * scale, mInfoPaint);

		// 绘制时间
		int timeWidth = (int) mInfoPaint.measureText(Utility.getTime24Hours());
		FontMetrics fm = mInfoPaint.getFontMetrics();
		c.drawText(Utility.getTime24Hours(), mWidth - mMarginWidth - scale
				* 132 - timeWidth - 10 * Utility.getDensity(mContext),
				mMarginHeight / 2 + (fm.bottom + fm.ascent - fm.top) / 1,
				mInfoPaint);
	}

	/**
	 * 为向前跨章翻页提供进度位置
	 * 
	 * @return
	 */
	private int getLastPageBegin() {
		mContentPaint.setTextSize(mContentFontSize);
		mContentPaint.setColor(mContentFontColor);

		mCurrentPageBegin = mCurrentPageEnd = 0;
		while (mCurrentPageEnd < mChapterLength) {
			mCurrentPageBegin = mCurrentPageEnd;
			getPageContent();
		}

		return mCurrentPageBegin;
	}

	/**
	 * 根据当前起始进度，计算下一页将要展示的文字内容
	 * 
	 * @return 下一页的内容 Vector<String>
	 */
	private Vector<String> getPageContent() {
		mContentPaint.setTextSize(mContentFontSize);
		mContentPaint.setColor(mContentFontColor);
		String strParagraph = "";
		Vector<String> lines = new Vector<String>();
		while (lines.size() < mLineCount && mCurrentPageEnd < mChapterLength) {
			byte[] paraBuf = readForwardParagraph(mCurrentPageEnd);
			mCurrentPageEnd += paraBuf.length;// 每次读取后，记录结束点位置，该位置是段落结束位置
			try {
				strParagraph = new String(paraBuf, CHARSET);// 转换成制定UTF-8编码
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageDown->转换编码失败", e);
			}
			String strReturn = "";
			// 替换掉回车换行符
			if (strParagraph.indexOf("\r\n") != -1) {
				strReturn = "\r\n";
				strParagraph = strParagraph.replaceAll("\r\n", "");
			} else if (strParagraph.indexOf("\n") != -1) {
				strReturn = "\n";
				strParagraph = strParagraph.replaceAll("\n", "");
			}

			if (strParagraph.length() == 0) {
				lines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				// 画一行文字
				int nSize = mContentPaint.breakText(strParagraph, true,
						mContentWidth, null);
				lines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);// 得到剩余的文字
				// 超出最大行数则不再画
				if (lines.size() >= mLineCount) {
					break;
				}
			}
			// 如果该页最后一段只显示了一部分，则从新定位结束点位置
			if (strParagraph.length() != 0) {
				try {
					mCurrentPageEnd -= (strParagraph + strReturn)
							.getBytes(CHARSET).length;
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "pageDown->记录结束点位置失败", e);
				}
			}
		}
		return lines;
	}

	/**
	 * 得到上页的起始位置
	 */
	private int getPrePageBegin() {
		if (mCurrentPageBegin < 0)
			mCurrentPageBegin = 0;
		mContentPaint.setTextSize(mContentFontSize);
		mContentPaint.setColor(mContentFontColor);
		Vector<String> lines = new Vector<String>();
		String strParagraph = "";
		while (lines.size() < mLineCount && mCurrentPageBegin > 0) {
			Vector<String> paraLines = new Vector<String>();
			byte[] paraBuf = readBackwardParagraph(mCurrentPageBegin);
			mCurrentPageBegin -= paraBuf.length;// 每次读取一段后,记录开始点位置,是段首开始的位置
			try {
				strParagraph = new String(paraBuf, CHARSET);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageUp->转换编码失败", e);
			}
			strParagraph = strParagraph.replaceAll("\r\n", "");
			strParagraph = strParagraph.replaceAll("\n", "");
			// 如果是空白行，直接添加
			if (strParagraph.length() == 0) {
				paraLines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				// 将一段文字按照内容宽度进行分段
				int textCountInLine = mContentPaint.breakText(strParagraph,
						true, mContentWidth, null);
				paraLines.add(strParagraph.substring(0, textCountInLine));
				strParagraph = strParagraph.substring(textCountInLine);
			}
			lines.addAll(0, paraLines);
		}

		while (lines.size() > mLineCount) {
			try {
				mCurrentPageBegin += lines.get(0).getBytes(CHARSET).length;
				lines.remove(0);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "pageUp->记录起始点位置失败", e);
			}
		}
		mCurrentPageEnd = mCurrentPageBegin;// 上上一页的结束点等于上一页的起始点
		return mCurrentPageBegin;
	}

	/**
	 * 读取指定位置的上一个段落
	 * 
	 * @param nFromPos
	 * @return byte[]
	 */
	private byte[] readBackwardParagraph(int nFromPos) {
		loadTextBuffer(nFromPos);
		nFromPos = nFromPos - mTextBufferStart;
		int nEnd = nFromPos;
		int i;
		byte b0, b1;
		if (CHARSET.equals("UTF-16LE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = mTextBuffer.get(i);
				b1 = mTextBuffer.get(i + 1);
				if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}

		} else if (CHARSET.equals("UTF-16BE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = mTextBuffer.get(i);
				b1 = mTextBuffer.get(i + 1);
				if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}
		} else {
			i = nEnd - 1;
			while (i > 0) {
				b0 = mTextBuffer.get(i);
				if (b0 == 0x0a && i != nEnd - 1) {// 0x0a表示换行符
					i++;
					break;
				}
				i--;
			}
		}
		if (i < 0)
			i = 0;
		int nParaSize = nEnd - i;
		int j;
		byte[] buf = new byte[nParaSize];
		for (j = 0; j < nParaSize; j++) {
			buf[j] = mTextBuffer.get(i + j);
		}
		return buf;
	}

	/**
	 * 读取指定位置的下一个段落
	 * 
	 * @param nFromPos
	 * @return byte[]
	 */
	private byte[] readForwardParagraph(int nFromPos) {
		loadTextBuffer(nFromPos);
		nFromPos = nFromPos - mTextBufferStart;
		int nStart = nFromPos;
		int i = nStart;
		int length = mTextBuffer.capacity();
		byte b0, b1;
		// 根据编码格式判断换行
		if (CHARSET.equals("UTF-16LE")) {
			while (i < length - 1) {
				b0 = mTextBuffer.get(i++);
				b1 = mTextBuffer.get(i++);
				if (b0 == 0x0a && b1 == 0x00) {
					break;
				}
			}
		} else if (CHARSET.equals("UTF-16BE")) {
			while (i < length - 1) {
				b0 = mTextBuffer.get(i++);
				b1 = mTextBuffer.get(i++);
				if (b0 == 0x00 && b1 == 0x0a) {
					break;
				}
			}
		} else {
			while (i < length) {
				b0 = mTextBuffer.get(i++);
				if (b0 == 0x0a) {
					break;
				}
			}
		}
		int nParaSize = i - nStart;
		byte[] buf = new byte[nParaSize];
		for (i = 0; i < nParaSize; i++) {
			buf[i] = mTextBuffer.get(nFromPos + i);
		}
		return buf;
	}

	/**
	 * 调整当前进度
	 * 
	 * @param begin
	 * @return
	 */
	public void setCurrentPageProgress(int begin) {
		begin = Math.max(0, Math.min(begin, mChapterLength - 1));
		mCurrentPageBegin = mCurrentPageEnd = begin;
		tureNextPage();
		turePrePage();
	}

	/**
	 * 获取当前页面在本章中的进度
	 * 
	 * @return
	 */
	public int getCurrentPageProgress() {
		return Math.max(0, Math.min(mCurrentPageBegin, mChapterLength - 1));
	}

	/**
	 * 设置当前绘制背景
	 * 
	 * @param background
	 */
	public void setBackgroundBitmap(Bitmap background) {
		mBackgroundBitmap = Bitmap.createScaledBitmap(background,
				(int) Utility.getScreenWidth(mContext),
				(int) Utility.getScreenHeight(mContext), true);
	}

	/**
	 * 设置字号
	 * 
	 * @param fontSize
	 */
	public void setFontSize(int fontSize) {
		this.mContentFontSize = fontSize;
		mLineCount = (int) (mContentHeight / (fontSize * 1.5)) - 1;
	}

	/**
	 * 设置字色
	 * 
	 * @param fontColor
	 */
	public void setFontColor(int fontColor) {
		this.mContentFontColor = fontColor;
	}

	/**
	 * 释放
	 */
	public void release() {
		mTextBuffer.clear();
	}

	/**
	 * 获取进度信息的回调接口，用于获取数据计算本章页面在全本书中的进度
	 * 
	 * @author Calvin
	 * 
	 */
	public interface ChapterProgressCallback {
		/**
		 * 获取本章起始位置在全书的进度
		 * 
		 * @return
		 */
		public int getChapterStartProgress();

		/**
		 * 获取整本书的长度
		 * 
		 * @return
		 */
		public int getBookTotalLength();
	}

}
