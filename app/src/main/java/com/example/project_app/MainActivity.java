package com.example.project_app;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import java.util.HashMap;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    public boolean isReadFilter = false;
    private int REQUEST_ENABLE_BT = 11;
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private int badPacketCount = 0;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothDeviceAdapter deviceListAdapter;
    private BluetoothDevice headset;
    public String address;

    /***********************************************************************************************************
     *  METHOD CALLED AT LAUNCH
     ***********************************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.first_view);
        deviceListAdapter = new BluetoothDeviceAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1);

        initView();
        setUpDrawWaveView();

        try {
            //Make sure that the device supports Bluetooth and Bluetooth is on
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                //This asks the user to enable Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }else{
                setUpDeviceListView();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
        }
    }

    /***********************************************************************************************************
     *  MAIN DATA HANDLER
     ***********************************************************************************************************/
    @SuppressLint("HandlerLeak")
    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 128:
                    // RAW DATA
                    updateWaveView(msg.arg1);
                    break;
                case 5:
                    // MEDITATION
                    tv_meditation.setText(msg.arg1);
                    break;
                case 4:
                    // ATTENTION
                    tv_attention.setText(msg.arg1);
                    break;
                case MindDataType.CODE_EEGPOWER /*131*/:
                    EEGPower power = (EEGPower) msg.obj;
                    if (power.isValidate()) {
                        tv_highalpha.setText(power.highAlpha);
                        tv_lowbeta.setText(power.lowBeta);
                        tv_highbeta.setText(power.highBeta);
                    }
                    break;
                case 2:
                    // POOR SIGNAL
                    if (msg.arg1==0) {
                        signal.setImageResource(R.drawable.signal_full);
                    } else if (msg.arg1<=30) {
                        signal.setImageResource(R.drawable.signal_medium);
                    } else {
                        signal.setImageResource(R.drawable.signal_low);
                    }

                    break;

                case MindDataType.CODE_FILTER_TYPE:
                    Log.d(MainActivity.TAG, "CODE_FILTER_TYPE: " + msg.arg1 + "  isReadFilter: " + MainActivity.this.isReadFilter);
                    if (MainActivity.this.isReadFilter) {
                        MainActivity.this.isReadFilter = false;
                        if (msg.arg1 != MindDataType.FilterType.FILTER_50HZ.getValue()) {
                            if (msg.arg1 != MindDataType.FilterType.FILTER_60HZ.getValue()) {
                                Log.e(MainActivity.TAG, "Error filter type");
                                break;
                            } else {
                                MainActivity.this.LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                                break;
                            }
                        } else {
                            MainActivity.this.LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                            break;
                        }
                    }
                    break;
                case 1234:
                    MainActivity.this.tgStreamReader.MWM15_getFilterType();
                    MainActivity.this.isReadFilter = true;
                    Log.d(MainActivity.TAG, "MWM15_getFilterType ");
                    break;
                case 1235:
                    MainActivity.this.tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_60HZ);
                    Log.d(MainActivity.TAG, "MWM15_setFilter  60HZ");
                    MainActivity.this.LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    MainActivity.this.tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_50HZ);
                    Log.d(MainActivity.TAG, "MWM15_SetFilter 50HZ ");
                    MainActivity.this.LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1237:
                    MainActivity.this.tgStreamReader.MWM15_getFilterType();
                    Log.d(MainActivity.TAG, "MWM15_getFilterType ");
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /***********************************************************************************************************
     *  SET UI LAYOUT
     ***********************************************************************************************************/
    private ImageView signal = null;
    private TextView status = null;
    private TextView tv_attention = null;
    private TextView tv_meditation = null;
    private TextView tv_highalpha = null;
    private TextView tv_lowbeta = null;
    private TextView tv_highbeta = null;
    private Button btn_start = null;
    private Button btn_stop = null;
    private LinearLayout wave_layout;

    private void initView() {
        status = (TextView) findViewById(R.id.connectionStatus);
        signal = (ImageView) findViewById(R.id.signal);
        tv_attention = (TextView) findViewById(R.id.tv_attention);
        tv_meditation = (TextView) findViewById(R.id.tv_meditation);
        tv_highalpha = (TextView) findViewById(R.id.tv_highalpha);
        tv_lowbeta= (TextView) findViewById(R.id.tv_lowbeta);
        tv_highbeta= (TextView) findViewById(R.id.tv_highbeta);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        wave_layout = (LinearLayout) findViewById(R.id.wave_layout);
        btn_start.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                start();
            }
        });
        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) { stop(); }
        });

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);
    }

    /***********************************************************************************************************
     *  STREAM HANDLER
     ***********************************************************************************************************/
    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int state) {

            HashMap<Integer, String> states = new HashMap<>(12);
            states.put(6, "STATE_COMPLETE");
            states.put(2, "STATE_CONNECTED");
            states.put(1, "STATE_CONNECTING");
            states.put(5, "STATE_DISCONNECTED");
            states.put(101, "STATE_ERROR");
            states.put(100, "STATE_FAILED");
            states.put(9, "STATE_GET_DATA_TIME_OUT");
            states.put(0, "STATE_INIT");
            states.put(8, "STATE_RECORDING_END");
            states.put(7, "STATE_RECORDING_START");
            states.put(4, "STATE_STOPPED");
            states.put(3, "STATE_WORKING");

            Log.d(TAG, "connectionStates change to: " + states.get(state));
            switch (state) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    status.setText("Connecting...");
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    status.setText("Connected!");
                    if (tgStreamReader.isBTConnected()) {
                        tgStreamReader.start();
                    }else{
                        showToast("Not connected to Bluetooth", Toast.LENGTH_SHORT);
                    }
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working
                    status.setText("Working...");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
                    tgStreamReader.setRecordStreamFilePath("/storage/emulated/0/neurosky/records");
                    tgStreamReader.startRecordRawData();
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    showToast("No data received from headset!", Toast.LENGTH_SHORT);
                    tgStreamReader.stopRecordRawData();
                    tgStreamReader.stop();
                    tgStreamReader.close();
                    tgStreamReader = null;
                    start();
                    break;
                case ConnectionStates.STATE_STOPPED:
                    status.setText("Stopped");
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    status.setText("Disconnected");
                    break;
                case ConnectionStates.STATE_ERROR:
                    status.setText("Error");
                    break;
                case ConnectionStates.STATE_FAILED:
                    status.setText("Failed");
                    break;
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = state;
            LinkDetectedHandler.sendMessage(msg);
        }
        @Override
        public void onRecordFail(int flag) {
            Log.e(TAG,"onRecordFail: " +flag);
        }
        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);
        }
        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);
        }
    };

    /***********************************************************************************************************
     *  CREATE DEVICE LIST
     ***********************************************************************************************************/
    public Dialog selectDialog;
    public ListView list_select = null;

    private void setUpDeviceListView() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_device, null);
        this.list_select = (ListView) view.findViewById(R.id.list_select);
        this.selectDialog = new Dialog(this);
        this.selectDialog.setContentView(view);
        this.list_select.setAdapter(deviceListAdapter);
        this.list_select.setOnItemClickListener(this.selectDeviceItemClickListener);
        this.selectDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                Log.e(TAG, "onCancel called!");
            }
        });
        this.selectDialog.show();
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            deviceListAdapter.add(device);
        }
        this.deviceListAdapter.notifyDataSetChanged();
    }

    private AdapterView.OnItemClickListener selectDeviceItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View arg1, int arg2, long arg3) {
            headset = (BluetoothDevice) deviceListAdapter.getItem(arg2);
            address = headset.getAddress();
            selectDialog.dismiss();
            selectDialog = null;
            Log.d(TAG, "onItemClick name: " + headset.getName() + " , address: " + headset.getAddress());
            tgStreamReader = createStreamReader(mBluetoothAdapter.getRemoteDevice(headset.getAddress()));
            tgStreamReader.connect();
        }
    };


    public void start() {
        if (headset == null) {
            showToast("Couldn't connect to the device. Try again.", Toast.LENGTH_SHORT);
            return;
        }
        createStreamReader(mBluetoothAdapter.getRemoteDevice(this.address));
        tgStreamReader.connectAndStart();
    }

    public void stop() {
        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();
            tgStreamReader = null;
        }
    }

    @Override
    protected void onDestroy() {
        if(tgStreamReader != null){
            tgStreamReader.close();
            tgStreamReader = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            if (resultCode == RESULT_OK) {
                setUpDeviceListView();
            }
        }
    }

    DrawWaveView waveView = null;
    public void setUpDrawWaveView() {
        waveView = new DrawWaveView(getApplicationContext());
        wave_layout.addView(waveView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        waveView.setValue(2048, 2048, -2048);
    }

    public void updateWaveView(int data) {
        if (waveView != null) {
            waveView.updateData(data);
        }
    }

    public void showToast(final String msg,final int timeStyle){
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }
        });
    }

    public TgStreamReader createStreamReader(BluetoothDevice bd) {
        if (this.tgStreamReader == null) {
            this.tgStreamReader = new TgStreamReader(bd, this.callback);
            this.tgStreamReader.startLog();
        } else {
            this.tgStreamReader.changeBluetoothDevice(bd);
            this.tgStreamReader.setTgStreamHandler(this.callback);
        }
        return this.tgStreamReader;
    }
}


