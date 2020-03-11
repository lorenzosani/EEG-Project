package com.lorenzosani.eeg_app;

import java.util.ArrayList;

import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.DataType.MindDataType.FilterType;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BluetoothDeviceDemoActivity extends AppCompatActivity {

	private static final String TAG = BluetoothDeviceDemoActivity.class.getSimpleName();
	private TgStreamReader tgStreamReader;
	private BluetoothAdapter mBluetoothAdapter;

	private ArrayList<Song> songList;
	private int currentSong = 0;
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound=false;

	private ArrayList<Integer> recentAttentionLvl = new ArrayList<>();
	private int averageAttention;

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
				start();
			}

		});

		// Set the first song to be displayed in the player
		Song firstSong = songList.get(currentSong);
		track_title.setText(firstSong.getTitle() + " by " + firstSong.getArtist());
	}
	
	private void start(){
			createStreamReader(mBluetoothAdapter);
            tgStreamReader.setGetDataTimeOutTime(10);
			tgStreamReader.connectAndStart();
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
		super.onStop();
		stop();
	}
	DrawWaveView waveView = null;
	public void setUpDrawWaveView() {
		
		waveView = new DrawWaveView(getApplicationContext());
		wave_layout.addView(waveView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		waveView.setValue(2048, 2048, -2048);
	}
	public void updateWaveView(int data) {
		if (waveView != null) {
			waveView.updateData(data);
		}
	}
	private int currentState = 0;
	private TgStreamHandler callback = new TgStreamHandler() {

		@Override
		public void onStatesChanged(int connectionStates) {
			Log.d(TAG, "connectionStates change to: " + connectionStates);
			currentState  = connectionStates;
			switch (connectionStates) {
			case ConnectionStates.STATE_CONNECTED:
                tgStreamReader.startRecordRawData();
				showToast("Connected", Toast.LENGTH_SHORT);
				break;
			case ConnectionStates.STATE_WORKING:
				LinkDetectedHandler.sendEmptyMessageDelayed(1234, 5000);
				break;
			case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                tgStreamReader.stopRecordRawData();
				showToast("No data received. Retrying...", Toast.LENGTH_SHORT);
				stop();
				start();
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
			Log.e(TAG,"onRecordFail: " +a);

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

	private static final int MSG_UPDATE_BAD_PACKET = 1001;
	private static final int MSG_UPDATE_STATE = 1002;
	private static final int MSG_CONNECT = 1003;
	private boolean isReadFilter = false;

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
				if (msg.arg1 > 0){ updateAverageAttention(msg.arg1); }
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

	private void updateAverageAttention(int attention) {
		recentAttentionLvl.add(attention);
		int length = recentAttentionLvl.size();
		int sum = 0;

		if (length >= 6 && attention >= averageAttention+20) {
			triggerMusic();
			recentAttentionLvl = new ArrayList<>();
		} else if (length >= 6) {
			recentAttentionLvl.remove(0);
		}
		for (int i : recentAttentionLvl) { sum += i; }
		averageAttention = sum/length;
	}

	private void triggerMusic() {
		if (musicSrv.isPng()){
			musicSrv.pausePlayer();
			track_play_pause.setImageResource(R.drawable.ic_play);
		} else {
			musicSrv.playSong(currentSong);
			track_play_pause.setImageResource(R.drawable.ic_pause);
		}
	}


	public void showToast(final String msg,final int timeStyle){
		this.runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText(getApplicationContext(), msg, timeStyle).show();
			}

		});
	}

	public TgStreamReader createStreamReader(BluetoothAdapter bd){

		if(tgStreamReader == null){
			tgStreamReader = new TgStreamReader(bd,callback);
			tgStreamReader.startLog();
		}else{
			tgStreamReader.setTgStreamHandler(callback);
		}
		return tgStreamReader;
	}

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