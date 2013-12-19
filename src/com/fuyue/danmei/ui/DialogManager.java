package com.fuyue.danmei.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.fuyue.BaseApplicaion;
import com.fuyue.danmei.BaseActivity;
import com.fuyue.util.logging.Log;

/**
 * 控制Dialog统一展示及行为统计等的控制类，可以为非UI线程提供Dialog展示，由BaseActivity中获取栈顶引用，注意不要持有引用避免泄露。
 * 
 * @author Calvin.Xuw
 * 
 */
public class DialogManager {

	/** tag */
	private final String TAG = getClass().getSimpleName();
	/** debug开关 */
	private final boolean DEBUG = BaseApplicaion.DEBUG;
	/** 静态实例 */
	private static DialogManager sDialogManager;

	/** 获取实例 */
	public static DialogManager getInstance() {
		if (sDialogManager == null) {
			sDialogManager = new DialogManager();
		}
		return sDialogManager;
	}

	/**
	 * 生成指定Dialog
	 * 
	 * @param titleRes
	 *            标题,resId
	 * @param messageRes
	 *            描述,resId
	 * @param callback
	 *            交互事件回调
	 * @param showNow
	 *            是否立即显示
	 * @param btnTextRes
	 *            按钮文字,resId
	 * @return {@link DialogControlCallback}
	 */
	public DialogControlCallback createDialog(int titleRes, int messageRes,
			DialogStateCallback callback, boolean showNow, int... btnTextRes) {
		String title = BaseActivity.getTopActivity().getString(titleRes);
		String message = BaseActivity.getTopActivity().getString(messageRes);
		String[] btnText = new String[btnTextRes.length];
		for (int i = 0; i < btnTextRes.length; i++) {
			btnText[i] = BaseActivity.getTopActivity().getString(btnTextRes[i]);
		}
		return createDialog(title, message, callback, showNow, btnText);
	}

	/**
	 * 生成指定Dialog
	 * 
	 * @param title
	 *            标题
	 * @param message
	 *            描述
	 * @param callback
	 *            交互事件回调
	 * @param showNow
	 *            是否立即显示
	 * @param btnText
	 *            按钮文字
	 * @return {@link DialogControlCallback}
	 */
	public DialogControlCallback createDialog(String title, String message,
			DialogStateCallback callback, boolean showNow, String... btnText) {

		/**
		 * 获取Dialog
		 */
		final Dialog dialog = createRealDialog(title, message, callback,
				btnText);

		DialogControlCallback dialogControlCallback = new DialogControlCallback() {

			@Override
			public void dismiss() {
				/**
				 * 允许自非UI线程对Dialog进行控制。
				 */
				new Handler(BaseActivity.getTopActivity().getMainLooper())
						.post(new Runnable() {
							@Override
							public void run() {
								dialog.dismiss();
							}
						});
			}

			@Override
			public void setCancelable(boolean isCancelable) {
				dialog.setCancelable(isCancelable);
			}

			@Override
			public void cancel() {
				/**
				 * 允许自非UI线程对Dialog进行控制。
				 */
				new Handler(BaseActivity.getTopActivity().getMainLooper())
						.post(new Runnable() {
							@Override
							public void run() {
								dialog.cancel();
							}
						});
			}

			@Override
			public void show() {
				/**
				 * 允许自非UI线程对Dialog进行控制。
				 */
				new Handler(BaseActivity.getTopActivity().getMainLooper())
						.post(new Runnable() {
							@Override
							public void run() {
								dialog.show();
							}
						});
			}
		};

		if (showNow) {
			new Handler(BaseActivity.getTopActivity().getMainLooper())
					.post(new Runnable() {
						@Override
						public void run() {
							dialog.show();
						}
					});
		}
		return dialogControlCallback;
	}

