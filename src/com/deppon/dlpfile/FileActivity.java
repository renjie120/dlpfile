package com.deppon.dlpfile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * 文件解密具体操作界面.
 * 
 * @author 130126
 * 
 */
public class FileActivity extends Activity implements OnClickListener {
	private Button callbackButton;
	private String name;
	private String pass;
	private Uri fileuri;
	private String serialId;
	// 全部过程超时时间为30秒钟
	public static final int TIMEOUT = 30;
	// 等待多少秒之后，解密文件没有被第三方程序加载到内存中就直接删除
	public static final int WAIT = 20;
	private boolean chaoshi = false;
	public Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				chaoshi = true;
				alert("请求超时，请稍候重试！");
				removeDialog(DIALOG_KEY);
				break;
			case 2:
				alert("程序出现异常,请反馈,异常编号0034！");
				break;
			default:
				super.hasMessages(msg.what);
				break;
			}
		}
	};

	/**
	 * 打开word文件.
	 * 
	 * @param extDir
	 * @param filename
	 */
	private void openFileWithWord(File extDir) {
		Uri uri = Uri.fromFile(extDir);
		String fileName = extDir.getAbsolutePath();
		PackageManager pm = this.getPackageManager();
		// Intent mainIntent = new Intent();
		// Intent mainIntent = new Intent("android.intent.action.VIEW");
		// 以下是对Intent进行过滤，
		// // mainIntent.setAction(Intent.ACTION_VIEW);
		// mainIntent.addCategory("android.intent.category.DEFAULT");
		// mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// if (fileName.endsWith(".pdf")) {
		// mainIntent.setDataAndType(uri, "application/pdf");
		// } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
		// mainIntent.setDataAndType(uri, "application/msword");
		// } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
		// mainIntent.setDataAndType(uri, "vnd.ms-powerpoint");
		// } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
		// mainIntent.setDataAndType(uri, "application/vnd.ms-excel");
		// } else if (fileName.endsWith(".gif")) {
		// mainIntent.setDataAndType(uri, "image/gif");
		// } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
		// mainIntent.setDataAndType(uri, "image/jpeg");
		// }
		// // 或许过滤后的Activities信息。此处需要注意一下的是第二个参数静态常量的设置影响你获取的结果，
		// // 我这里设置的默认，也就是全部。还可以设置只获取系统的，等等
		// List resolveInfos = pm.queryIntentActivities(mainIntent,
		// PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
		//
		// for (Iterator iterator = resolveInfos.iterator();
		// iterator.hasNext();) {
		// ResolveInfo resolveInfo = (ResolveInfo) iterator.next();
		// String nameapp = resolveInfo.activityInfo.name;
		// String namepack = resolveInfo.activityInfo.packageName;
		// String lable = (String) resolveInfo.loadLabel(pm);
		// System.out.println("应用程序包名：" + namepack + " 应用程序入口Activity:"
		// + nameapp + " 程序名：" + lable);
		// }
		// mainIntent.setPackage("com.mobisystems.office");
		// // mainIntent.setComponent(new
		// ComponentName("com.mobisystems.office",
		// "Activity:com.mobisystems.office.pdf.PdfViewerLauncher"));
		// this.startActivity(mainIntent);

		try { 
			if (!(fileName.endsWith(".doc") ||fileName.endsWith(".ppt") || fileName.endsWith(".xls"))) {
				Intent intent = new Intent("android.intent.action.VIEW");
				intent.addCategory("android.intent.category.DEFAULT");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				if (fileName.endsWith(".pdf")) {
					intent.setDataAndType(uri, "application/pdf");
				} else if (fileName.endsWith(".docx")) {
					// intent.setPackage("com.mobisystems.office");
					intent.setDataAndType(uri, "application/msword");
				} else if (fileName.endsWith(".ppt")
						|| fileName.endsWith(".pptx")) { 
					intent.setDataAndType(uri, "vnd.ms-powerpoint");
				} else if (fileName.endsWith(".xlsx")) { 
					intent.setDataAndType(uri, "application/vnd.ms-excel");
				} else if (fileName.endsWith(".gif")) {
					intent.setDataAndType(uri, "image/gif");
				} else if (fileName.endsWith(".jpeg")
						|| fileName.endsWith(".jpg")) {
					intent.setDataAndType(uri, "image/jpeg");
				}
				if (!chaoshi) {
					this.startActivity(intent);
					Thread.sleep(WAIT * 1000);
				}
			} else {
				if (fileName.endsWith(".doc")) {
					Intent intent = new Intent();
					intent.setClass(FileActivity.this, Word2003Read.class);
					intent.putExtra("filepath", fileName);
					startActivity(intent);
				} else if (fileName.endsWith(".xls")) {
					Intent intent = new Intent();
					intent.setClass(FileActivity.this, Excel2003Read.class);
					intent.putExtra("filepath", fileName);
					startActivity(intent);
				}else if (fileName.endsWith(".ppt")) {
					Intent intent = new Intent();
					intent.setClass(FileActivity.this, Excel2003Read.class);
					intent.putExtra("filepath", fileName);
					startActivity(intent);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			alert("没有安装相关软件，请安装软件后重试");
		} finally {
			deleteFile();
		}
	}

	/**
	 * 保存文件.
	 * 
	 * @param extDir
	 * @param filename
	 * @param byts
	 */
	private void saveFile(File extDir, String filename, byte[] byts) {
		File fullFilename = null;// = new File(extDir, filename);
		try {
			String dirName = extDir.getAbsolutePath() + "/dlpfiles";
			File newExtDir = new File(dirName);
			if (!newExtDir.exists()) {
				newExtDir.mkdir();
				newExtDir.setWritable(true);
			}
			fullFilename = new File(newExtDir, filename);
			if (fullFilename.exists()) {
				fullFilename.deleteOnExit();
			}
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
					new FileOutputStream(fullFilename));
			bufferedOutputStream.write(byts);
			bufferedOutputStream.close();

			openFileWithWord(fullFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
	}

	/**
	 * 对文件进行解密.
	 */

	// 定义回调函数.
	ResponseHandler<byte[]> handler = new ResponseHandler<byte[]>() {
		public byte[] handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toByteArray(entity);
			} else {
				return null;
			}
		}
	};

	private static final int DIALOG_KEY = 0;
	private ProgressDialog dialog;

	private class MyListLoader extends AsyncTask<String, String, String> {

		private boolean showDialog;
		private String name = null;
		private String pass = null;
		private String serialId = null;
		private Uri uri = null;

		public MyListLoader(boolean showDialog, String name, String pass,
				String serialId, Uri uri) {
			this.showDialog = showDialog;
			this.name = name;
			this.pass = pass;
			this.uri = uri;
			this.serialId = serialId;
		}

		@Override
		protected void onPreExecute() {
			if (showDialog) {
				showDialog(DIALOG_KEY);
			}
		}

		public String doInBackground(String... p) {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			try {
				// 先删除一次目录！
				deleteFile();
				if (fileuri != null) {
					File extDir = Environment.getExternalStorageDirectory();
					String filepath = fileuri.getPath();
					File oldFile = new File(filepath);
					String filename = oldFile.getName();
					HttpPost httppost = new HttpPost(MainActivity.FILEURL);
					FileBody bin = new FileBody(oldFile);
					MultipartEntityBuilder builder = MultipartEntityBuilder
							.create().addPart("file", bin);
					// 正式环境上面，继续添加几个参数.
					if (!MainActivity.ISDEBUG) {
						StringBody namebody = new StringBody(name,
								ContentType.TEXT_PLAIN);
						StringBody passbody = new StringBody(pass,
								ContentType.TEXT_PLAIN);
						StringBody serialIdbody = new StringBody(serialId,
								ContentType.TEXT_PLAIN);
						builder = builder.addPart("userId", namebody)
								.addPart("password", passbody)
								.addPart("serial", serialIdbody);
					}
					HttpEntity reqEntity = builder.build();
					httppost.setEntity(reqEntity);
					// 调用请求，将返回结果保存到本地文件.
					byte[] charts = httpclient.execute(httppost, handler);
					// 保存到本地文件流.
					saveFile(extDir, filename, charts);
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// 关闭请求
				httpclient.getConnectionManager().shutdown();
			}
			return "";
		}

		@Override
		public void onPostExecute(String Re) {
			if (showDialog) {
				removeDialog(DIALOG_KEY);
			}
		}

		@Override
		protected void onCancelled() {
			if (showDialog) {
				removeDialog(DIALOG_KEY);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_KEY: {
			dialog = new ProgressDialog(this);
			dialog.setMessage("正在解密,请稍候");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_content);
		Intent intent = getIntent();
		name = intent.getStringExtra("name");
		pass = intent.getStringExtra("pass");
		serialId = intent.getStringExtra("serialId");
		fileuri = (Uri) intent.getParcelableExtra("fileurl");
		callbackButton = (Button) findViewById(R.id.callback_button);
		callbackButton.setOnClickListener(this);
		// commitFile();
		// 进行一次处理之后，就设置name为空。
		final MyListLoader task = new MyListLoader(true, name, pass, serialId,
				fileuri);
		task.execute("");
		new Thread() {
			public void run() {
				try {
					/**
					 * 在这里你可以设置超时的时间 切记：这段代码必须放到线程中执行，因为不放单独的线程中执行的话该方法会冻结UI线程
					 * 直接导致onPreExecute()方法中的弹出框不会立即弹出。
					 */
					task.get(TIMEOUT, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				} catch (TimeoutException e) {
					task.cancel(true);
					myHandler.sendEmptyMessage(1);
				}// 请求超时
			};
		}.start();
	}

	public void alert(String mess) {
		new AlertDialog.Builder(FileActivity.this).setTitle("提示")
				.setMessage(mess).setPositiveButton("确定", null).show();
	}

	/**
	 * 删除文件.
	 */
	private void deleteFile() {
		// try {
		// File extDir = Environment.getExternalStorageDirectory();
		// String dirName = extDir.getAbsolutePath() + "/dlpfiles";
		// File newExtDir = new File(dirName);
		// if (newExtDir.exists()) {
		// File[] fls = newExtDir.listFiles();
		// for (File f : fls) {
		// f.delete();
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// myHandler.sendEmptyMessage(2);
		// }
	}

	public void onDestroy() {
		super.onDestroy();
		deleteFile();
	}

	@Override
	public void onClick(View v) {
		try {
			if (v.getId() == R.id.callback_button) {
				Intent data = new Intent(Intent.ACTION_SENDTO);
				data.setData(Uri.parse("mailto:" + MainActivity.EMALADDRESS));
				PackageInfo info = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				data.putExtra(Intent.EXTRA_SUBJECT, "关于德邦DLP反馈的邮件");
				data.putExtra(Intent.EXTRA_TEXT, "DLP附件解密使用问题反馈：<br>OA用户:"
						+ name + "<br>手机序号:" + serialId + "<br>程序版本号:"
						+ info.versionName + "<br><br>问题如下：<br><br>");
				startActivity(data);
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
