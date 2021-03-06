package com.deppon.dlpfile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

import com.litesuits.http.LiteHttpClient;
import com.litesuits.http.async.HttpAsyncExcutor;
import com.litesuits.http.data.HttpStatus;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.request.Request;
import com.litesuits.http.request.param.HttpParam;
import com.litesuits.http.response.Response;
import com.litesuits.http.response.handler.HttpResponseHandler;
/**
 * 首页界面.
 * 
 * @author 130126
 * 
 */
public class MainActivity extends Activity implements OnClickListener {
	// 反馈邮件地址
	public static String EMALADDRESS = "dpmobile@deppon.com";
	public static String EXIT_ACTION = "exitAction";
	// 登陆请求地址
	public static String LOGINURL = "http://app.deppon.com/center/checkLogin";
	// 文件解密地址
	public static String FILEURL = "http://app.deppon.com/center/decryptFile";
	// 登陆超时时间30秒
	public static final int TIMEOUT = 30;

	// false：正式环境.
	public static final boolean ISDEBUG = true;
	static {
		// 如果是调试
		if (ISDEBUG) {
			// 反馈地址
			EMALADDRESS = "lishuiqing110@163.com";
//			 LOGINURL = "http://10.224.70.132:8083/center/checkLogin";
//			 FILEURL = "http://10.224.70.132:8083/center/decryptFile";
			// 登陆地址
			LOGINURL = "http://192.168.67.47/center/checkLogin";
			// 文件解密地址
			FILEURL = "http://192.168.67.47/center/decryptFile";
		}
	}

	// 密码
	private EditText inputPass;
	// 用户名
	private EditText inputUser;
	// 登陆地址
	private Button buttonLogin;
	// 注册
	// private Button zhuceLogin;
	// 手机号
	String deviceId = null;
	// 记住密码
	private CheckBox remeberPassword;
	// 用户名
	private String name;
	// 密码
	private String pass;
	// 手机序列号
	private String serialId;
	// 弹出框
	private static final int DIALOG_KEY = 0;
	// 精度条
	private ProgressDialog dialog;

	/**
	 * 判断网络是否好用.
	 * 
	 * @param context
	 * @return
	 */
	public boolean isNetworkConnected(Context context) {
		try {
			// 判断网络情况
			if (context != null) {
				// 链接管理器
				ConnectivityManager mConnectivityManager = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mNetworkInfo = mConnectivityManager
						.getActiveNetworkInfo();
				// 返回网络状态
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

	public void onResume() {
		super.onResume();
	}

	Intent in = null;
	// 打开文件
	Uri openFile = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		// 用户密码
		inputPass = (EditText) findViewById(R.id.inputPass);
		// 用户名
		inputUser = (EditText) findViewById(R.id.inputName);
		// 手机临时存储变量
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		in = getIntent();
		//是否从详情页面返回来的首页.
		String isBack = in.getStringExtra("back");
		// 打开intent
		if (in != null && in.getData() != null) {
			openFile = in.getData();
		}
		// 是否记住密码
		remeberPassword = (CheckBox) findViewById(R.id.remember_password);
		// 得到本地存储的变量
		String remeber = mSharedPreferences.getString("remeber", "false");
		String pass = mSharedPreferences.getString("pass", "");
		String user = mSharedPreferences.getString("userId", "");
		buttonLogin = (Button) findViewById(R.id.buttonLogin);
		// zhuceLogin = (Button) findViewById(R.id.registLogin); 
		// 选择了记住密码
		if ("true".equals(remeber)) {
			remeberPassword.setChecked(true);
		}
		if (ISDEBUG) {
			// alert("使用的是测试版本！！");
		}

		// 注册事件
		// zhuceLogin.setOnClickListener(this);
		// 登陆事件
		buttonLogin.setOnClickListener(this);
		// 点击记住密码的按钮操作
		remeberPassword
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						// 如果是记住密码
						if (arg1) {
							SharedPreferences.Editor mEditor = mSharedPreferences
									.edit();
							// 保存用户名密码到本地存储
							mEditor.putString("remeber", "true");
							mEditor.putString("pass", inputPass.getText()
									.toString());
							mEditor.putString("userId", inputUser.getText()
									.toString());
							mEditor.commit();
						} else {
							// 没有记住密码就清空本地存储里面的变量
							SharedPreferences.Editor mEditor = mSharedPreferences
									.edit();
							mEditor.putString("remeber", "false");
							mEditor.putString("pass", "");
							mEditor.putString("userId", "");
							mEditor.commit();
						}
					}

				});

