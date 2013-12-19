package com.fuyue.danmei.read;

import android.app.Activity;
import android.view.Gravity;
import android.widget.Toast;

import com.fuyue.frame.R;
import com.fuyue.util.Utility;

public class BookToast {

	public static void showSaveToast(Activity activity) {
		Toast toast = new Toast(activity);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(activity.getLayoutInflater().inflate(R.layout.toast_save,
				null));
		toast.setGravity(Gravity.CENTER, 0,
				(int) (Utility.getScreenHeight(activity) / 3));
		toast.show();
	}
}
