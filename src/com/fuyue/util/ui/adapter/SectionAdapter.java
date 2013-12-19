package com.fuyue.util.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * section适配器
 * 
 * @author Calvin
 * 
 */
public abstract class SectionAdapter extends BaseAdapter {

	@Override
	public int getCount() {
		int sectionCount = sectionCount();
		int count = 0;
		for (int i = 0; i < sectionCount; i++) {
			count += getCountWithSection(i);
		}
		return count;
	}

	@Override
	public Object getItem(int position) {
		// 分配给各个section
		for (int i = 0; i < sectionCount(); i++) {
			if (getCountWithSection(i) > position) {
				return getItemWithSection(i, position);
			} else {
				position -= getCountWithSection(i);
			}
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// 分配给各个section
		for (int i = 0; i < sectionCount(); i++) {
			if (getCountWithSection(i) > position) {
				return getViewWithSection(i, position, convertView);
			} else {
				position -= getCountWithSection(i);
			}
		}
		return null;
	}

	/**
	 * 获取分栏条数
	 * 
	 * @return
	 */
	public abstract int sectionCount();

	/**
	 * 获取指定sectionId下的item数量
	 * 
	 * @param sectionId
	 * @return
	 */
	public abstract int getCountWithSection(int sectionId);

	/**
	 * 获取是定sectionId下的某个item
	 * 
	 * @param sectionId
	 * @param position
	 * @return
	 */
	public abstract Object getItemWithSection(int sectionId, int position);

	/**
	 * 获取指定sectionId下的section name
	 * 
	 * @param sectionId
	 * @return
	 */
	public abstract String getSectionName(int sectionId);

	/**
	 * 根据sectionId和postion获取View
	 * 
	 * @param sectionId
	 * @param position
	 * @param convertView
	 * @return
	 */
	public abstract View getViewWithSection(int sectionId, int position,
			View convertView);
}
