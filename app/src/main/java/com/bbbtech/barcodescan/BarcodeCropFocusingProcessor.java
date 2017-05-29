package com.bbbtech.barcodescan;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;


/**
 * Created by Wooseong Kim in barcode-reader on 2016. 11. 30.
 *
 * BarcodeCropFocusingProcessor
 *  중앙 특정 영역 부근만 측정되도록 하는 로직
 *
 *  setCameraSourceSize()와 setPreviewRect()를 반드시 호출해 주어야 함
 *
 *  BarcodeTracker tracker = new BarcodeTracker(this, this, false);
 *  final BarcodeCropFocusingProcessor focusingProcessor = new BarcodeCropFocusingProcessor(barcodeDetector, tracker);
 *  mPreview.setCallback(new CameraSourcePreviewCallback() {
 *      public void onCameraPreviewSizeDetermined(Size previewSize) {
 *          focusingProcessor.setCameraSourceSize(previewSize);
 *      }
 *  });
 *  barcodeDetector.setProcessor(focusingProcessor);
 *
 *  mPreview.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
 *      public boolean onPreDraw() {
 *          mPreview.getViewTreeObserver().removeOnPreDrawListener(this);
 *
 *          Rect visibleRect = new Rect();
 *          mPreview.getLocalVisibleRect(visibleRect);
 *
 *          focusingProcessor.setPreviewRect(visibleRect, mPreview.getPaddingLeft(), mPreview.getPaddingTop());
 *          return true;
 *      }
 *  });
 */
public class BarcodeCropFocusingProcessor extends FocusingProcessor<Barcode> {
    private Size cameraSourceSize;  // 카메라 소스 크기
    private Rect previewRect;       // 카메라 프리뷰 영역 전체
    private int paddingHorizontal;
    private int paddingVertical;
    private Rect cropFrameRect;     // 카메라 프리뷰가 표시되는 뷰에서 padding 부분을 제외한 영역

    private float widthScaleFactor = 1;
    private float heightScaleFactor = 1;

    public BarcodeCropFocusingProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
        super(detector, tracker);
    }

    public void setCameraSourceSize(Size cameraSourceSize) {
        this.cameraSourceSize = cameraSourceSize;

        if (this.cameraSourceSize != null && previewRect != null) {
            calculateCropFrameRect();
        }
    }

    public void setPreviewRect(Rect previewRect, int paddingHorizontal, int paddingVertical) {
        this.previewRect = previewRect;
        this.paddingHorizontal = paddingHorizontal;
        this.paddingVertical = paddingVertical;

        if (cameraSourceSize != null && this.previewRect != null) {
            calculateCropFrameRect();
        }
    }

    private void calculateCropFrameRect() {
        int cropFrameWidth = previewRect.right - (paddingHorizontal * 2);
        int cropFrameHeight = previewRect.bottom - (paddingVertical * 2);

        // 뷰와 카메라소스의 비율 차이만큼 좌표 변환
        widthScaleFactor = (float) previewRect.width() / cameraSourceSize.getHeight();
        heightScaleFactor = (float) previewRect.height() / cameraSourceSize.getWidth();

        this.cropFrameRect = new Rect(
                paddingHorizontal, paddingVertical,
                paddingHorizontal + cropFrameWidth, paddingVertical + cropFrameHeight);
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
                RectF barcodeRectF = getTranslatedRect(barcode.getBoundingBox());

                // 일반적 상황: 여러 개의 바코드 중 cropRect 안에 들어오는 바코드가 존재할 경우
                Rect barcodeRect = new Rect();
                barcodeRectF.roundOut(barcodeRect);
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

    private RectF getTranslatedRect(Rect rect) {
        RectF rectF = new RectF(rect);

        rectF.left = translateX(rectF.left);
        rectF.top = translateY(rectF.top);
        rectF.right = translateX(rectF.right);
        rectF.bottom = translateY(rectF.bottom);

        return rectF;
    }

    // 이하 구글 비전 라이브러리에서 가져온 메서드들
    /**
     * Adjusts a horizontal value of the supplied value from the preview scale to the view
     * scale.
     */
    private float scaleX(float horizontal) {
        return horizontal * widthScaleFactor;
    }

    /**
     * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
     */
    private float scaleY(float vertical) {
        return vertical * heightScaleFactor;
    }

    /**
     * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
     * system.
     */
    private float translateX(float x) {
        // 중앙에서 스캔을 하는 것이라 우선 불필요하다고 판단
//        if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
//            return mOverlay.getWidth() - scaleX(x); // 대칭 -x + 2a (a = 대칭 선이 되는 x절편 좌표 값, a가 중앙값이니 * 2 해서 getWidth())
//        } else {
        return scaleX(x);
//        }
    }

    /**
     * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
     * system.
     */
    private float translateY(float y) {
        return scaleY(y);
    }
}
