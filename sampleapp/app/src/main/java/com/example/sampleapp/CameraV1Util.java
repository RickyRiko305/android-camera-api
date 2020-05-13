package com.example.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraV1Util {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static boolean isCameraSupport(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static Camera openDefaultCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error open camera: " + e.getMessage());
        }
        return camera;
    }

    public static Camera openCamera(int cameraId) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error open camera: " + e.getMessage());
        }
        return camera;
    }

    public static int getNumberOfCamera() {
        return Camera.getNumberOfCameras();
    }

    public static Camera.Size getBestPictureSize(@NonNull List<Camera.Size> pictureSizeList) {
        Camera.Size bestPictureSize = null;
        for (Camera.Size pictureSize : pictureSizeList) {
            if (bestPictureSize == null ||
                    (pictureSize.height >= bestPictureSize.height &&
                            pictureSize.width >= bestPictureSize.width)) {
                bestPictureSize = pictureSize;
            }
        }
        return bestPictureSize;
    }

    public static Camera.Size getBestPreviewSize(@NonNull List<Camera.Size> previewSizeList, int previewWidth, int previewHeight) {
        Camera.Size bestPreviewSize = null;
        for (Camera.Size previewSize : previewSizeList) {
            if (bestPreviewSize != null) {
                int diffBestPreviewWidth = Math.abs(bestPreviewSize.width - previewWidth);
                int diffPreviewWidth = Math.abs(previewSize.width - previewWidth);
                int diffBestPreviewHeight = Math.abs(bestPreviewSize.height - previewHeight);
                int diffPreviewHeight = Math.abs(previewSize.height - previewHeight);
                if (diffPreviewWidth + diffPreviewHeight < diffBestPreviewWidth + diffBestPreviewHeight) {
                    bestPreviewSize = previewSize;
                }
            } else {
                bestPreviewSize = previewSize;
            }
        }
        return bestPreviewSize;
    }

    public static boolean isContinuousFocusModeSupported(List<String> supportedFocusModes) {
        if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
            for (String focusMode : supportedFocusModes) {
                if (focusMode != null && focusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getCameraDisplayOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        int orientation = 0;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (cameraInfo.orientation + degree) % 360;
            orientation = (360 - orientation) % 360;
        } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            orientation = (cameraInfo.orientation - degree + 360) % 360;
        }
        return orientation;
    }

    public static Matrix getCropCenterScaleMatrix(float viewWidth, float viewHeight, float previewWidth, float previewHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        if (previewWidth > viewWidth && previewHeight > viewHeight) {
            scaleX = previewWidth / viewWidth;
            scaleY = previewHeight / viewHeight;
        } else if (previewWidth < viewWidth && previewHeight < viewHeight) {
            scaleY = viewWidth / previewWidth;
            scaleX = viewHeight / previewHeight;
        } else if (viewWidth > previewWidth) {
            scaleY = (viewWidth / previewWidth) / (viewHeight / previewHeight);
        } else if (viewHeight > previewHeight) {
            scaleX = (viewHeight / previewHeight) / (viewWidth / previewWidth);
        }
        return createScaleMatrix(scaleX, scaleY, viewWidth, viewHeight);
    }


    private static Matrix createScaleMatrix(float scaleX, float scaleY, float width, float height) {
        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, width / 2, height / 2);
        return matrix;
    }


    private static int getOrientationExifValue(int orientation) {
        switch (orientation) {
            case 90:
                return ExifInterface.ORIENTATION_ROTATE_90;
            case 180:
                return ExifInterface.ORIENTATION_ROTATE_180;
            case 270:
                return ExifInterface.ORIENTATION_ROTATE_270;
            default:
                return ExifInterface.ORIENTATION_NORMAL;
        }
    }

}
