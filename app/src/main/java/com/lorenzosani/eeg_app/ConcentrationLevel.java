package com.lorenzosani.eeg_app;

import android.util.Log;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class ConcentrationLevel{
    private static final String TAG = MusicControlActivity.class.getSimpleName();

    private ArrayList<Integer> attentionData = new ArrayList<>();
    private ArrayList<Integer> meditationData = new ArrayList<>();
    private ArrayList<Integer> recentBetaWaves = new ArrayList<>();
    private ArrayList<Integer> recentAlphaWaves = new ArrayList<>();
    private int averageAttention;
    private int averageMeditation;
    private int averageBeta;
    private int averageAlpha;
    private int lastAttention;
    private int lastMeditation;
    private int lastBeta;
    private int lastAlpha;

    boolean mindControlEnabled;
    boolean isPoorQuality;
    boolean isTrigger;
    int secondsToAverage;

    ConcentrationLevel() {
        mindControlEnabled = false;
        isPoorQuality = true;
        isTrigger = false;
        secondsToAverage = 6;
    }

    void newAttention(int data) {
        if (isPoorQuality(data)){ return; }
        if (mindControlEnabled) {
            lastAttention = data;
            attentionData.add(data);
            if (attentionData.size() > secondsToAverage) {
                attentionData.remove(0);
                triggerMusic();
            }
            averageAttention = getAverage(attentionData);
        }
    }

    void newMeditation(int data) {
        if (isPoorQuality(data)){ return; }
        if (mindControlEnabled) {
            lastMeditation = data;
            meditationData.add(data);
            if (meditationData.size() > secondsToAverage) {
                meditationData.remove(0);
                triggerMusic();
            }
            averageMeditation = getAverage(meditationData);
        }
    }

    void newBeta(int data) {

    }

    void newAlpha(int data) {

    }

    private boolean isPoorQuality(int data) {
        if (data == 0) {
            isPoorQuality = true;
            return true;
        }
        isPoorQuality = false;
        return false;
    }

    private void triggerMusic() {
        int length = attentionData.size();
        if (length >= secondsToAverage) {
            if (isTrigger()) {
                isTrigger = true;
                attentionData = new ArrayList<>(0);
                meditationData = new ArrayList<>(0);
            }
        }
    }

    private boolean isTrigger() {
        return lastAttention > averageAttention && lastMeditation < averageMeditation;
    }

    private int getAverage(ArrayList<Integer> list) {
        int sum = 0;
        if (list.size() == 0) {
            return 0;
        }
        for (int i : list) {
            sum += i;
        }
        return sum/list.size();
    }
}
