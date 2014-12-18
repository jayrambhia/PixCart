package com.fenchtose.pixcart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.AsyncTask;
import android.os.Environment;

import android.view.SurfaceHolder;
import android.widget.Toast;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {
	
	private static final int STATE_OFF = 0;
	private static final int STATE_NO_CALLBACKS = 1;
	private static final int STATE_PREVIEW = 2;
    private static final int STATE_PROCESS = 3;
    private static final int STATE_PROCESS_IN_PROGRESS = 4; 
    private static final int STATE_PREVIEW_PICTURE = 5;
    private static final int STATE_PREVIEW_PICTURE_IN_PROGRESS = 6;
    
    private int mState = STATE_PREVIEW;
	
    private Camera mCamera = null;
	public Camera.Parameters params;
	private SurfaceHolder sHolder;
	private GetPictureTask getPictureTask;

	private Bitmap bitmap = null;
	
	private String previewBitmapFileName; // preview bitmap to be saved
	
	private int camId = 1;
	public List<Camera.Size> supportedSizes;
	public int isCamOpen = 0;
	public boolean isSizeSupported = false;

	private int previewWidth, previewHeight;
	private Context context;
	
	public CameraPreview(Context con, int width, int height, int camera_pref) {
		context = con;
		previewWidth = width;
		previewHeight = height;
		camId = camera_pref;
	}
	
	public CameraPreview(Context con, int width, int height, int camera_pref, String fname) {
		context = con;
		previewWidth = width;
		previewHeight = height;
		camId = camera_pref;
		previewBitmapFileName = fname;
	}
	
	public void resumePreview() {
		if (isCamOpen != 1) {
			openCamera(camId);
		}
	}
	
	private int openCamera(int cameraId) {
		if (isCamOpen == 1) {
			releaseCamera();
		}

		// this.currentZoomLevel = 1;

		if (cameraId == 0) {
			int fflag = 0;
			try {
				mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
				fflag = 1;
			} catch (Exception e) {
				fflag = 0;
			}

			if (fflag == 0) {
				try {
					mCamera = Camera.open(1);
					fflag = 1;
				} catch (Exception e) {
					fflag = 0;
				}
			}

			if (fflag == 0) {
				camId = 1;
				this.changeCam(1);
				Toast.makeText(context, "Unable to initialize front camera",
						Toast.LENGTH_SHORT).show();
				return camId;
			}

		} else {
			int bflag = 0;
			try {
				mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
				bflag = 1;
			} catch (Exception e) {
				bflag = 0;
			}
			if (bflag == 0) {
				try {
					mCamera = Camera.open(0); // for htc evo on which I tested it
					bflag = 1;
				} catch (Exception e) {
					bflag = 0;
				}
			}

			if (bflag == 0) {
				return -1;
			}
		}

		if (mCamera == null) {
			return -1;
		}
		
		mCamera.setDisplayOrientation(90);
		
		
		params = mCamera.getParameters();
		
		supportedSizes = params.getSupportedPreviewSizes();
		Camera.Size sz = supportedSizes.get(0);
		
		for (int i = 0; i < supportedSizes.size(); i++) {
			sz = supportedSizes.get(i);
			if (sz.width == previewWidth && sz.height == previewHeight) {
				isSizeSupported = true;
				break;
			}
		}

		if (isSizeSupported) {
			params.setPreviewSize(previewWidth, previewHeight);
		} else {
			previewWidth = sz.width;
			previewHeight = sz.height;
			params.setPreviewSize(sz.width, sz.height);
		}

		try {
			mCamera.setParameters(params);
		} catch (RuntimeException e) {
			camId = 1;
			if (cameraId == 0) {
				changeCam(camId);
				Toast.makeText(context, "Unable to initialize front camera",
						Toast.LENGTH_SHORT).show();
				return camId;
			}

		}
		mCamera.startPreview();

		try {
			mCamera.setPreviewDisplay(sHolder);

			// Had to move it here. Otherwise it stopped focusing when capture button
			// was pressed on Nexus 7. Weird issue. I thought this might happen with 
			// other devices too
			mCamera.setPreviewCallbackWithBuffer(this);
	        int expectedBytes = previewWidth * previewHeight *
	                ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
	        for (int i=0; i < 4; i++) {
	            mCamera.addCallbackBuffer(new byte[expectedBytes]);
	        }
	        mState = STATE_PREVIEW;
			//mCamera.setPreviewCallback(this);
		} catch (IOException e) {
			mCamera.release();
			mCamera = null;
			return -1;
		}
		
		isCamOpen = 1;
		return isCamOpen;
	}
	public int isCamOpen() {
		return isCamOpen;
	}

	public void resetSize(int[] size) {
		previewWidth = size[0];
		previewHeight = size[1];
		this.openCamera(camId);
	}

	public void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
			mState = STATE_OFF;
		}
		isCamOpen = 0;
	}
	
	public int changeCam(int camId) {
		int success;
		success = openCamera(camId);
		this.camId = camId;
		return success;
	}
	
	public void switchCam() {
		if (camId == 1) {
			camId = 0;
		} else {
			camId = 1;
		}
		openCamera(camId);
	}

	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (data == null) {
			return;
		}
		
		int expectedBytes = previewWidth * previewHeight *
                ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;

        if (expectedBytes != data.length) {

            mState = STATE_NO_CALLBACKS;
            mCamera.setPreviewCallbackWithBuffer(null);
            return;
        }
        
        if (mState == STATE_PREVIEW_PICTURE_IN_PROGRESS) {
        	mCamera.addCallbackBuffer(data);
			return;
        }
		
			
		if (mState == STATE_PREVIEW_PICTURE) {
			mState = STATE_PREVIEW_PICTURE_IN_PROGRESS;
			getPictureTask = new GetPictureTask();
			getPictureTask.execute(data);
		} else {
			mCamera.addCallbackBuffer(data);
			return;
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		sHolder = holder;
		isCamOpen = openCamera(camId);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseCamera();
	}
	
	public void takePicture() {
		mState = STATE_PREVIEW_PICTURE;
	}
	
	/**
	 * Called from PreviewSurfaceView to set touch focus.
	 * 
	 * @param - Rect - new area for auto focus
	 */
	public void doTouchFocus(final Rect tfocusRect) {
		try {
			final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
			Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
			focusList.add(focusArea);
		  
			Camera.Parameters para = mCamera.getParameters();
			para.setFocusAreas(focusList);
			para.setMeteringAreas(focusList);
			mCamera.setParameters(para);
		  
			mCamera.autoFocus(myAutoFocusCallback);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * AutoFocus callback
	 */
	AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

		  @Override
		  public void onAutoFocus(boolean arg0, Camera arg1) {
		   // TODO Auto-generated method stub
		   if (arg0){
		    mCamera.cancelAutoFocus();      
		   }
	    }
	};
	
	/**
	 * This class extends AsyncTask. Creates a thread in background to 
	 * save current scene as .jpg.
	 * 
	 * @params - byte[] - data from previewCallback
	 * 
	 * Once the task is finished, it calls appropriate method of LenxActivity
	 * based on 'getPictureTaskFor' String. 
	 * Currently, it just calls startExposurePreviewActivity()
	 */
	
	private class GetPictureTask extends AsyncTask<byte[], Void, Boolean> {

		@Override
		protected Boolean doInBackground(byte[]... params) {
			mState = STATE_PREVIEW_PICTURE_IN_PROGRESS;
			byte[] nvFrameData = params[0];
			YuvImage img = new YuvImage(nvFrameData, ImageFormat.NV21, previewWidth, previewHeight, null);
			File file;
			if (previewBitmapFileName == null) {
				file = getDateFile();
			} else {
				file = new File(previewBitmapFileName);
			}
			
			FileOutputStream filecon;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				filecon = new FileOutputStream(file);
				img.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 90, baos);
				
				byte[] rawImage = baos.toByteArray();
				Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);
				
				// Rotate the Bitmap
			    Matrix matrix = new Matrix();
			    matrix.postRotate(90);

			    // We rotate the same Bitmap
			    bitmap = Bitmap.createBitmap(bitmap, 0, 0, previewWidth, previewHeight, matrix, false);

			    // We dump the rotated Bitmap to the stream 
			    bitmap.compress(CompressFormat.JPEG, 90, filecon);
				
				previewBitmapFileName = file.toString();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			mState = STATE_PREVIEW;
//			CameraPreviewActivity cpa = (CameraPreviewActivity) context;
//			cpa.showImageToUser();
		}

	}
	
	public String getPreviewBitmapFilename() {
		return previewBitmapFileName;
	}
	
	/**
	 * get a file with path as current time.
	 * @return File
	 */
	public File getDateFile() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Date now = new Date();
		String _path = Environment.getExternalStorageDirectory()
				+ File.separator + "Lenx" + File.separator
				+ formatter.format(now) + ".jpg";
		File file = new File(_path);
		return file;
	}
	
	public int getPreviewWidth() {
		return previewWidth;
	}
	
	public int getPreviewHeight() {
		return previewHeight;
	}
}