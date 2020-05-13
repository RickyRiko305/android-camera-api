package com.example.sampleapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    File caseFile;

    CascadeClassifier faceDetected;

    private Mat mRgba,mGrey;
    ImageView ivBitmap;

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private TextureView textureViewCamera;

    private Camera camera;
    private byte mBuffer[];

    private ImageReader mImageReader = ImageReader.newInstance(1280, 720, ImageFormat.YV12, 30);

    int mWidth;// = 1280;
    int mHeight;// = 720;

    int mYSize;
    int mUVSize;// = mYSize/4;
    int mFrameSize;// = mYSize+(mUVSize*2);

    byte[] tempYbuffer;// = new byte[mYSize];
    byte[] tempUbuffer;// = new byte[mUVSize];
    byte[] tempVbuffer;// = new byte[mUVSize];

    private HandlerThread mCameraHandlerThread;
    private Handler mCameraHandler;

    private int mFrameWidth;
    private int mFrameHeight;

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        textureViewCamera = findViewById(R.id.textureViewCamera);
        ivBitmap = findViewById(R.id.ivBitmap);

        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseCallback);
        }
        else{
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (allPermissionsGranted()) {
            //startCamera();
            if (textureViewCamera.isAvailable()) {
                try {
                    setupCamera(textureViewCamera.getWidth(), textureViewCamera.getHeight());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startCameraPreview(textureViewCamera.getSurfaceTexture());
                textureViewCamera.setSurfaceTextureListener(this);
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        textureViewCamera.setSurfaceTextureListener(this);


         mYSize = mWidth*mHeight;
         mUVSize = mYSize/4;
         mFrameSize = mYSize+(mUVSize*2);


        tempYbuffer = new byte[mYSize];
        tempUbuffer = new byte[mUVSize];
        tempVbuffer = new byte[mUVSize];

//        ImageReader.OnImageAvailableListener mImageAvailListener = new ImageReader.OnImageAvailableListener() {
//            @Override
//            public void onImageAvailable(ImageReader reader) {
//                //when a buffer is available from the camera
//                //get the image
//                Image image = reader.acquireNextImage();
//                Image.Plane[] planes = image.getPlanes();
//
//                //copy it into a byte[]
//                byte[] outFrame = new byte[mFrameSize];
//                int outFrameNextIndex = 0;
//
//
//                ByteBuffer sourceBuffer = planes[0].getBuffer();
//                sourceBuffer.get(tempYbuffer, 0, tempYbuffer.length);
//
//                ByteBuffer vByteBuf = planes[1].getBuffer();
//                vByteBuf.get(tempVbuffer);
//
//                ByteBuffer yByteBuf = planes[2].getBuffer();
//                yByteBuf.get(tempUbuffer);
//
//                //free the Image
//                image.close();
//            }
//        };
//        Surface mCameraRecieverSurface = mImageReader.getSurface();
//        {
//            mImageReader.setOnImageAvailableListener(mImageAvailListener, mCameraHandler);
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (allPermissionsGranted()){
            if (textureViewCamera.isAvailable()) {
                try {
                    setupCamera(textureViewCamera.getWidth(), textureViewCamera.getHeight());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startCameraPreview(textureViewCamera.getSurfaceTexture());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textureViewCamera.setSurfaceTextureListener(null);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        if (allPermissionsGranted()){
            try {
                setupCamera(width, height);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startCameraPreview(surfaceTexture);
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        stopCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        final Bitmap bitmap = textureViewCamera.getBitmap();

        if(bitmap==null)
            return;

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        MatOfRect faceDetections = new MatOfRect();

        faceDetected.detectMultiScale(mat,faceDetections);

        for (Rect rect: faceDetections.toArray()){
            Imgproc.rectangle(mat,new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255,0,0), 3);
        }

        Utils.matToBitmap(mat, bitmap);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivBitmap.setImageBitmap(bitmap);
            }
        });
    }

    private void setupCamera(int width, int height) throws IOException {
        mWidth = width;
        mHeight = height;
        camera = CameraV1Util.openCamera(cameraId);
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size bestPreviewSize = CameraV1Util.getBestPreviewSize(parameters.getSupportedPreviewSizes(), width, height);
        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        Camera.Size bestPictureSize = CameraV1Util.getBestPictureSize(parameters.getSupportedPictureSizes());
        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
        if (CameraV1Util.isContinuousFocusModeSupported(parameters.getSupportedFocusModes())) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        parameters = camera.getParameters();

        mFrameWidth = parameters.getPreviewSize().width;
        mFrameHeight = parameters.getPreviewSize().height;
        int size = (parameters.getPreviewSize().width) * (parameters.getPreviewSize().height);
        size  = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        mBuffer = new byte[size];

        camera.addCallbackBuffer(mBuffer);
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                //call
                Mat mat = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
                mat.put(0,0,bytes);
            }
        });
        camera.setDisplayOrientation(CameraV1Util.getCameraDisplayOrientation(this, cameraId));
        textureViewCamera.setTransform(CameraV1Util.getCropCenterScaleMatrix(width, height, bestPreviewSize.width, bestPreviewSize.height));
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.YV12, 30);

    }

    private void startCameraPreview(SurfaceTexture surfaceTexture) {
        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error start camera preview: " + e.getMessage());
        }
    }

    private void stopCamera() {
        try {
            camera.stopPreview();
            camera.release();
        } catch (Exception e) {
            Log.e(TAG, "Error stop camera preview: " + e.getMessage());
        }
    }


    private BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    caseFile = new File(cascadeDir,"haarcascade_frontalface_alt2.xml");

                    FileOutputStream fos = new FileOutputStream(caseFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while((bytesRead = inputStream.read(buffer))!=-1){
                        fos.write(buffer,0,bytesRead);
                    }

                    inputStream.close();
                    fos.close();

                    faceDetected = new CascadeClassifier(caseFile.getAbsolutePath());

                    if(faceDetected.empty()){
                        faceDetected = null;
                    }
                    else {
                        cascadeDir.delete();
                    }

                    //javaCameraView.enableView();

                }
                break;

                default:{
                    super.onManagerConnected(status);
                }

            }

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //startCamera();
                if (textureViewCamera.isAvailable()) {
                    try {
                        setupCamera(textureViewCamera.getWidth(), textureViewCamera.getHeight());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startCameraPreview(textureViewCamera.getSurfaceTexture());
                    textureViewCamera.setSurfaceTextureListener(this);
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
