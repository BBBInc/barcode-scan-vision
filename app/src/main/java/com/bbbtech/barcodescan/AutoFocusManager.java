/*
 * Copyright (C) 2012 ZXing authors
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

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bosphere.filelogger.FL;

/**
 * This should be created and used from the camera thread only. The thread message queue is used
 * to run all operations on the same thread.
 */
final class AutoFocusManager {

    private static final String TAG = AutoFocusManager.class.getSimpleName();

    private final long AUTO_FOCUS_INTERVAL_MS;

    private boolean stopped;
    private boolean focusing;
    private final boolean useCancel;
    private final boolean useAutoFocus;
    private final boolean useAFBinary;
    @SuppressWarnings("deprecation")
    private final Camera camera;
    private Handler handler;

    private int MESSAGE_FOCUS = 1;

    @SuppressWarnings("deprecation")
    private final Camera.AutoFocusCallback afRepeat = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera theCamera) {
//            FL.i(TAG, "onAutoFocus focus mode = [%s], focus success ? [%s], AUTO_FOCUS_INTERVAL_MS = [%s] <<<", theCamera.getParameters().getFocusMode(), success, String.valueOf(AUTO_FOCUS_INTERVAL_MS));
            handler.post(repeatRunnable);
        }
    };

    @SuppressWarnings("deprecation")
    private final Camera.AutoFocusCallback afBinaryRepeat = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera theCamera) {
//            FL.i(TAG, "onAutoFocus focus mode = [%s], focus success ? [%s], AUTO_FOCUS_INTERVAL_MS = [%s] <<<", theCamera.getParameters().getFocusMode(), success, String.valueOf(AUTO_FOCUS_INTERVAL_MS));
            if (!success)
                handler.post(repeatRunnable);
            else
                handler.post(cancelRunnable);
        }
    };

    private final Handler.Callback focusHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_FOCUS) {
                focus();
                return true;
            }
            return false;
        }
    };

    private final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            focusing = false;
            autoFocusAgainLater();
        }
    };

    private final Runnable cancelRunnable = new Runnable() {
        @Override
        public void run() {
            autoFocusCancel();
        }
    };

    @SuppressWarnings("deprecation")
    AutoFocusManager(Camera camera) {

        this.handler = new Handler(focusHandlerCallback);
        this.camera = camera;
        this.AUTO_FOCUS_INTERVAL_MS = 500l;
        this.useAutoFocus = true;
        this.useCancel = false;
        this.useAFBinary = false;
        start();
    }

    AutoFocusManager(Camera camera, long interval) {
        this.handler = new Handler(focusHandlerCallback);
        this.camera = camera;
        this.AUTO_FOCUS_INTERVAL_MS = interval;
        this.useAutoFocus = true;
        this.useCancel = false;
        this.useAFBinary = false;
        start();
    }

    AutoFocusManager(Camera camera, long interval, boolean useCancel) {
        this.handler = new Handler(focusHandlerCallback);
        this.camera = camera;
        this.AUTO_FOCUS_INTERVAL_MS = interval;
        this.useCancel = useCancel;
        this.useAutoFocus = true;
        this.useAFBinary = false;
        start();
    }

    AutoFocusManager(Camera camera, long interval, boolean useCancel, boolean useBinary) {
        this.handler = new Handler(focusHandlerCallback);
        this.camera = camera;
        this.AUTO_FOCUS_INTERVAL_MS = interval;
        this.useCancel = useCancel;
        this.useAutoFocus = true;
        this.useAFBinary = useBinary;
        start();
    }

    private synchronized void autoFocusAgainLater() {
        if (!stopped && !handler.hasMessages(MESSAGE_FOCUS)) {
//            FL.w(TAG, "repeat auto focus <<<");
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FOCUS), AUTO_FOCUS_INTERVAL_MS);
        }
    }

    private synchronized void autoFocusCancel() {
        if (!stopped /* && !handler.hasMessages(MESSAGE_FOCUS) */) {
//            FL.w(TAG, "cancel auto focus <<<");
            try {
                camera.cancelAutoFocus();
            } catch (Exception e) {
                FL.w(TAG, "Unexpected exception while cancelAutoFocus", e);
            }
            if (!focusing)
                handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FOCUS), AUTO_FOCUS_INTERVAL_MS);
//            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FOCUS), AUTO_FOCUS_INTERVAL_MS);
        }
    }

    /**
     * Start auto-focus. The first focus will happen now, then repeated every two seconds.
     */
    private void start() {
        stopped = false;
        focus();
    }

    private void focus() {
        if (useAutoFocus) {
            if (!stopped && !focusing) {
                try {
                    if (useCancel) {
//                        FL.w(TAG, "cancel auto focus <<<<<");
                        camera.cancelAutoFocus();
                    }
                } catch (Exception e) {
                    FL.w(TAG, "Unexpected exception while cancelAutoFocus", e);
                }

                try {
                    FL.w(TAG, "doing auto focus <<<<<");
                    camera.autoFocus(afRepeat);
                    focusing = true;
                } catch (RuntimeException re) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    FL.w(TAG, "Unexpected exception while focusing", re);
                    // Try again later to keep cycle going
                    autoFocusAgainLater();
                }
            }
        }
    }

    private void cancelOutstandingTask() {
        handler.removeMessages(MESSAGE_FOCUS);
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Stop auto-focus.
     */
    void stop() {
        FL.w(TAG, "stop auto focus >>>>>");
        stopped = true;
        focusing = false;

        cancelOutstandingTask();

        if (useAutoFocus) {
            // Doesn't hurt to call this even if not focusing
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }
    }

    public long getAUTO_FOCUS_INTERVAL_MS() {
        return AUTO_FOCUS_INTERVAL_MS;
    }
}
