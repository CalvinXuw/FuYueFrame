package com.fuyue.danmei.index.model;

import java.util.List;

import com.fuyue.util.net.parser.AbstractIfengXMLItem;

/**
 * 书架model
 * 
 * @author Calvin
 * 
 */
public class BookshelfModel extends AbstractIfengXMLItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7852675509484230203L;

	/** 书籍列表 */
	public List<BookshelfBookItem> mBookItems;
	/** 图片列表 */
	public List<BookshelfImageItem> mImageItems;

	/**
	 * 构造，录入解析路径
	 */
	public BookshelfModel() {
		addMappingRuleArrayField("mBookItems", "books", BookshelfBookItem.class);
		addMappingRuleArrayField("mImageItems", "images",
				BookshelfImageItem.class);
	}

	/**
	 * 单本书籍item，用于嵌套解析
	 * 
	 * @author Calvin
	 * 
	 */
	public static class BookshelfBookItem extends AbstractIfengXMLItem
			implements IBookshelfItem {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7053391378818784837L;
		/** 书籍id */
		public int mBookId;
		/** 书籍名称 */
		public String mBookName;
		/** 书籍缩略图 */
		public String mBookThumbImage;
		/** 书籍路径 */
		public String mBookFilePath;
		/** 书籍封面 */
		public String mCover;
		/** 书籍封底 */
		public String mBackCover;

		/**
		 * 构造，录入解析路径
		 */
		public BookshelfBookItem() {
			addMappingRuleField("mBookId", ":id");
			addMappingRuleField("mBookName", ":name");
			addMappingRuleField("mBookThumbImage", ":thumbimage");
			addMappingRuleField("mBookFilePath", ":filepath");
			addMappingRuleField("mCover", ":cover");
			addMappingRuleField("mBackCover", ":backcover");
		}
	}

	/**
	 * 单张图片item，用于嵌套解析
	 * 
	 * @author Calvin
	 * 
	 */
	public static class BookshelfImageItem extends AbstractIfengXMLItem
			implements IBookshelfItem {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2079734606753779371L;
		/** 图片名称 */
		public String mImageName;
		/** 图片路径 */
		public String mImageFilePath;

		/**
		 * 构造，录入解析路径
		 */
		public BookshelfImageItem() {
			addMappingRuleField("mImageName", ":name");
			addMappingRuleField("mImageFilePath", ":filepath");
		}
	}

	/**
	 * 书架类型的item
	 * 
	 * @author Calvin
	 * 
	 */
	public static interface IBookshelfItem {

	}
}
