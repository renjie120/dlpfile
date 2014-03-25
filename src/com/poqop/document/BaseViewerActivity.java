package com.poqop.document;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.deppon.dlpfile.FileActivity;
import com.deppon.dlpfile.R;
import com.poqop.document.events.CurrentPageListener;
import com.poqop.document.events.DecodingProgressListener;
import com.poqop.document.models.CurrentPageModel;
import com.poqop.document.models.DecodingProgressModel;
import com.poqop.document.models.ZoomModel;
import com.poqop.document.views.PageViewZoomControls;

public abstract class BaseViewerActivity extends Activity implements
		DecodingProgressListener, CurrentPageListener {
	private static final int MENU_EXIT = 0;
	private static final int MENU_GOTO = 1;
	private static final int DIALOG_GOTO = 0;
	private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";
	private DecodeService decodeService;
	private DocumentView documentView;
	private ViewerPreferences viewerPreferences;
	private Toast pageNumberToast;
	private CurrentPageModel currentPageModel;

	private static final int MAX_VALUE = 3800; // 设置放大的最大倍数
	private static final float MULTIPLIER = 400.0f;
	public ZoomModel zoomModel;

	float lastX;
	float lastY;
	float magnify = 1.0f; // 放大
	float reduce = 1.0f; // 缩小
	LinearLayout zoom; // 自定义一个linearlayout用来存放两个缩放按钮
	public Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				BaseViewerActivity.this.finish();
				break;
			default:
				super.hasMessages(msg.what);
				break;
			}
		}
	};

	public float[] getScreen2() {
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		return new float[] { dm.widthPixels, dm.heightPixels };
	}
	
	public void alert(String mess) {
		new AlertDialog.Builder(BaseViewerActivity.this).setTitle("提示")
				.setMessage(mess).setPositiveButton("确定", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Intent intent = new Intent(BaseViewerActivity.this,
								FileActivity.class);
						startActivity(intent);
					}
					
				} ).show();
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDecodeService();
		final ZoomModel zoomModel = new ZoomModel();
		final DecodingProgressModel progressModel = new DecodingProgressModel();
		progressModel.addEventListener(this);
		currentPageModel = new CurrentPageModel();
		currentPageModel.addEventListener(this);
		documentView = new DocumentView(this, zoomModel, progressModel,
				currentPageModel);
		zoomModel.addEventListener(documentView);
		documentView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));
		decodeService.setContentResolver(getContentResolver());
		decodeService.setContainerView(documentView);
		documentView.setDecodeService(decodeService);
		try {
			decodeService.open(getIntent().getData());
			this.zoomModel = zoomModel;
			ImageView zoomIn, zoomOut, zoomClose;
			viewerPreferences = new ViewerPreferences(this);

			/*
			 * 放大按鈕圖片進行放大
			 */
			zoom = new LinearLayout(this);
			zoom.setVisibility(View.GONE);
			zoom.setOrientation(LinearLayout.HORIZONTAL);

			zoom.setLayoutParams(new LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			zoomOut = new ImageView(this);
			zoomOut.setImageResource(R.drawable.gallery_zoom_out_touch);
			zoomOut.setLayoutParams(new LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			zoomOut.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					System.out.println("寬" + zoom.getWidth());
					System.out.println("高:" + zoom.getHeight());
					switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						lastX = event.getX();
						setCurrentValue(getToureduceCurrentValues()
								- (event.getX() - lastX));
						break;
					}
					return true;
				}
			});

			zoomIn = new ImageView(this);
			zoomIn.setImageResource(R.drawable.gallery_zoom_in_touch);
			zoomIn.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						lastX = event.getX();
						setCurrentValue(getToumagnifyCurrentValues()
								- (event.getX() - lastX));
						break;
					}
					return true;
				}
			});

			float[] screen2 = getScreen2();
			float screenWidth = screen2[0];
			float screenHeight = screen2[1];

			/*
			 * 縮小按鈕圖片進行縮小
			 */
			zoomClose = new ImageView(this);
			zoomClose.setImageResource(R.drawable.close);
			LinearLayout.LayoutParams p = new LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			p.width = (int) (screenWidth * (20 / 256f));
			p.height = (int) (screenHeight * (20 / 436f));
			p.topMargin = (int) (screenHeight * (3 / 436f));
			p.leftMargin = (int) (screenWidth * 0.5);
			zoomClose.setLayoutParams(p);
			zoomClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					myHandler.sendEmptyMessage(1);
				}

			});
			zoom.addView(zoomOut); // 将两个按钮添加到一个小布局中
			zoom.addView(zoomIn);
			zoom.addView(zoomClose);

			final FrameLayout frameLayout = createMainContainer();
			frameLayout.addView(documentView);
			frameLayout.addView(zoom); // 將两个按钮添加到总布局中去
			frameLayout.addView(createZoomControls(zoomModel));
			setFullScreen();
			setContentView(frameLayout);

			final SharedPreferences sharedPreferences = getSharedPreferences(
					DOCUMENT_VIEW_STATE_PREFERENCES, 0);
			documentView.goToPage(sharedPreferences.getInt(getIntent()
					.getData().toString(), 0));
			documentView.showDocument();

			documentView.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					zoom.setVisibility(View.VISIBLE);
				}
			});

			viewerPreferences.addRecent(getIntent().getData());
		} catch (Exception e) {
			alert("打开文档出错，请重试或者联系管理员"); 
		}

	}

	// 放大
	public float getToumagnifyCurrentValues() {
		float mv = (zoomModel.getZoom() - 0.5f) * 400;
		return mv;
	}

	// 缩小
	public float getToureduceCurrentValues() {
		float mv = (zoomModel.getZoom() - 1.0f) * 200;
		return mv;
	}

	public void setCurrentValue(float currentValue) {
		if (currentValue < 0.0)
			currentValue = 0.0f;
		if (currentValue > MAX_VALUE)
			currentValue = MAX_VALUE;
		final float zoom = 1.0f + currentValue / MULTIPLIER;
		zoomModel.setZoom(zoom);
	}

	public void decodingProgressChanged(final int currentlyDecoding) {
		runOnUiThread(new Runnable() {
			public void run() {
				getWindow().setFeatureInt(
						Window.FEATURE_INDETERMINATE_PROGRESS,
						currentlyDecoding == 0 ? 10000 : currentlyDecoding);
			}
		});
	}

	public void currentPageChanged(int pageIndex) {
		final String pageText = (pageIndex + 1) + "/"
				+ decodeService.getPageCount();
		if (pageNumberToast != null) {
			pageNumberToast.setText(pageText);
		} else {
			pageNumberToast = Toast.makeText(this, pageText, 300);
		}
		pageNumberToast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
		pageNumberToast.show();
		saveCurrentPage();
	}

	private void setWindowTitle() {
		final String name = getIntent().getData().getLastPathSegment();
		getWindow().setTitle(name);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setWindowTitle();
	}

	/*
	 * 设置全屏
	 */
	private void setFullScreen() {
		if (viewerPreferences.isFullScreen()) {
			// getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // 在标题栏上加入更新进度条
		}
	}

	/*
	 * 设置缩放栏的位置
	 */
	private PageViewZoomControls createZoomControls(ZoomModel zoomModel) {
		final PageViewZoomControls controls = new PageViewZoomControls(this,
				zoomModel);
		controls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
		zoomModel.addEventListener(controls);
		return controls;
	}

	private FrameLayout createMainContainer() {
		return new FrameLayout(this);
	}

	private void initDecodeService() {
		if (decodeService == null) {
			decodeService = createDecodeService();
		}
	}

	protected abstract DecodeService createDecodeService();

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		decodeService.recycle();
		decodeService = null;
		super.onDestroy();
	}

	private void saveCurrentPage() {
		final SharedPreferences sharedPreferences = getSharedPreferences(
				DOCUMENT_VIEW_STATE_PREFERENCES, 0);
		final SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(getIntent().getData().toString(),
				documentView.getCurrentPage());
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_EXIT, 0, "退出");
		menu.add(0, MENU_GOTO, 0, "跳转");
		// final MenuItem menuItem = menu.add(0, MENU_FULL_SCREEN, 0,
		// "Full screen").setCheckable(true).setChecked(viewerPreferences.isFullScreen());
		// setFullScreenMenuItemText(menuItem);
		return true;
	}

	private void setFullScreenMenuItemText(MenuItem menuItem) {
		menuItem.setTitle("Full screen "
				+ (menuItem.isChecked() ? "on" : "off"));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_EXIT:
			System.exit(0);
			return true;
		case MENU_GOTO:
			showDialog(DIALOG_GOTO);
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_GOTO:
			return new GoToPageDialog(this, documentView, decodeService);
		}
		return null;
	}
}
