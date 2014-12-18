package com.fenchtose.pixcart;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class PixActivity extends Activity {
	
	private CameraPreview cameraPreview;
	private PreviewSurfaceView camView;
//	private DrawingView drawingView;
	private Bitmap previewBitmap;
	private String filename;
	
	private int previewWidth = 640;
	private int previewHeight = 480;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ActionBar actionBar = getActionBar();
		actionBar.hide();
//		actionBar.setDisplayShowTitleEnabled(false);
//		actionBar.setDisplayShowHomeEnabled(false);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_pix);
		
		camView = (PreviewSurfaceView) findViewById(R.id.preview_surface);
		SurfaceHolder camHolder = camView.getHolder();

		cameraPreview = new CameraPreview(this, previewWidth, previewHeight, 1);
		camHolder.addCallback(cameraPreview);
		camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		camView.setListener(cameraPreview);
		
//		drawingView = (DrawingView) findViewById(R.id.drawing_view);
//		camView.setDrawingView(drawingView);

	}
	
	@Override
	public void onPause() {
		if (cameraPreview != null) {
			cameraPreview.releaseCamera();
		}
		
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (cameraPreview != null) {
			cameraPreview.resumePreview();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (cameraPreview != null) {
			cameraPreview.releaseCamera();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pix, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
