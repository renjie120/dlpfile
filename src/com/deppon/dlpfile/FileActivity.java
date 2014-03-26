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
	// 返回按钮
	private Button callbackButton;
	// 用户名
	private String name;
	// 密码
	private String pass;
	// 最大文件大小.
	private long maxFileLength = 2 * 1024 * 1024;
	// 提示文件超过1M大小
	private long bigFileLength = 1 * 1024 * 1024;
	// 文件名
	private Uri fileuri;
	private String serialId;
	// 全部过程超时时间为2分钟
	public static final int TIMEOUT = 120;
	// 等待50秒之后，解密文件没有被第三方程序加载到内存中就直接删除
	public static final int WAIT = 120;
	private boolean chaoshi = false;
	public Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				chaoshi = true;
				alert("请求超时，请稍候重试。");
				// removeDialog(DIALOG_KEY);
				break;
			case 2:
				alert("程序出现异常,请反馈。");
				break;
			case 3:
				alert("不支持超过2M的文件，建议通过PC端访问");
				break;
			case 5:
				alert("文件大于1M,可能耗时比较长,是否继续进行?");
				break;
			case 4:
				removeDialog(DIALOG_KEY);
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
		// 文件
		Uri uri = Uri.fromFile(extDir);
		// 文件名
		String fileName = extDir.getAbsolutePath();
		// pm
		PackageManager pm = this.getPackageManager();

		try {
			// 初始化
			Intent intent = new Intent("android.intent.action.VIEW");
			intent.addCategory("android.intent.category.DEFAULT");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// pdf文件
			if (fileName.endsWith(".pdf")) {
				intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.setClass(this,
						org.vudroid.pdfdroid.PdfViewerActivity.class);
			}
			// gif图片
			else if (fileName.endsWith(".gif")) {
				intent.setDataAndType(uri, "image/gif");
			}
			// jpg图片
			else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
				intent.setDataAndType(uri, "image/jpeg");
			}
			// png图片
			else if (fileName.endsWith(".png")) {
				intent.setDataAndType(uri, "image/png");
			}
			// 是否超时
			if (!chaoshi) {
				myHandler.sendEmptyMessage(4);
				this.startActivity(intent);
				FileActivity.this.finish();
				Thread.sleep(WAIT * 1000);
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
		// 文件全名
		File fullFilename = null;
		try {
			// 目录
			String dirName = extDir.getAbsolutePath() + "/dlpfiles";
			// 新地址
			File newExtDir = new File(dirName);
			// 目录不存在就创建
			if (!newExtDir.exists()) {
				newExtDir.mkdir();
				// newExtDir.setWritable(true);
			}
			// 如果已经存在就删除
			fullFilename = new File(newExtDir, filename);
			// 删除临时文件夹里面文件
			if (fullFilename.exists()) {
				fullFilename.deleteOnExit();
			}
			// 写文件流
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
					new FileOutputStream(fullFilename));
			// 写进去
			bufferedOutputStream.write(byts);
			// 关闭文件流
			bufferedOutputStream.close();

			// 打开文件
			openFileWithWord(fullFilename);
		} catch (IOException e) {
			// 补货异常
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
			// 得到返回对象
			HttpEntity entity = response.getEntity();
			// 进行处理
			if (entity != null) {
				return EntityUtils.toByteArray(entity);
			}
			// 不进行处理
			else {
				return null;
			}
		}
	};
	// 弹出框
	private static final int DIALOG_KEY = 0;
	// dialog变量
	private ProgressDialog dialog;
	// 文件大小
	private long fileLen = 0;

	// 异步任务
	private class MyListLoader extends AsyncTask<String, String, String> {
		// 显示dialog
		private boolean showDialog;
		// 文件名
		private String name = null;
		// 密码
		private String pass = null;
		// 序号
		private String serialId = null;
		// wenjian
		private Uri uri = null;

		// 构造函数
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
			// 显示
			if (showDialog) {
				showDialog(DIALOG_KEY);
			}
		}

		/**
		 * 后台
		 */
		public String doInBackground(String... p) {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			try {
				// 先删除一次目录！
				deleteFile();
				// 文件存在
				if (fileuri != null) {
					// 得到sd卡
					File extDir = Environment.getExternalStorageDirectory();
					String filepath = fileuri.getPath();
					File oldFile = new File(filepath);
					fileLen = oldFile.length();
					if (fileLen > maxFileLength) {
						myHandler.sendEmptyMessage(3);
					} else {
						if (fileLen > maxFileLength) {
							alert("文件大于1M,可能耗时比较长,是否继续进行?");
						}
						String filename = oldFile.getName();
						// 得到请求
						HttpPost httppost = new HttpPost(MainActivity.FILEURL);
						// 添加文件
						FileBody bin = new FileBody(oldFile);
						// 构造请求
						MultipartEntityBuilder builder = MultipartEntityBuilder
								.create().addPart("file", bin);
						// 正式环境上面，继续添加几个参数.
						StringBody namebody = new StringBody(name,
								ContentType.TEXT_PLAIN);
						// 添加参数
						StringBody passbody = new StringBody(pass,
								ContentType.TEXT_PLAIN);
						// 添加参数
						StringBody filenamebody = new StringBody(filename,
								ContentType.TEXT_PLAIN);
						// 添加参数
						StringBody serialIdbody = new StringBody(serialId,
								ContentType.TEXT_PLAIN);
						// 添加参数
						builder = builder.addPart("userId", namebody)
								.addPart("password", passbody)
								.addPart("filename", filenamebody)
								.addPart("serial", serialIdbody);
						HttpEntity reqEntity = builder.build();
						httppost.setEntity(reqEntity);
						// 调用请求，将返回结果保存到本地文件.
						byte[] charts = httpclient.execute(httppost, handler);
						// 保存到本地文件流.
						if (filename.endsWith(".docx")
								|| filename.endsWith(".xls")
								|| filename.endsWith(".xlsx")
								|| filename.endsWith(".ppt")
								|| filename.endsWith(".pptx")
								|| filename.endsWith(".doc")) {
							// 如果是office系列就进行文件名转换
							filename = filename.replace(".", "_") + ".pdf";
						}
						// 保存返回的二进制流
						saveFile(extDir, filename, charts);
					}
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
		// 设置布局文件
		setContentView(R.layout.read_content);
		// 得到页面变量
		Intent intent = getIntent();
		name = intent.getStringExtra("name");
		pass = intent.getStringExtra("pass");
		serialId = intent.getStringExtra("serialId");
		// 文件流
		fileuri = (Uri) intent.getParcelableExtra("fileurl");
		System.out.println("file---name=" + name + ",serialId=" + serialId
				+ ",fileuri=" + fileuri);

		// 按钮
		callbackButton = (Button) findViewById(R.id.callback_button);
		callbackButton.setOnClickListener(this);
		// commitFile();
		// 进行一次处理之后，就设置name为空。

		if (fileuri != null) {
			final MyListLoader task = new MyListLoader(true, name, pass,
					serialId, fileuri);
			task.execute("");
			new Thread() {
				public void run() {
					try {
						/**
						 * 在这里你可以设置超时的时间
						 * 切记：这段代码必须放到线程中执行，因为不放单独的线程中执行的话该方法会冻结UI线程
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
	}

	/**
	 * 弹出框
	 * 
	 * @param mess
	 */
	public void alert(String mess) {
		new AlertDialog.Builder(FileActivity.this).setTitle("提示")
				.setMessage(mess).setPositiveButton("确定", null).show();
	}

	/**
	 * 删除文件.
	 */
	private void deleteFile() {
		try {
			File extDir = Environment.getExternalStorageDirectory();
			String dirName = extDir.getAbsolutePath() + "/dlpfiles";
			File newExtDir = new File(dirName);
			if (newExtDir.exists()) {
				File[] fls = newExtDir.listFiles();
				for (File f : fls) {
					f.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			myHandler.sendEmptyMessage(2);
		}
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
				// 得到程序版本
				PackageInfo info = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				data.putExtra(Intent.EXTRA_SUBJECT, "关于德邦DLP反馈的邮件");
				data.putExtra(Intent.EXTRA_TEXT, "DLP附件解密使用问题反馈：<br>OA用户:"
						+ name + "<br>手机序号:" + serialId + "<br>程序版本号:"
						+ info.versionName + "<br><br>问题如下：<br><br>");
				startActivity(data);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
