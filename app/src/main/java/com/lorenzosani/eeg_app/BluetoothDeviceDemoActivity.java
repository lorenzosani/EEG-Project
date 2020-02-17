package com.lorenzosani.eeg_app;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.DataType.MindDataType.FilterType;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

public class BluetoothDeviceDemoActivity extends AppCompatActivity {

	private static final String TAG = BluetoothDeviceDemoActivity.class.getSimpleName();
	private TgStreamReader tgStreamReader;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private String address = null;

	private ArrayList<Song> songList;
	private int currentSong = 0;
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.bluetoothdevice_view);

		getSongList();
		initView();
		setUpDrawWaveView();

		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
				Toast.makeText(
						this,
						"Please enable your Bluetooth and re-run this program !",
						Toast.LENGTH_LONG).show();
				finish();
			}  
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "error:" + e.getMessage());
			return;
		}
	}

	private ServiceConnection musicConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
			//get service
			musicSrv = binder.getService();
			//pass list
			musicSrv.setList(songList);
			musicBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicBound = false;
		}
	};

	private TextView track_title;
	private ImageView track_previous;
	private ImageView track_play_pause;
	private ImageView track_next;

	private TextView tv_attention = null;
	private TextView tv_meditation = null;
	private TextView tv_lowalpha = null;
	
	private TextView  tv_highalpha = null;
	private TextView  tv_lowbeta = null;
	private TextView  tv_highbeta = null;
	
	private Button btn_start = null;
	private Button btn_stop = null;
	private Button btn_selectdevice = null;
	private LinearLayout wave_layout;
	
	private int badPacketCount = 0;

	private void initView() {
		track_title = (TextView) findViewById(R.id.track_title);
		track_previous = (ImageView) findViewById(R.id.track_previous);
		track_play_pause = (ImageView) findViewById(R.id.track_play_pause);
		track_next = (ImageView) findViewById(R.id.track_next);

		tv_attention = (TextView) findViewById(R.id.tv_attention);
		tv_meditation = (TextView) findViewById(R.id.tv_meditation);
		tv_lowalpha = (TextView) findViewById(R.id.tv_lowalpha);
		
		tv_highalpha = (TextView) findViewById(R.id.tv_highalpha);
		tv_lowbeta= (TextView) findViewById(R.id.tv_lowbeta);
		tv_highbeta= (TextView) findViewById(R.id.tv_highbeta);
		
		btn_start = (Button) findViewById(R.id.btn_start);
		btn_stop = (Button) findViewById(R.id.btn_stop);
		btn_selectdevice =  (Button) findViewById(R.id.btn_selectdevice);
		wave_layout = (LinearLayout) findViewById(R.id.wave_layout);
		
		btn_start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				badPacketCount = 0;
				showToast("connecting ...",Toast.LENGTH_SHORT);
				start();
			}
		});

		btn_stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(tgStreamReader != null){
					tgStreamReader.stop();
				}
			}

		});

		track_play_pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (musicSrv.isPng()){
					musicSrv.pausePlayer();
					track_play_pause.setImageResource(R.drawable.ic_play);
				} else {
					musicSrv.playSong(currentSong);
					track_play_pause.setImageResource(R.drawable.ic_pause);
				}
			}

		});

		track_previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				goToSong(-1);
			}

		});

		track_next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				goToSong(1);
			}

		});

		btn_selectdevice.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				scanDevice();
			}

		});

		// Set the first song to be displayed in the player
		Song firstSong = songList.get(currentSong);
		track_title.setText(firstSong.getTitle() + " by " + firstSong.getArtist());
	}
	
	private void start(){
		if(address != null){
			BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
			createStreamReader(bd);

			tgStreamReader.connectAndStart();
		}else{
			showToast("Please select device first!", Toast.LENGTH_SHORT);
		}
	}

	public void stop() {
		if(tgStreamReader != null){
			tgStreamReader.stop();
			tgStreamReader.close();//if there is not stop cmd, please call close() or the data will accumulate 
			tgStreamReader = null;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(tgStreamReader != null){
			tgStreamReader.close();
			tgStreamReader = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(playIntent==null){
			playIntent = new Intent(this, MusicService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		stop();
	}

	// TODO view
	DrawWaveView waveView = null;
	// (2) demo of drawing ECG, set up of view
	public void setUpDrawWaveView() {
		
		waveView = new DrawWaveView(getApplicationContext());
		wave_layout.addView(waveView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		waveView.setValue(2048, 2048, -2048);
	}
	// (2) demo of drawing ECG, update view
	public void updateWaveView(int data) {
		if (waveView != null) {
			waveView.updateData(data);
		}
	}
	private int currentState = 0;
	private TgStreamHandler callback = new TgStreamHandler() {

		@Override
		public void onStatesChanged(int connectionStates) {
			// TODO Auto-generated method stub
			Log.d(TAG, "connectionStates change to: " + connectionStates);
			currentState  = connectionStates;
			switch (connectionStates) {
			case ConnectionStates.STATE_CONNECTED:
				//sensor.start();
				showToast("Connected", Toast.LENGTH_SHORT);
				break;
			case ConnectionStates.STATE_WORKING:
				//byte[] cmd = new byte[1];
				//cmd[0] = 's';
				//tgStreamReader.sendCommandtoDevice(cmd);
				LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
				break;
			case ConnectionStates.STATE_GET_DATA_TIME_OUT:
				//get data time out
				break;
			case ConnectionStates.STATE_COMPLETE:
				//read file complete
				break;
			case ConnectionStates.STATE_STOPPED:
				break;
			case ConnectionStates.STATE_DISCONNECTED:
				break;
			case ConnectionStates.STATE_ERROR:
				Log.d(TAG,"Connect error, Please try again!");
				break;
			case ConnectionStates.STATE_FAILED:
				Log.d(TAG,"Connect failed, Please try again!");
				break;
			}
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = MSG_UPDATE_STATE;
			msg.arg1 = connectionStates;
			LinkDetectedHandler.sendMessage(msg);
			

		}

		@Override
		public void onRecordFail(int a) {
			// TODO Auto-generated method stub
			Log.e(TAG,"onRecordFail: " +a);

		}

		@Override
		public void onChecksumFail(byte[] payload, int length, int checksum) {
			// TODO Auto-generated method stub
			
			badPacketCount ++;
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = MSG_UPDATE_BAD_PACKET;
			msg.arg1 = badPacketCount;
			LinkDetectedHandler.sendMessage(msg);

		}

		@Override
		public void onDataReceived(int datatype, int data, Object obj) {
			// TODO Auto-generated method stub
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = datatype;
			msg.arg1 = data;
			msg.obj = obj;
			LinkDetectedHandler.sendMessage(msg);
			//Log.i(TAG,"onDataReceived");
		}

	};

	private boolean isPressing = false;
	private static final int MSG_UPDATE_BAD_PACKET = 1001;
	private static final int MSG_UPDATE_STATE = 1002;
	private static final int MSG_CONNECT = 1003;
	private boolean isReadFilter = false;

	int raw;
	private Handler LinkDetectedHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case 1234:
        		tgStreamReader.MWM15_getFilterType();
        		isReadFilter = true;
        		Log.d(TAG,"MWM15_getFilterType ");
        		
        		break;
        	case 1235:
        		tgStreamReader.MWM15_setFilterType(FilterType.FILTER_60HZ);
        		Log.d(TAG,"MWM15_setFilter  60HZ");
        		LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
        		break;
        	case 1236:
        		tgStreamReader.MWM15_setFilterType(FilterType.FILTER_50HZ);
        		Log.d(TAG,"MWM15_SetFilter 50HZ ");
        		LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
        		break;
        		
			case 1237:
        		tgStreamReader.MWM15_getFilterType();
        		Log.d(TAG,"MWM15_getFilterType ");
        		
        		break;
        		
        	case MindDataType.CODE_FILTER_TYPE:
        		Log.d(TAG,"CODE_FILTER_TYPE: " + msg.arg1 + "  isReadFilter: " + isReadFilter);
        		if(isReadFilter){
        			isReadFilter = false;
        			if(msg.arg1 == FilterType.FILTER_50HZ.getValue()){
        				LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
        			}else if(msg.arg1 == FilterType.FILTER_60HZ.getValue()){
        				LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
        			}else{
        				Log.e(TAG,"Error filter type");
        			}
        		}
        		
        		break;
        		
        		
        		
			case MindDataType.CODE_RAW:
					updateWaveView(msg.arg1);
				break;
			case MindDataType.CODE_MEDITATION:
				Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
				tv_meditation.setText("" +msg.arg1 );
				break;
			case MindDataType.CODE_ATTENTION:
				Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
				tv_attention.setText("" +msg.arg1 );
				break;
			case MindDataType.CODE_EEGPOWER:
				EEGPower power = (EEGPower)msg.obj;
				if(power.isValidate()){
					tv_lowalpha.setText("" +power.lowAlpha);
					tv_highalpha.setText("" +power.highAlpha);
					tv_lowbeta.setText("" +power.lowBeta);
					tv_highbeta.setText("" +power.highBeta);
				}
				break;
			case MindDataType.CODE_POOR_SIGNAL://
				int poorSignal = msg.arg1;
				Log.d(TAG, "poorSignal:" + poorSignal);

				break;
			case MSG_UPDATE_BAD_PACKET:
				Log.d(TAG, "badPacket:" + msg.arg1);
				
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};


	public void showToast(final String msg,final int timeStyle){
		this.runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText(getApplicationContext(), msg, timeStyle).show();
			}

		});
	}
	
	//show device list while scanning
	private ListView list_select;
	private BTDeviceListAdapter deviceListApapter = null;
	private Dialog selectDialog;
	
	// (3) Demo of getting Bluetooth device dynamically
    public void scanDevice(){

    	if(mBluetoothAdapter.isDiscovering()){
    		mBluetoothAdapter.cancelDiscovery();
    	}
    	
    	setUpDeviceListView();
    	//register the receiver for scanning
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
    	
    	mBluetoothAdapter.startDiscovery();
    }
    
 private void setUpDeviceListView(){
    	
    	LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_select_device, null);
		list_select = (ListView) view.findViewById(R.id.list_select);
		selectDialog = new Dialog(this);
		selectDialog.setContentView(view);
    	//List device dialog

    	deviceListApapter = new BTDeviceListAdapter(this);
    	list_select.setAdapter(deviceListApapter);
    	list_select.setOnItemClickListener(selectDeviceItemClickListener);
    	
    	selectDialog.setOnCancelListener(new OnCancelListener(){

			@Override
			public void onCancel(DialogInterface arg0) {
				// TODO Auto-generated method stub
				Log.e(TAG,"onCancel called!");
				BluetoothDeviceDemoActivity.this.unregisterReceiver(mReceiver);
			}
    		
    	});
    	
    	selectDialog.show();
    	
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	for(BluetoothDevice device: pairedDevices){
    		deviceListApapter.addDevice(device);
    	}
		deviceListApapter.notifyDataSetChanged();
    }
 
 //Select device operation
 private OnItemClickListener selectDeviceItemClickListener = new OnItemClickListener(){
	 
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3) {
			// TODO Auto-generated method stub
			Log.d(TAG, "Rico ####  list_select onItemClick     ");
	    	if(mBluetoothAdapter.isDiscovering()){
	    		mBluetoothAdapter.cancelDiscovery();
	    	}
	    	//unregister receiver
	    	BluetoothDeviceDemoActivity.this.unregisterReceiver(mReceiver);

	    	mBluetoothDevice =deviceListApapter.getDevice(arg2);
	    	selectDialog.dismiss();
	    	selectDialog = null;
	    	
			Log.d(TAG,"onItemClick name: "+mBluetoothDevice.getName() + " , address: " + mBluetoothDevice.getAddress() );
			address = mBluetoothDevice.getAddress().toString();
			
			//ger remote device
			BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress().toString());
         
			//bind and connect
			//bindToDevice(remoteDevice); // create bond works unstable on Samsung S5
			//showToast("pairing ...",Toast.LENGTH_SHORT);

			tgStreamReader = createStreamReader(remoteDevice); 
			tgStreamReader.connectAndStart();
		
		}
	
 };

	public TgStreamReader createStreamReader(BluetoothDevice bd){

		if(tgStreamReader == null){
			// Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
			tgStreamReader = new TgStreamReader(bd,callback);
			tgStreamReader.startLog();
		}else{
			// (1) Demo of changeBluetoothDevice
			tgStreamReader.changeBluetoothDevice(bd);
			
			// (4) Demo of setTgStreamHandler, you can change the data handler by this function
			tgStreamReader.setTgStreamHandler(callback);
		}
		return tgStreamReader;
	}

 	public void bindToDevice(BluetoothDevice bd){
 	    int ispaired = 0;
		if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
			//ispaired = remoteDevice.createBond();
			try {
				//Set pin
				if(Utils.autoBond(bd.getClass(), bd, "0000")){
					ispaired += 1;
				}
				//bind to device
				if(Utils.createBond(bd.getClass(), bd)){
					ispaired += 2;
				}
				Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TAG,"bool="+bool);
					
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d(TAG, " paire device Exception:    " + e.toString());	
			}
		}
		Log.d(TAG, " ispaired:    " + ispaired);	

 }

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
				Log.d(TAG, "mReceiver()");
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(TAG,"mReceiver found device: " + device.getName());
				
				// update to UI
				deviceListApapter.addDevice(device);
				deviceListApapter.notifyDataSetChanged();
			} 
		}
	};

	public void getSongList() {
		songList = new ArrayList<Song>();
		ContentResolver musicResolver = getContentResolver();
		Cursor musicCursor = musicResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if(musicCursor!=null && musicCursor.moveToFirst()){
			//get columns
			int titleColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.TITLE);
			int idColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media._ID);
			int artistColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.ARTIST);
			//add songs to list
			do {
				long thisId = musicCursor.getLong(idColumn);
				String thisTitle = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				if (!thisTitle.startsWith("Voice")){
					songList.add(new Song(thisId, thisTitle, thisArtist));
				}
			}
			while (musicCursor.moveToNext());
		}
	}

	public void goToSong(int i){
		Song s;
		currentSong+=i;
		try{
			s = songList.get(currentSong);
		}catch(IndexOutOfBoundsException e){
			if(i>0){
				currentSong = 0;
			}else{
				currentSong = songList.size()-1;
			}
			s = songList.get(currentSong);
		}
		track_title.setText(s.getTitle() + " by " + s.getArtist());
		musicSrv.playSong(currentSong);
		track_play_pause.setImageResource(R.drawable.ic_pause);
	}
}