package com.lorenzosani.eeg_app;


import com.neurosky.connection.TgStreamReader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * This activity is the man entry of this app. It demonstrates the usage of 
 * (1) TgStreamReader.redirectConsoleLogToDocumentFolder()
 * (2) TgStreamReader.stopConsoleLog()
 * (3) demo of getVersion
 */
public class DemoActivity extends Activity {
	private static final String TAG = DemoActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main_view);

		initView();

		TgStreamReader.redirectConsoleLogToDocumentFolder();

		Log.d(TAG,"lib version: " + TgStreamReader.getVersion());

		// Ask permission to read storage to get available songs
		if (ContextCompat.checkSelfPermission(DemoActivity.this,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(DemoActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(DemoActivity.this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

			} else {
				ActivityCompat.requestPermissions(DemoActivity.this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

			}
		}
	}

	private Button btn_device = null;

	private void initView() {
		btn_device = (Button) findViewById(R.id.btn_device);

		btn_device.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(DemoActivity.this,BluetoothDeviceDemoActivity.class);
				Log.d(TAG,"Start the BluetoothDeviceDemoActivity");
				startActivity(intent);
			}
		});
	}



	@Override
	protected void onDestroy() {
		
		// (2) Example of stopConsoleLog()
		TgStreamReader.stopConsoleLog();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

}
