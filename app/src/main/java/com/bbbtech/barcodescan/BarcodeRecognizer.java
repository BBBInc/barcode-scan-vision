package com.bbbtech.barcodescan;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * Created by Wooseong Kim in Em2-Glucose-Pro on 2016. 11. 30.
 *
 * BarcodeRecognizer
 */

public interface BarcodeRecognizer {
    void onRecognized(Barcode item);
}
