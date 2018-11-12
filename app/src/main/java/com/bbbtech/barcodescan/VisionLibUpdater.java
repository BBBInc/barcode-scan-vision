package com.bbbtech.barcodescan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Seungyong Yun on 2017. 8. 29.
 * Modified by Levin Yu
 * @Desc 비전 라이브러리 업데이터
 *       엘리마크2에 구글계정을 설정하지 않고 사용할 경우 비전라이브러리를 사용하는데 필요한 파일이 없을 수 있음.
 *       필요한 파일이 없으면 비전라이브러리에서 바코드를 정상적으로 인식할 수 없음.
 *       이에 대한 해결책으로 엘리마크2 ROM의 시스템 디렉토리에 비전라이브러리 파일을 선탑재하고,
 *       앱의 런타임때 선탑재된 파일을 복사하여 문제를 해결하도록 하였음.
 *       기존엔 프로젝트 별로 BarcodeUtils란 클래스가 산재해 있었으나 엘리마크2에서 비전라이브러리를 사용하는 기능을
 *       구현하는 앱에선 모두 필요한 클래스이므로 아예 라이브러리에 포함시킬 필요가 있어 추가함.
 *
 *       비전라이브러리를 사용하기 전, fetchVisionLibrary8_7ifNeeded{@link VisionLibUpdater#fetchVisionLibrary8_7ifNeeded(Context)}
 *       를 호출해서 버전 체크 및 업데이트 수행.
 *
 *       가급적이면 fetchVisionLibrary11_3ifNeeded{@link VisionLibUpdater#fetchVisionLibrary11_3ifNeeded(Context)}를
 *       사용할 것을 추천함.
 *       비전라이브러리엔 바코드의 길이가 6자 미만(혹은 이하)이면 비전라이브러리에서 바코드를 인식하지 못하는 이슈가 있는데
 *       10.x버전에서 수정되었음.
 */
public class VisionLibUpdater {

    private static final int INVALID_VERSION = -1;
    private static String TAG = VisionLibUpdater.class.getSimpleName();

    private static final String BARCODE_LIB_VER_8_7 = "8.7";
    private static final String BARCODE_LIB_VER_11_3 = "11.3";

    private static final String PACKAGE_GOOGLE_PLAY_SERVICE = "com.google.android.gms";
    private static final String PACKAGE_GMS_VISION = "com.google.android.gms.vision";

    private static final String DIR_SYS_BBB_RES = "/system/bbb_res";
    private static final String DIR_APP_DATA = "/data/data";
    private static final String DIR_GMS_VISION = "files/" + PACKAGE_GMS_VISION;
    private static final String DIR_BARCODE = "barcode";
    private static final String DIR_SYS_VISION_8_7 =
            DIR_SYS_BBB_RES + "/_bar_/" + PACKAGE_GMS_VISION + "_" + BARCODE_LIB_VER_8_7;

    private static final String DIR_SYS_VISION_11_3 =
            DIR_SYS_BBB_RES + "/_bar_/" + PACKAGE_GMS_VISION + "_" + BARCODE_LIB_VER_11_3;

    private static final String DIR_DATA_VISION =
            DIR_APP_DATA + "/" + PACKAGE_GOOGLE_PLAY_SERVICE + "/" + DIR_GMS_VISION;

    private static final String DIR_SYS_VISION_BARCODE_8_7 = DIR_SYS_VISION_8_7 + "/" + DIR_BARCODE;
    private static final String DIR_SYS_VISION_BARCODE_11_3 = DIR_SYS_VISION_11_3 + "/" + DIR_BARCODE;
    private static final String DIR_DATA_VISION_BARCODE = DIR_DATA_VISION + "/" + DIR_BARCODE;
    private static final String PATH_DATA_BARCODE_LIB_V7A_LIBBARHOPPERSO = DIR_DATA_VISION_BARCODE
            + "/libs/armeabi-v7a/libbarhopper.so";

    private static final int VER_GOOGLEPLAYSERVICES_87 = 8705246;

    private static final int FLEN_LIBBARHOPPERSO_11_3 = 234768;
    private static final int BARCODELIB_VER_87 = 1;
    private static final int BARCODELIB_VER_113 = 2;


    /**
     * 구글 플레이서비스 8.7
     * @param context
     * @throws IOException
     */
    public static void fetchVisionLibrary8_7ifNeeded(Context context) throws IOException {
        if (readGoolglePlayServiceVersionCode(context.getPackageManager()) == VER_GOOGLEPLAYSERVICES_87) {
            if (readVisionLibVersion() == INVALID_VERSION) {
                fetchVisionLibrary8_7();
            }
        }
    }

    /**
     * 바코드의 길이가 6자 미만(혹은 이하)이면 비전라이브러리에서 바코드를 인식하지 못하는 이슈로 11.3버전을 복사하도록
     * 추가함.
     * 해당 이슈는 10.x에서 수정되었음.
     * @Author levin.yu
     * @param context
     * @throws IOException
     */
    public static void fetchVisionLibrary11_3ifNeeded(Context context) throws IOException {
        if (readGoolglePlayServiceVersionCode(context.getPackageManager()) == VER_GOOGLEPLAYSERVICES_87) {
            if (readVisionLibVersion() < BARCODELIB_VER_113) {
                fetchVisionLibrary11_3();
            }
        }
    }

    private static int readGoolglePlayServiceVersionCode(PackageManager packageManager) {
        try {
            return packageManager.getPackageInfo("com.google.android.gms", 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return INVALID_VERSION;
    }


    private static int readVisionLibVersion() {
        //  /data/data/com.google.android.gms/files/com.google.android.gms.vision/barcode/libs/armeabi-v7a/libbarhopper.so
        //  -rwxrwxrwx shell    shell      234768
        try {
            File visionLibFile = new File(PATH_DATA_BARCODE_LIB_V7A_LIBBARHOPPERSO);

            if (visionLibFile.exists()) {
                if (visionLibFile.length() >= FLEN_LIBBARHOPPERSO_11_3) {
                    return BARCODELIB_VER_113;
                } else {
                    return BARCODELIB_VER_87;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return INVALID_VERSION;
    }

    private static void fetchVisionLibrary8_7() throws IOException {
        prepareVisionLibraryDir();
        VisionLibUpdater.copyDir(DIR_SYS_VISION_BARCODE_8_7, DIR_DATA_VISION_BARCODE);
    }

    private static void fetchVisionLibrary11_3() throws IOException {
        prepareVisionLibraryDir();
        VisionLibUpdater.copyDir(DIR_SYS_VISION_BARCODE_11_3, DIR_DATA_VISION_BARCODE);
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private static void prepareVisionLibraryDir() {
        String visionLibDirPath = DIR_DATA_VISION_BARCODE;
        File destDir = new File(visionLibDirPath);
        if (!destDir.exists() && !destDir.isDirectory()) {
            if (destDir.mkdirs()) {
                Log.i(TAG, "Create dir " + visionLibDirPath);
            } else {
                Log.i(TAG, "[ERROR] Create dir " + visionLibDirPath);
            }
        }

        destDir.setReadable(true, false);
        destDir.setWritable(true, false);
        destDir.setExecutable(true, false);
    }

    private static void copyDir(String srcPath, String destPath) throws IOException {
        File srcDir = new File(srcPath);
        File destDir = new File(destPath);

        if (!srcDir.exists()) {
            Log.d(TAG, "Directory does not exist." + srcPath);
        } else {
            copyDir(srcDir, destDir);
        }
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private static void copyDir(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
                dest.setReadable(true, false);
                dest.setWritable(true, false);
                dest.setExecutable(true, false);
                Log.d(TAG, "Directory copied from " + src + "  to " + dest);
            }

            String files[] = src.list();
            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                destFile.setReadable(true, false);
                destFile.setWritable(true, false);
                destFile.setExecutable(true, false);
                //recursive copyDir
                copyDir(srcFile, destFile);
            }
        } else {
            InputStream inputStream = new FileInputStream(src);
            OutputStream outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            inputStream.close();
            outStream.close();
            Log.d(TAG, "File copied from " + src + " to " + dest);
        }
    }
}
