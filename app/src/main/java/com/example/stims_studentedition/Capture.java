package com.example.stims_studentedition;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.journeyapps.barcodescanner.CaptureActivity;

public class Capture extends CaptureActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}