/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bbbtech.barcodescan;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Debug;

import com.bosphere.filelogger.FL;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
public class BarcodeTracker extends Tracker<Barcode> {
    private BarcodeRecognizer mBarcodeRecognizer;
    private Context mContext;
    private boolean mBeepEnabled;
    private MediaPlayer mediaPlayer;
    private MediaPlayer.OnCompletionListener listener;

    public BarcodeTracker(BarcodeRecognizer barcodeRecognizer) {
        mBarcodeRecognizer = barcodeRecognizer;
    }

    public BarcodeTracker(Context context, BarcodeRecognizer barcodeRecognizer, boolean beepEnabled) {
        mContext = context;
        mBarcodeRecognizer = barcodeRecognizer;
        mBeepEnabled = beepEnabled;
    }

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    @Override
    public void onNewItem(int id, Barcode item) {
        if (mBarcodeRecognizer != null) {
            if (mContext != null && mBeepEnabled) {
                if (null == mediaPlayer) {
                    mediaPlayer = MediaPlayer.create(mContext, R.raw.barcode_beep);
                    mediaPlayer.setOnCompletionListener(getListener());
                    mediaPlayer.start();
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.start();
                }
            }
            mBarcodeRecognizer.onRecognized(item);
        }
    }

    private MediaPlayer.OnCompletionListener getListener() {
        if (null == listener) {
            listener = new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    FL.i("media play onComplete >>");
                    release();
                }
            };
        }

        return listener;
    }

    private void release() {
        mediaPlayer.release();
        mediaPlayer.setOnCompletionListener(null);
        mediaPlayer = null;
    }
}
