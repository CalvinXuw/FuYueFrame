package com.fuyue.util.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

/**
 * 组合形式的列表adapter，子类需要实现所有抽象方法，且需要在 {@link #getItem(int)}中返回子项item实现所需的数据源
 * 
 * @author Calvin
 * 
 */
public abstract class ComposeStyleAdapter extends BaseAdapter {

	/**
	 * 获取包含的样式数量
	 * 
	 * @return
	 */
	public abstract int getSubStyleCount();

	/**
	 * 根据样式获取不填充数据的原始View
	 * 
	 * @param style
	 * @return
	 */
	public abstract View getSubStyleOrginView(int style);

	/**
	 * 根据样式类型进行分别实现子项View
	 * 
	 * @param converView
	 * @param style
	 * @param itemRes
	 * @return
	 */
	public abstract View getSubStyleItemView(View converView, int style,
			Object itemRes);

	/**
	 * 获取指定位置的视图样式
	 * 
	 * @param position
	 * @return
	 */
	public abstract int getItemSubStyle(int position);

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout container = null;
		if (convertView == null) {
			container = new LinearLayout(parent.getContext());
			container.setOrientation(LinearLayout.VERTICAL);
			LayoutParams layoutParams = new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			container.setLayoutParams(layoutParams);

			for (int i = 0; i < getSubStyleCount(); i++) {
				View subItemView = getSubStyleOrginView(i);
				subItemView.setId(i);
				container.addView(subItemView);
			}
		} else {
			container = (LinearLayout) convertView;
		}

		int currentStyle = getItemSubStyle(position);
		for (int i = 0; i < container.getChildCount(); i++) {
			View subItemView = container.getChildAt(i);
			if (subItemView.getId() != currentStyle) {
				subItemView.setVisibility(View.GONE);
			} else {
				subItemView.setVisibility(View.VISIBLE);
				getSubStyleItemView(subItemView, currentStyle,
						getItem(position));
			}
		}

		return container;
	}
}
