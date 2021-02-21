package com.example.superior_piano;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class Bruh extends JavaCameraView {
    public Bruh(Context context, int cameraId) {
        super(context, cameraId);
    }

    public Bruh(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEffect(String effect) {
        if (mCamera == null) return;
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(effect);
        mCamera.setParameters(params);
    }
}
