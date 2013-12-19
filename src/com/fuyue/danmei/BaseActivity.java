package com.fuyue.danmei;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.fuyue.BaseApplicaion;
import com.fuyue.frame.R;
import com.fuyue.util.SdkVersionUtils;
import com.fuyue.util.model.AbstractModel;
import com.fuyue.util.model.AbstractModel.OnModelProcessListener;
import com.fuyue.util.model.ModelManageQueue;

/**
 * BaseActivity所有实现的Activity页面应将继承自此类
 * 
 * @author Xuwei
 * 
 */
public abstract class BaseActivity extends FragmentActivity implements
		OnModelProcessListener {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/**
	 * 窗口栈。
	 */
	public static LinkedList<BaseActivity> sActivityStack = new LinkedList<BaseActivity>();

	/** model管理类 */
	protected ModelManageQueue mModelManageQueue;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		sActivityStack.add(this);
		mModelManageQueue = new ModelManageQueue();
	}

	@Override
	protected void onResume() {
		// 移到顶端。
		sActivityStack.remove(this);
		sActivityStack.add(this);
		mModelManageQueue.pauseQueue(false);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mModelManageQueue.pauseQueue(true);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		sActivityStack.remove(this);
		System.gc();
		mModelManageQueue.clearQueue();
		super.onDestroy();
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		if (SdkVersionUtils.hasFroyo()) {
			overridePendingTransition(R.anim.activity_anim_push_left_in,
					R.anim.activity_anim_push_left_out);
		}
	}

	@Override
	public void finish() {
		sActivityStack.remove(this);
		super.finish();
		if (SdkVersionUtils.hasFroyo()) {
			overridePendingTransition(R.anim.activity_anim_push_right_in,
					R.anim.activity_anim_push_right_out);
		}
	}

	/**
	 * 提供原始finish方法
	 */
	public final void baseFinish() {
		super.finish();
	}

	/**
	 * 获取栈顶的activity
	 * 
	 * @return
	 */
	public static Activity getTopActivity() {
		if (sActivityStack.size() > 0) {
			return sActivityStack.getLast();
		}
		throw new NullPointerException(
				"need for a activity reference, but the stack is empty");
	}

	/**
	 * 清空activity栈
	 */
	public static void clearStack() {
		while (!sActivityStack.isEmpty()) {
			sActivityStack.poll().baseFinish();
		}
		sActivityStack.clear();
	}

	@Override
	public void onSuccess(AbstractModel model) {
		// do nothing
	}

	@Override
	public void onFailed(AbstractModel model, int errorCode) {
		// do nothing
	}
}
