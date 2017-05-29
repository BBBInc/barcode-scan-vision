package com.bbbtech.barcodescan;

import com.google.android.gms.common.images.Size;

/**
 * Created by Wooseong Kim in barcode-reader on 2017. 5. 29.
 *
 * CameraSourcePreviewCallback
 */
public interface CameraSourcePreviewCallback {
    void onCameraPreviewSizeDetermined(Size previewSize);
}
