package com.bbbtech.barcodescan;

import android.hardware.Camera;

/**
 * Created by levin.yu on 2018. 10. 30..
 */

public interface CameraFrameListener {
    void onFrame(byte[] data, Camera camera);
}
