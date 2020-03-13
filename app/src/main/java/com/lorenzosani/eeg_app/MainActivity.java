package com.lorenzosani.eeg_app;

import com.neurosky.connection.TgStreamReader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private BluetoothAdapter mBluetoothAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main_view);

		initView();
		TgStreamReader.redirectConsoleLogToDocumentFolder();

		// Ask permission to read storage to get available songs
		if (ContextCompat.checkSelfPermission(MainActivity.this,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(MainActivity.this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
			} else {
				ActivityCompat.requestPermissions(MainActivity.this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
			}
		}
	}

	private void initView() {
		Button btn_device = (Button) findViewById(R.id.btn_device);
		btn_device.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
			try {
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
					Toast.makeText(getApplicationContext(), "Please enable your Bluetooth!", Toast.LENGTH_LONG).show();
				} else{
					Intent intent = new Intent(MainActivity.this, MusicControlActivity.class);
					Log.d(TAG,"Start the BluetoothDeviceDemoActivity");
					startActivity(intent);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "error:" + e.getMessage());
			}
			}
		});
	}

	@Override
	protected void onDestroy() {
		TgStreamReader.stopConsoleLog();
		super.onDestroy();
	}

	@Override
	protected void onStart() { super.onStart();	}

	@Override
	protected void onStop() { super.onStop(); }

}
