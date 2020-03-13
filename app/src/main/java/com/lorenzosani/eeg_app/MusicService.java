package com.lorenzosani.eeg_app;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final IBinder musicBind = new MusicBinder();

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private boolean paused;
    private int currentSong;

    public void onCreate(){
        super.onCreate();
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void playSong(int i){
        if (paused && currentSong == i) {
            player.start();
            paused = false;
            return;
        }
        player.reset();
        Song playSong = songs.get(i);
        currentSong = i;
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public boolean isPng(){
        if (player != null) {
            return player.isPlaying();
        }
        initMusicPlayer();
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
        paused = true;
    }

    public void setList(ArrayList<Song> s){
        songs=s;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        player = null;
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
