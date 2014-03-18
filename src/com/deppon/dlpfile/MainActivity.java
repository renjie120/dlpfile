package com.deppon.dlpfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

/**
 * 首页界面.
 * 
 * @author 130126
 * 
 */
public class MainActivity extends Activity implements OnClickListener {
	public static String EMALADDRESS = "dpmobile@deppon.com";
	public static String LOGINURL = "http://app.deppon.com/center/checkLogin";
	public static String FILEURL = "http://app.deppon.com/center/decryptFile";

	// false：正式环境.
	public static final boolean ISDEBUG = true;
	static {
		if (ISDEBUG) {
			EMALADDRESS = "lishuiqing110@163.com";
			// LOGINURL = "http://10.224.70.10:8081/center/checkLogin";
			// FILEURL = "http://10.224.70.10:8081/center/decryptFile";
			LOGINURL = "http://192.168.67.47/center/checkLogin";
			FILEURL = "http://192.168.67.47/center/decryptFile";
		}
	}
	private EditText inputPass;
	private EditText inputUser;
	private Button buttonLogin;
	private Button zhuceLogin;
	String deviceId = null;
	private CheckBox remeberPassword;
	private String name;
	private String pass;
	private String serialId;
	private Uri openFile = null;
	private static final int DIALOG_KEY = 0;
	private ProgressDialog dialog;

	/**
	 * 异步加载界面.
	 * 
	 * @author 130126
	 * 
	 */
	private class MyListLoader extends AsyncTask<String, String, String> {

		private boolean showDialog;
		private TelephonyManager tm;

		public MyListLoader(boolean showDialog, TelephonyManager tm) {
			this.showDialog = showDialog;
			this.tm = tm;
		}

		@Override
		protected void onPreExecute() {
			if (showDialog) {
				showDialog(DIALOG_KEY);
			}
			buttonLogin.setEnabled(false);
		}

		public String doInBackground(String... p) {
			name = inputUser.getText().toString();
			pass = inputPass.getText().toString();
			Map params = new HashMap();
			deviceId = tm.getDeviceId();
			params.put("serial", deviceId);
			params.put("userId", name);
			params.put("password", pass);

			login(LOGINURL, params, "UTF-8");
			return "";
		}

		@Override
		public void onPostExecute(String Re) {
			if (showDialog) {
				removeDialog(DIALOG_KEY);
			}
			buttonLogin.setEnabled(true);
		}

		@Override
		protected void onCancelled() {
			if (showDialog) {
				removeDialog(DIALOG_KEY);
			}
			buttonLogin.setEnabled(true);
		}
	}

	/**
	 * 判断网络是否好用.
	 * 
	 * @param context
	 * @return
	 */
	public boolean isNetworkConnected(Context context) {
		try {
			if (context != null) {
				ConnectivityManager mConnectivityManager = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mNetworkInfo = mConnectivityManager
						.getActiveNetworkInfo();
				if (mNetworkInfo != null) {
					return mNetworkInfo.isAvailable();
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		inputPass = (EditText) findViewById(R.id.inputPass);

		inputUser = (EditText) findViewById(R.id.inputName);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		remeberPassword = (CheckBox) findViewById(R.id.remember_password);
		String remeber = mSharedPreferences.getString("remeber", "false");
		String pass = mSharedPreferences.getString("pass", "");
		String user = mSharedPreferences.getString("userId", "");
		buttonLogin = (Button) findViewById(R.id.buttonLogin);
		zhuceLogin = (Button) findViewById(R.id.registLogin);
		if ("true".equals(remeber)) {
			remeberPassword.setChecked(true);
		}
		if (ISDEBUG) {
			alert("使用的是测试版本！！");
		}
		remeberPassword
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							SharedPreferences.Editor mEditor = mSharedPreferences
									.edit();
							mEditor.putString("remeber", "true");
							mEditor.putString("pass", inputPass.getText()
									.toString());
							mEditor.putString("userId", inputUser.getText()
									.toString());
							mEditor.commit();
						} else {
							SharedPreferences.Editor mEditor = mSharedPreferences
									.edit();
							mEditor.putString("remeber", "false");
							mEditor.putString("pass", "");
							mEditor.putString("userId", "");
							mEditor.commit();
						}
					}

				});
		zhuceLogin.setOnClickListener(this);
		buttonLogin.setOnClickListener(this);
		Intent in = getIntent();
		if (in != null && in.getData() != null) {
			openFile = in.getData();
		}

