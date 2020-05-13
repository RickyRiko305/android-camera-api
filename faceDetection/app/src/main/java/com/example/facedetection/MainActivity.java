package com.example.facedetection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
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


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    JavaCameraView javaCameraView;
    File caseFile;

    CascadeClassifier faceDetected;

    private Mat mRgba,mGrey;

    private int Index=0;
    private Switch cameraMode;
    private Switch flashLight;
    private static final int CAMERA_REQUEST = 123;
    boolean hasCameraFlash = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);

        hasCameraFlash = getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        javaCameraView = (JavaCameraView)findViewById(R.id.javaCameraView);

        cameraMode = (Switch)findViewById(R.id.switch1);
        flashLight = (Switch)findViewById(R.id.switch2);


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


        if(Index == 1){
            javaCameraView.setCameraIndex(1);
        }
        else{
            javaCameraView.setCameraIndex(0);
        }


        cameraMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraMode.isChecked()){
                    javaCameraView.setCameraIndex(1);
                    Index =1;
                    onRestart();
                }
                else{
                    javaCameraView.setCameraIndex(0);
                    Index =0;
                    onRestart();
                }
            }
        });

        flashLight.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if (flashLight.isChecked()){
                    javaCameraView.turnOnTheFlash();
                }
                else{
                    javaCameraView.turnOffTheFlash();
                }
            }
        });
        javaCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGrey = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGrey.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();

        //MatOfRect faceDetections = new MatOfRect();

            if(Index == 1){

                Mat mRgbaT = mRgba.t();
                Core.flip(mRgba.t(),mRgbaT,-1);
                Mat mGreyT = mGrey.t();
                Core.flip(mGrey.t(),mGreyT,-1);

                MatOfRect faceDetections = new MatOfRect();

                faceDetected.detectMultiScale(mGreyT,faceDetections);

                for (Rect rect: faceDetections.toArray()){

                    Imgproc.rectangle(mRgbaT,new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255,0,0));
                }

                return mRgbaT;
            }
            else{
                Mat mRgbaT = mRgba.t();
                Core.flip(mRgba.t(),mRgbaT,1);
                Mat mGreyT = mGrey.t();
                Core.flip(mGrey.t(),mGreyT,1);
                Mat destination = new Mat(mGreyT.rows(),mGreyT.cols(),mGreyT.type());
                Imgproc.GaussianBlur(mGreyT,destination,new Size(11,11),1);
                MatOfRect faceDetections = new MatOfRect();
                //faceDetected.detectMultiScale(mGreyT,faceDetections);
                faceDetected.detectMultiScale(destination,faceDetections);

                for (Rect rect: faceDetections.toArray()){
                    Imgproc.rectangle(mRgbaT,new Point(rect.x,rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255,0,0));
                }


                return mRgbaT;
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

                    javaCameraView.enableView();

                }
                break;

                default:{
                    super.onManagerConnected(status);
                }

            }

        }
    };


    @Override
    protected void onRestart() {
        javaCameraView.disableView();
        if(Index == 1){
            javaCameraView.setCameraIndex(1);
        }
        else{
            javaCameraView.setCameraIndex(0);
        }
        javaCameraView.enableView();
        super.onRestart();
    }

}
