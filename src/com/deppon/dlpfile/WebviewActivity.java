package com.deppon.dlpfile;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * 文件解密具体操作界面.
 * 
 * @author 130126
 * 
 */
public class WebviewActivity extends Activity {
	private WebView view;
	private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.webview);
		view = (WebView) findViewById(R.id.content);
		WebSettings webSettings = view.getSettings();
		webSettings.setJavaScriptEnabled(true);
		// view.addJavascriptInterface(new Object() {
		// public void clickOnAndroid() {
		// mHandler.post(new Runnable() {
		// public void run() {
		// // view.loadUrl("javascript:wave()");
		// Toast.makeText(WebviewActivity.this, "测试调用java",
		// Toast.LENGTH_LONG).show();
		// }
		// });
		// }
		// }, "demo");

		view.addJavascriptInterface(new Object() { 
			public void clickOnAndroid(final String i) { 
				mHandler.post(new Runnable() { 
					public void run() {  
						Toast.makeText(WebviewActivity.this,
								"测试调用java" +i,
								Toast.LENGTH_LONG).show(); 
					} 
				}); 
			} 
		}, "demo");

		view.setWebViewClient(new DIYWebViewClient());
		view.loadUrl("http://10.224.70.10:8081");

	}

	private class DIYWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
}