		// 如果设置了记住密码，就自动进行登录验证.
		if ("true".equals(remeber)) {
			inputPass.setText(pass);
			remeberPassword.setChecked(true);
			inputUser.setText(user);
			// 如果有文件，就直接登录.
			if (in != null && in.getData() != null) {
				go();
			}
		}

	}

	private void savePass() {
		boolean arg1 = remeberPassword.isChecked();
		if (arg1) {
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putString("remeber", "true");
			mEditor.putString("pass", inputPass.getText().toString());
			mEditor.putString("userId", inputUser.getText().toString());
			mEditor.commit();
		} else {
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putString("remeber", "false");
			mEditor.putString("pass", "");
			mEditor.putString("userId", "");
			mEditor.commit();
		}
	}

	private void go() {
		if (!isNetworkConnected(this)) {
			alert("网络异常，请确认联网后重试");
		} else {
			TelephonyManager tm = (TelephonyManager) this
					.getSystemService(Context.TELEPHONY_SERVICE);
			savePass();
			new MyListLoader(true, tm).execute("");
		}
	}

	private void regist() {
		PackageInfo info;
		try {
			name = inputUser.getText().toString();
			TelephonyManager tm = (TelephonyManager) this
					.getSystemService(Context.TELEPHONY_SERVICE);
			deviceId = tm.getDeviceId();
			if (name == null || "".equals(name.trim())
					|| "null".equals(name.trim())) {
				alert("对不起，请填写要注册的OA工号");
			} else {
				Intent data = new Intent(Intent.ACTION_SENDTO);
				data.setData(Uri.parse("mailto:" + EMALADDRESS));
				info = getPackageManager().getPackageInfo(getPackageName(), 0);
				data.putExtra(Intent.EXTRA_SUBJECT, "DLP解密程序注册邮件");
				data.putExtra(Intent.EXTRA_TEXT, "OA用户:" + name + "<br>手机序号:"
						+ deviceId + "<br>程序版本号:" + info.versionName
						+ "<br><br>");
				startActivity(data);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void onClick(View v) {
		if (v.getId() == R.id.buttonLogin) {
			go();
		} else if (v.getId() == R.id.registLogin) {
			regist();
		}
	}

	public Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				alert("对不起，该用户没有权限");
				break;
			case 2:
				alert("对不起，手机序列号不匹配");
				break;
			case 3:
				alert("对不起，密码错误");
				break;
			case 4:
				alert("对不起，参数错误");
				break;
			case 5:
				alert("没有安装相关软件，请安装软件后重试");
				break;
			case 6:
				alert("对不起，服务端异常或者网络异常，请稍候重试");
				break;
			default:
				super.hasMessages(msg.what);
				break;
			}
		}
	};

	/**
	 * 退出程序的时候，删除临时文件夹里面的解密文件.
	 */
	protected void onDestroy() {
		super.onDestroy();
		clearDir();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_KEY: {
			dialog = new ProgressDialog(this);
			dialog.setMessage("正在登录,请稍候");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}
		}
		return null;
	}

	/**
	 * 弹出提示信息.
	 * 
	 * @param mess
	 */
	public void alert(String mess) {
		new AlertDialog.Builder(MainActivity.this).setTitle("提示")
				.setMessage(mess).setPositiveButton("确定", null).show();
	}

	private void clearDir() {
		File extDir = Environment.getExternalStorageDirectory();
		String dirName = extDir.getAbsolutePath() + "/dlpfiles";
		extDir = new File(dirName);
		if (extDir.exists()) {
			File[] subList = extDir.listFiles();
			for (File f : subList) {
				f.delete();
			}
		}
	}

	private String filename;

	/**
	 * 处理结果.
	 * 
	 * @param result
	 */
	public void console(String result) {
		if ("40002".equals(result)) {
			myHandler.sendEmptyMessage(1);
		} else if ("40003".equals(result)) {
			myHandler.sendEmptyMessage(2);
		} else if ("40004".equals(result)) {
			myHandler.sendEmptyMessage(3);
		} else if ("40005".equals(result)) {
			myHandler.sendEmptyMessage(4);
		}
	}

	/**
	 * 登陆验证
	 * 
	 * @param url
	 * @param params
	 * @param encoding
	 * @throws Exception
	 */
	public void login(final String url, final Map params, final String encoding) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			System.out.println("请求登陆地址：" + url);
			HttpPost httpost = new HttpPost(url);
			// 添加参数
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			if (params != null && params.keySet().size() > 0) {
				Iterator iterator = params.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry entry = (Entry) iterator.next();
					nvps.add(new BasicNameValuePair((String) entry.getKey(),
							(String) entry.getValue()));
				}
			}

			httpost.setEntity(new UrlEncodedFormEntity(nvps, encoding));
			HttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					entity.getContent(), encoding));
			// 如果没有登录成功，就弹出提示信息.
			String result = br.readLine();
			if (!"40001".equals(result)) {
				console(result);
			}
			// 否则就进行文件解析处理.
			else {
				Intent intent = new Intent(MainActivity.this,
						FileActivity.class);
				intent.putExtra("name", name);
				intent.putExtra("pass", pass);
				intent.putExtra("fileurl", openFile);
				if (serialId == null || "".equals(serialId.toString())) {
					TelephonyManager tm = (TelephonyManager) this
							.getSystemService(Context.TELEPHONY_SERVICE);
					serialId = tm.getDeviceId();
				}
				intent.putExtra("serialId", serialId);
				this.startActivity(intent);
			}
		} catch (Exception e) {
			e.printStackTrace();
			myHandler.sendEmptyMessage(6);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}
}
