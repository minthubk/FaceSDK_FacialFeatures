/*
 * FaceSDK Library Sample
 * Copyright (C) 2013 Luxand, Inc. 
 */

package com.example.facialfeatures;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.luxand.FSDK;
import com.luxand.FSDK.*;

public class MainActivity extends Activity {
    private static final String[] PERMISSIONS = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE = 100;
	private static final String TAG = "MainActivity";
	protected HImage oldpicture;
	private static int RESULT_LOAD_IMAGE = 1;
	protected boolean processing;
	ImageView normalImage;
	// Adding button
	Button process;
	private TextView infoText;


	// Subclass for async processing of FaceSDK functions.
	// If long-run task runs in foreground - Android kills the process.
	private class DetectFaceInBackground extends AsyncTask<String, Void, String> {
		protected FSDK_Features features;
		protected TFacePosition faceCoords;
		protected String picturePath;
		protected HImage picture;
		protected int result;
		
		@Override
		protected String doInBackground(String... params) {
			String log = new String();
			picturePath = params[0];
			faceCoords = new TFacePosition();
			faceCoords.w = 0;
			picture = new HImage();
			result = FSDK.LoadImageFromFile(picture, picturePath);
			if (result == FSDK.FSDKE_OK) {
				result = FSDK.DetectFace(picture, faceCoords);
				features = new FSDK_Features();
				if (result == FSDK.FSDKE_OK) {
					//DEBUG
				    //FSDK.SetFaceDetectionThreshold(1);
				    //FSDK.SetFaceDetectionParameters(false, false, 70);
				    //long t0 = System.currentTimeMillis();
				    //for (int i=0; i<10; ++i)
				        //result = FSDK.DetectFacialFeatures(picture, features);
				        result = FSDK.DetectFacialFeaturesInRegion(picture, faceCoords, features);
				    //Log.d("TT", "TIME: " + ((System.currentTimeMillis()-t0)/10.0f));
				}
			}
			processing = false; //long-running code is complete, now user may push the button
			return log;
		}   
		
		@Override
		protected void onPostExecute(String resultstring) {
			TextView tv = (TextView) findViewById(R.id.infoText);
			
			if (result != FSDK.FSDKE_OK)
				return;
			
			FaceImageView imageView = (FaceImageView) findViewById(R.id.faceImageView);
			
			imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
						
		    tv.setText(resultstring);
		    
			imageView.detectedFace = faceCoords;
			
			if (features.features[0] != null) // if detected
				imageView.facial_features = features;
			
			int [] realWidth = new int[1];
			FSDK.GetImageWidth(picture, realWidth);
			imageView.faceImageWidthOrig = realWidth[0];
			imageView.invalidate(); // redraw, marking up faces and features
			
			if (oldpicture != null)
				FSDK.FreeImage(oldpicture);
			oldpicture = picture;
		}
		
		@Override
		protected void onPreExecute() {
		}
		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}
	//end of DetectFaceInBackground class
	
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {

		processing = true; //prevent user from pushing the button while initializing

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); //using res/layout/activity_main.xml


		//Check storage permissions
		if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
		}

		infoText = (TextView) findViewById(R.id.infoText);
		normalImage = findViewById(R.id.normalImage);
		process = (Button) findViewById(R.id.process);

		try {
			int res = FSDK.ActivateLibrary(getString(R.string.fsdk_license));
			FSDK.Initialize();
			FSDK.SetFaceDetectionParameters(false, false, 100);
			FSDK.SetFaceDetectionThreshold(5);

			if (res == FSDK.FSDKE_OK) {
				infoText.setText("FaceSDK activated\n");
			} else {
				infoText.setText("Error activating FaceSDK: " + res + "\n");
			}
		}
    	catch (Exception e) {
			infoText.setText("exception " + e.getMessage());
		}


        processing = false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_load:
				if (!processing) {
					processing = true;
					Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(i, RESULT_LOAD_IMAGE);
				}

				break;
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Log.d(TAG, "onActivityResult: Image uri: " + selectedImage.toString());

			Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);

			Log.d(TAG, "onActivityResult: picturePath: " + picturePath);
			cursor.close();

			Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
			normalImage.setImageBitmap(bitmap);
		
			TextView tv = (TextView) findViewById(R.id.infoText);
	        tv.setText("processing...");
			new DetectFaceInBackground().execute(picturePath);
		} else {
			processing = false;
		}
    }
}
