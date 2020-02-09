package com.lorenzosani.eeg_app;


import com.neurosky.connection.TgStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

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
		// (1) Example of redirectConsoleLogToDocumentFolder()
		// Call redirectConsoleLogToDocumentFolder at the beginning of the app, it will record all the log.
		// Call redirectConsoleLogToDocumentFolder at the beginning of the app, it will record all the log.
		// Don't forget to call stopConsoleLog() in onDestroy() if it is the end point of this app.
		// If you can't find the end point of the app , you don't have to call stopConsoleLog()
		TgStreamReader.redirectConsoleLogToDocumentFolder();
		// (3) demo of getVersion
		Log.d(TAG,"lib version: " + TgStreamReader.getVersion());
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
