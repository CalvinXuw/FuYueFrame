package com.fuyue.danmei.update;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.DialogInterface;

import com.fuyue.danmei.config.ClientInfoConfig;
import com.fuyue.danmei.ui.DialogManager;
import com.fuyue.frame.R;
import com.fuyue.util.Utility;
import com.fuyue.util.net.parser.AbstractIfengItem;
import com.fuyue.util.net.parser.AbstractIfengJSONItem;
import com.fuyue.util.net.requestor.AbstractRequestor;
import com.fuyue.util.net.requestor.WebRequestTask.RequestType;

/**
 * 客户端升级检测model
 * 
 * @author Calvin
 * 
 */
public class ClientUpdateRequertor extends AbstractRequestor {

	/** 应用号 */
	private static final String KEY_AID = "aid";
	/** 渠道号 */
	private static final String KEY_CID = "cid";

	/**
	 * 构造
	 * 
	 * @param context
	 * @param listener
	 */
	public ClientUpdateRequertor(Context context,
			OnModelProcessListener listener) {
		super(context, listener);
		setAutoParseClass(ClientUpdateItem.class);
		setRequestType(RequestType.GET);
	}

	/** 客户端更新item */
	private ClientUpdateItem mClientUpdateItem;

	@Override
	@Deprecated
	public void request() {

	}

	/**
	 * 自动检查更新，加以间隔限制
	 */
	public void checkUpdateAuto() {
		if (ClientInfoConfig.getInstance(mContext).shouldCheckUpdate()) {
			super.request();
		}
	}

	/**
	 * 请求检查更新，跨过请求间隔限制
	 */
	public void checkUpdate() {
		super.request();
	}

	/**
	 * 弹出更新对话框
	 */
	public void showUpdateDialog() {
		if (mClientUpdateItem == null) {
			// 无可用更新
			return;
		}

		StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append(String.format(
				mContext.getString(R.string.update_version_compare),
				mClientUpdateItem.mNewVersion,
				mClientUpdateItem.mCurrentVersion));
		messageBuffer.append("\n");
		messageBuffer.append(mClientUpdateItem.mDesc);

		DialogManager.getInstance().createDialog(
				mContext.getString(R.string.update_title),
				messageBuffer.toString(),
				new DialogManager.DialogStateCallback() {

					@Override
					public void onClick(int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							ClientUpdateProcesser.getInstance(mContext,
									mClientUpdateItem.mUrl,
									mClientUpdateItem.mNewVersion)
									.executeSyncTask();
						}
					}

					@Override
					public void onCancel() {

					}
				}, true, mContext.getString(R.string.update_update),
				mContext.getString(R.string.dialog_cancel));
	}

	/**
	 * 获取更新item，如果没有可用更新，则返回null
	 * 
	 * @return
	 */
	public ClientUpdateItem getClientUpdateItem() {
		return mClientUpdateItem;
	}

	@Override
	protected List<NameValuePair> getRequestHeaders() {
		return null;
	}

	@Override
	protected List<NameValuePair> getRequestParams() {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair(KEY_AID, Utility
				.getApplicationMetaData(mContext, "appId") + ""));
		params.add(new BasicNameValuePair(KEY_CID, Utility
				.getApplicationMetaData(mContext, "channelId") + ""));
		return params;
	}

	@Override
	protected List<NameValuePair> getExtraParams() {
		return null;
	}

	@Override
	protected void handleResult(AbstractIfengItem item) {
		ClientUpdateItem clientUpdateItem = (ClientUpdateItem) item;

		int currentVersionCode = Utility.getAppVersionCode(mContext);

		// 版本检测
		if (clientUpdateItem.mNewVersionCode > currentVersionCode) {
			mClientUpdateItem = clientUpdateItem;
			mClientUpdateItem.mCurrentVersion = Utility
					.getAppVersionName(mContext);
		}
	}

	@Override
	protected String getRequestUrl() {
		return "http://113.10.155.228:8085/";
	}

	/**
	 * 解析客户端升级接口
	 * 
	 * @author Calvin
	 * 
	 */
	public static class ClientUpdateItem extends AbstractIfengJSONItem {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8899888216299305472L;

		/** 版本名 */
		public String mNewVersion;
		/** 版本号 */
		public int mNewVersionCode;

		/** 当前版本名 */
		public String mCurrentVersion;

		/** 客户端下载地址 */
		public String mUrl;
		/** 更新描述 */
		public String mDesc;

		/**
		 * 构造
		 */
		public ClientUpdateItem() {
			addMappingRuleField("mNewVersion", "version_info/version");
			addMappingRuleField("mNewVersionCode", "version_info/versioncode");
			addMappingRuleField("mUrl", "version_info/download_url");
			addMappingRuleField("mDesc", "version_info/update_info");
		}
	}

}