	/**
	 * 实际生成创建Dialog的方法，可在其中自定义DialogView
	 * 
	 * @param title
	 *            标题
	 * @param message
	 *            描述
	 * @param callback
	 *            交互事件回调
	 * @param btnText
	 *            按钮文字
	 * @return 生成好的Dialog
	 */
	private Dialog createRealDialog(final String title, final String message,
			final DialogStateCallback callback, final String... btnText) {
		/*
		 * 对于按钮定义的数量及交互回调对象的检查
		 */
		if (btnText.length == 0) {
			if (DEBUG) {
				Log.w(TAG, "create a dialog without btn in it");
			}
		} else if (btnText.length > 3) {
			throw new IllegalArgumentException(
					"create a dialog with too many btn");
		} else if (callback == null) {
			throw new IllegalArgumentException(
					"create a dialog with btn , but did not pass the callback");
		}

		final OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callback.onClick(which);
			}
		};

		final OnCancelListener cancelListener = new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				callback.onCancel();
			}
		};

		/*
		 * 以Callable形式在UI线程中createDialog，从FutureTask再取出Callable中的返回对象
		 */
		Callable<Dialog> createDialogWithUIThread = new Callable<Dialog>() {

			@Override
			public Dialog call() throws Exception {
				/*
				 * 在此处可自定义DialogView
				 */
				AlertDialog dialog = new AlertDialog.Builder(
						BaseActivity.getTopActivity()).create();
				if (!TextUtils.isEmpty(title)) {
					dialog.setTitle(title);
				}
				if (!TextUtils.isEmpty(message)) {
					dialog.setMessage(message);
				}

				if (btnText.length > 0) {
					dialog.setButton(Dialog.BUTTON_POSITIVE, btnText[0],
							clickListener);
				}
				if (btnText.length > 1) {
					dialog.setButton(Dialog.BUTTON_NEGATIVE, btnText[1],
							clickListener);
				}
				if (btnText.length > 2) {
					dialog.setButton(Dialog.BUTTON_NEUTRAL, btnText[2],
							clickListener);
				}

				dialog.setOnCancelListener(cancelListener);
				return dialog;
			}
		};

		FutureTask<Dialog> futureTask = new FutureTask<Dialog>(
				createDialogWithUIThread);

		if (isInUIThread()) {
			futureTask.run();
		} else {
			new Handler(BaseActivity.getTopActivity().getMainLooper())
					.post(futureTask);
		}

		try {
			AlertDialog dialog = (AlertDialog) futureTask.get();
			return dialog;
		} catch (InterruptedException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		} catch (ExecutionException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}
		return null;
	}

	/**
	 * 判断是否在ui线程内
	 * 
	 * @return
	 */
	private boolean isInUIThread() {
		return Thread.currentThread().getId() == Looper.getMainLooper()
				.getThread().getId();
	}

	/**
	 * Dialog状态回调，包含click事件及cancel事件
	 * 
	 * @author Calvin
	 * 
	 */
	public interface DialogStateCallback {
		/**
		 * 点击Dialog按钮
		 * 
		 * @param which
		 *            {@link DialogInterface#BUTTON_POSITIVE},
		 *            {@link DialogInterface#BUTTON_NEGATIVE},
		 *            {@link DialogInterface#BUTTON_NEUTRAL}
		 */
		public void onClick(int which);

		/**
		 * Dialog取消
		 */
		public void onCancel();
	}

	/**
	 * 外部控制Dialog的回调接口。用于某个延迟取消Dialog行为。
	 * 
	 * @author Calvin
	 * 
	 */
	public interface DialogControlCallback {

		/**
		 * 显示Dialog，无需同步在UI线程执行
		 */
		public void show();

		/**
		 * 取消Dialog，不会触发 {@link DialogStateCallback#onCancel()}
		 */
		public void dismiss();

		/**
		 * 取消Dialog，触发 {@link DialogStateCallback#onCancel()}
		 */
		public void cancel();

		/**
		 * 设置是否可由用户自主取消
		 * 
		 * @param isCancelable
		 */
		public void setCancelable(boolean isCancelable);
	}
}
