package com.chivumarius.qrcodescanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class MainActivity extends AppCompatActivity {


    // ▼ "CAMERA PROVIDER" ▼
    private ListenableFuture cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private MyImageAnalyzer analyzer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ▼ "INITIALIZE" THE "WIDGET" ▼
        previewView = findViewById(R.id.previewView);
        // ▼ "SETTING" THE "FLAGS" ▼
        this.getWindow().setFlags(1024, 1024);


        // ▼ "BACKGROUND SERVICE" ("JOB") ▼
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);


        // ▼ INSTANTIATE "MY IMAGE ANALYZER" CLASS ▼
        analyzer = new MyImageAnalyzer(getSupportFragmentManager());


        // ▼ "CAMERA PROVIDER FUTURE" ▼
        cameraProviderFuture.addListener(
                new Runnable() {

                    // ▼ "RUNNABLE" ▼
                    public void run() {
                        // ▼ "DONE" IN "BACKGROUND JOB" ▼

                        // ▼ "BLOCKS" ▼
                        try {

                            // ▼ CHECKING PERMISSIONS ▼
                            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != (PackageManager.PERMISSION_GRANTED)){
                                // ▼ "REQUESTING PERMISSIONS" ▼
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,new String[]{
                                                Manifest.permission.CAMERA
                                              },
                                        101
                                );

                            } else{

                                // ▼ GETTING "PROCESS CAMERA PROVIDER" ▼
                                ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();

                                // ▼ CALLING "BIND PREVIEW()" METHOD ▼
                                bindpreview(processCameraProvider);
                            }

                        } catch (ExecutionException e) {
                            e.printStackTrace();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },

                ContextCompat.getMainExecutor(this)
        );
    }



    // ▼ "ON REQUEST PERMISSIONS RESULT()" METHOD ▼
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        // ▼ CHECKING PERMISSIONS ▼
        if (requestCode == 101 && grantResults.length > 0) {

            // ▼ CREATING AN INSTANCE OF "PROCESS CAMERA PROVIDER" ▼
            ProcessCameraProvider processCameraProvider = null;


            // ▼ "BLOCKS" ▼
            try {
                // ▼ INITIALIZING "PROCESS CAMERA PROVIDER" ▼
                processCameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();

            } catch (ExecutionException e) {
                e.printStackTrace();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // ▼ CALLING "BIND PREVIEW()" METHOD ▼
            bindpreview(processCameraProvider);
        }
    }




    // ▼ "BIND PREVIEW()" METHOD ▼
    private void bindpreview(ProcessCameraProvider processCameraProvider) {
        // ▼ CREATING AN "INSTANCE" OF "PREVIEW"
        Preview preview = new Preview.Builder().build();

        // ▼ USING BACK CAMERA FOR "PREVIEW" ▼
        CameraSelector cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // ▼ GETTING "SURFACE PROVIDER" ▼
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // ▼ DECLARING "IMAGE CAPTURE" ▼
        ImageCapture imageCapture;

        // ▼ INITIALIZING "IMAGE CAPTURE" ▼
        imageCapture = new ImageCapture.Builder().build();


        // ▼ CREATING AN "INSTANCE" OF "IMAGE ANALYSIS" ▼
        ImageAnalysis imageAnalysis = new ImageAnalysis
                .Builder()
                .setTargetResolution(new Size(1280,720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer);

        // ▼ CALLING "UNBIND ALL()" METHOD ▼
        processCameraProvider.unbindAll();

        // ▼ BINDING "IMAGE ANALYSIS" TO "PREVIEW" ▼
        processCameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
        );
    }






    // ▼ "MY IMAGE ANALYZER(){}" INNER CLASS ▼
    public class MyImageAnalyzer implements ImageAnalysis.Analyzer {

        // ▼ C"REATING OBJECTS" FROM THE "CLASSES" ▼
        private FragmentManager fragmentManager;
        private bottom_dialog bd;


        // ▼ "CONSTRUCTOR" ▼
        public MyImageAnalyzer(FragmentManager fragmentManager) {
            // INITIALIZATION OF "OBJECTS" ▼
            this.fragmentManager = fragmentManager;
            bd = new bottom_dialog();
        }



        // ▼ "ANALYZE()" METHOD ▼
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // ▼ CALLING "SCAN BAR CODE()" METHOD ▼
            scanbarcode(image);
        }




        // ▼ THE "SCAN BAR CODE()" METHOD ▼
        private void scanbarcode(ImageProxy image) {

            // ▼ SUPPRESSING "UNSAFE OPT-IN ERROR" ▼
            @SuppressLint("UnsafeOptInUsageError")
            Image image1 = image.getImage();

            // ▼ IF IS NOT NULL ▼
            assert image1 != null;


            // ▼ CREATING AN "INSTANCE" OF "INPUT IMAGE" ▼
            InputImage inputImage = InputImage.fromMediaImage(
                    image1,
                    image.getImageInfo().getRotationDegrees()
            );


            // ▼ "OPTIONS" FOR THE "BARCODE SCANNER" ▼
            BarcodeScannerOptions options =
                    new BarcodeScannerOptions
                            .Builder()
                            .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_AZTEC)
                            .build();




            // ▼
            BarcodeScanner scanner = BarcodeScanning.getClient(options);



            // ▼ "TASK" WITH "LIST" OF "BARCODES" ▼
            Task<List<Barcode>> result = scanner.process(inputImage)

                    // ▼ "ON SUCCESS" METHOD ▼
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {

                        // ▼ "ON SUCCESS" METHOD ▼
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            // ▼ CALLING "READER BARCODE DATA()" METHOD ▼
                            readerBarcodeData(barcodes);
                        }
                    })


                    // ▼ "ON FAILURE" METHOD ▼
                    .addOnFailureListener(new OnFailureListener() {

                        // ▼ "ON FAILURE" METHOD ▼
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // ▼ "FAILED" TO "READ QR CODE" ▼
                            Toast.makeText(
                                    MainActivity.this,
                                    "Failed to Read QR Code",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })


                    // ▼ "ON COMPLETE" METHOD (DONE FOR BOTH SUCCESS & FAILURE)▼
                    .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {

                        // ▼ "ON COMPLETE" METHOD ▼
                        @Override
                        public void onComplete(@NonNull @NotNull Task<List<Barcode>> task) {
                            // ▼ "CLOSING" THE "IMAGE" ▼
                            image.close();
                        }
                    });}





        // ▼ THE "READER BARCODE DATA()" METHOD ▼
        private void readerBarcodeData(List<Barcode> barcodes) {

            // ▼ LOOPING THROUGH "BARCODES" ▼
            for (Barcode barcode: barcodes) {

                // ▼ "GETTING" THE "BOUNDING BOX" AND "CORNERS" ▼
                Rect bounds = barcode.getBoundingBox();
                Point[] corners = barcode.getCornerPoints();

                // ▼ "GETTING" THE "RAW VALUE" OF THE "BARCODE" ▼
                String rawValue = barcode.getRawValue();

                // ▼ "GETTING" THE "VALUE TYPE" OF THE "BARCODE" ▼
                int valueType = barcode.getValueType();



                // ▼ SWITCH STATEMENT FOR THE "VALUE TYPE" ▼
                switch (valueType) {

                    // ♦ "CASE 1" - FOR "WIFI" CONNECTION ♦
                    case Barcode.TYPE_WIFI:
                        // ▼ "GETTING" THE "SSID" AND "PASSWORD" ▼
                        String ssid = barcode.getWifi().getSsid();
                        String password = barcode.getWifi().getPassword();

                        // ▼ "GETTING" THE "ENCRYPTION TYPE" ▼
                        int type = barcode.getWifi().getEncryptionType();
                        break;

                    // ♦ "CASE 2" - FOR "URL" LINK ♦
                    case Barcode.TYPE_URL:

                        // ▼ IF THE "BOTTOM DIALOG" IS "NOT ADDED" ▼
                        if(!bd.isAdded()) {
                            // ▼ ADDING THE "BOTTOM DIALOG" ▼
                            bd.show(fragmentManager,"");
                        }


                        // ▼ IF THERE IS A BOTTOM DIALOG →
                        //      → THEN "GET" THE "URL" AND "DISPLAY" IT ▼
                        bd.fetchurl(barcode.getUrl().getUrl());
                        String title = barcode.getUrl().getTitle();
                        String url = barcode.getUrl().getUrl();
                        break;
                }
            }
        }
    }
}