package com.bbbtech.barcodescan;

import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;


/**
 * Created by Wooseong Kim in barcode-reader on 2016. 11. 30.
 *
 * BarcodeCropFocusingProcessor
 *  중앙 특정 영역 부근만 측정되도록 하는 로직
 */

public class BarcodeCropFocusingProcessor extends FocusingProcessor<Barcode> {
    private Rect cropFrameRect; // 카메라 소스 크기에 맞게 변환된 Crop 영역 Rect

    public BarcodeCropFocusingProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
        super(detector, tracker);
    }

    public void setPreviewRect(Rect previewRect, int paddingHorizontal, int paddingVertical) {
        int cropFrameWidth = previewRect.right - (paddingHorizontal * 2);
        int cropFrameHeight = previewRect.bottom - (paddingVertical * 2);

        Rect originalCropFrameRect = new Rect(
                paddingHorizontal, paddingVertical,
                paddingHorizontal + cropFrameWidth, paddingVertical + cropFrameHeight);

        // elemark 2 전용
        // 뷰와 소스의 비율 차이만큼 좌표 변환
        Rect previewSourceRect = new Rect(0, 0, 1080, 1440);
        float widthRatio = (float) previewSourceRect.width() / previewRect.width();
        float heightRatio = (float) previewSourceRect.height() / previewRect.height();

        this.cropFrameRect = new Rect(
                (int) (originalCropFrameRect.left * widthRatio), (int) (originalCropFrameRect.top * heightRatio),
                (int) (originalCropFrameRect.right * widthRatio), (int) (originalCropFrameRect.bottom * heightRatio));
    }

    @Override
    public int selectFocus(Detector.Detections<Barcode> detections) {
        SparseArray<Barcode> barcodes = detections.getDetectedItems();
        int detectedId = -1;

        for (int i = 0; i < barcodes.size(); ++i) {
            int id = barcodes.keyAt(i);

            Barcode barcode = barcodes.get(id);

            // 정상적인 상황에서 바코드가 측정 되었을 때, Crop 영역 외의 바코드는 무시하는 코드
            if (barcode != null && cropFrameRect != null) {
                Rect barcodeRect = barcode.getBoundingBox();

                // 일반적 상황: 여러 개의 바코드 중 cropRect 안에 들어오는 바코드가 존재할 경우
                if (cropFrameRect.contains(barcodeRect)) {
                    detectedId = id;
                }
                // 특수상황: 두 개 이상의 똑같은 바코드가 세로로 있는 상태에서 잡힐 때 Rect가 두 바코드가 붙은 상태로 나오는 상황을 캐치
                else if (barcodeRect.height() > cropFrameRect.height() &&
                        barcodeRect.left > cropFrameRect.left && barcodeRect.right < cropFrameRect.right) {
                    detectedId = id;
                }
            }
        }
        return detectedId;
    }
}