		// 如果设置了记住密码，就自动进行登录验证.
		if ("true".equals(remeber)) {
			inputPass.setText(pass);
			remeberPassword.setChecked(true);
			inputUser.setText(user);
			// 如果不是点击的返回按钮回退的，并且有文件，就直接登录.
			if(!"true".equals(isBack)){
				if (in != null && in.getData() != null) {
					go(openFile);
				} else {
					go(null);
				}
			}
		}
	}

	/**
	 * 记住密码
	 */
	private void savePass() {
		boolean arg1 = remeberPassword.isChecked();
		// 记住密码
		if (arg1) {
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putString("remeber", "true");
			mEditor.putString("pass", inputPass.getText().toString());
			mEditor.putString("userId", inputUser.getText().toString());
			mEditor.commit();
		}
		// 不记住密码
		else {
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putString("remeber", "false");
			mEditor.putString("pass", "");
			mEditor.putString("userId", "");
			mEditor.commit();
		}
	}

	private void resetPage() {
		buttonLogin.setEnabled(true);
		removeDialog(DIALOG_KEY);
	}

	/**
	 * 进行登陆操作
	 */
	private void go(Uri file) {
		if (!isNetworkConnected(this)) {
			alert("网络异常，请确认联网后重试");
		} else {
			TelephonyManager tm = (TelephonyManager) this
					.getSystemService(Context.TELEPHONY_SERVICE);
			savePass();
			// new MyListLoader(true, tm).execute("");
			name = inputUser.getText().toString();
			// 密码
			pass = inputPass.getText().toString();
			showDialog(DIALOG_KEY);
			buttonLogin.setEnabled(false);
			if ("".equals(name.trim())) {
				alert("请输入账号");
				resetPage();
			} else if ("".equals(pass.trim())) {
				alert("请输入密码");
				resetPage();
			} else { 
				final Uri _f = file;
				LiteHttpClient client = LiteHttpClient
						.getInstance(MainActivity.this);
				LoginArg arg = new LoginArg(serialId, name, pass);
				HttpAsyncExcutor asyncExcutor = new HttpAsyncExcutor();
				asyncExcutor.execute(client, new Request(LOGINURL, arg),
						new HttpResponseHandler() { 
							@Override
							protected void onFailure(Response res, HttpException e) {
								alert("登陆失败,请检查VPN是否已经开启");
							}

							@Override
							protected void onSuccess(Response response,
									HttpStatus arg1,
									com.litesuits.http.data.NameValuePair[] arg2) {
								String result = response.getString();
								if (!"40001".equals(result)) {
									console(result);
								}
								// 否则就进行文件解析处理.
								else {
									Map intent = new HashMap();
									intent.put("name", name);
									intent.put("pass", pass);
									intent.put("fileurl", _f);
									intent.put("serialId", serialId);

									Message mes = new Message();
									mes.what = 7;
									mes.obj = intent;
									myHandler.sendMessage(mes);
								}
							} 
						}); 
			}
		}
	}

	class LoginArg implements HttpParam {
		private String serial;
		private String userId;
		private String password;

		public LoginArg(String serial, String userId, String pass) {
			this.serial = serial;
			this.userId = userId;
			this.password = pass;
		}
	} 
	/**
	 * 注册.
	 */
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
			if ("".equals(inputUser.getText().toString().trim())) {
				alert("请输入账号");
				resetPage();
			} else if ("".equals(inputPass.getText().toString().trim())) {
				alert("请输入密码");
				resetPage();
			} else {
				go(null);
			}
		} /*
		 * else if (v.getId() == R.id.registLogin) { regist(); }
		 */
	}

	public Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				alert("对不起，该用户没有权限");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 2:
				alert("对不起，手机序列号不匹配");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 3:
				alert("对不起，密码错误");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 4:
				alert("对不起，参数错误");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 5:
				alert("没有安装相关软件，请安装软件后重试");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 6:
				alert("对不起，服务端异常或者网络异常，请稍候重试");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				break;
			case 7:
				Map m = (HashMap) msg.obj;
				Intent intent = new Intent(MainActivity.this,
						FileActivity.class);
				intent.putExtra("name", m.get("name") + "");
				intent.putExtra("pass", m.get("pass") + "");
				intent.putExtra("fileurl", (Uri) m.get("fileurl"));
				intent.putExtra("serialId", m.get("serialId") + "");
				removeDialog(DIALOG_KEY);
				buttonLogin.setEnabled(true);
				startActivity(intent);
				MainActivity.this.finish();
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

}
