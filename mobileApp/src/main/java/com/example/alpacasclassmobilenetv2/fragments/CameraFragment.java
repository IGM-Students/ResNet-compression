package com.example.alpacasclassmobilenetv2.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.alpacasclassmobilenetv2.MainActivity;
import com.example.alpacasclassmobilenetv2.R;
import com.example.alpacasclassmobilenetv2.databinding.FragmentCameraBinding;
import com.example.alpacasclassmobilenetv2.helpers.ClassifierHelper;
import com.example.alpacasclassmobilenetv2.helpers.ImageViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.internal.ImageConvertUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding fragmentCameraBinding;
    private Preview preview;
    private PreviewView mCameraView;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private ImageViewModel viewModel;
    private ClassifierHelper classifierHelper = new ClassifierHelper();


    private Context safeContext;


    private final String[] CAMERA_PERMISSION = {
            Manifest.permission.CAMERA,
    };


    public CameraFragment() {
        super(R.layout.fragment_image);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        safeContext = context;
        classifierHelper.setupModel(context);
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =  ProcessCameraProvider.getInstance(safeContext);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                ProcessCameraProvider cameraProvider;
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases(cameraProvider);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }, ContextCompat.getMainExecutor(safeContext));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(512, 512))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                Map<String, Double> result;
                @SuppressLint("UnsafeOptInUsageError")
                Image image = imageProxy.getImage();
                InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
                Bitmap bitmap = null;
                try {
                    bitmap = ImageConvertUtils.getInstance().getUpRightBitmap(inputImage);
                } catch (MlKitException e) {
                    e.printStackTrace();
                }

                Bitmap finalBitmap = ClassifierHelper.preprocessBitmap(bitmap);

                FragmentActivity activity = getActivity();
                if (activity != null) {
                    result = ClassifierHelper.classifyImage(activity, finalBitmap);
                } else {
                    result = new HashMap<String, Double>();
                    result.put("-", 0.);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!isDetached()) {
                                MainActivity act = (MainActivity) getActivity();
                                if (act != null) {
                                    act.updateResultBar(result);
                                }
                            }
                        }
                    });
                }

                imageProxy.close();
            }
        });


        cameraProvider.unbindAll();
        try {

            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            preview.setSurfaceProvider(mCameraView.createSurfaceProvider());
        } catch (Exception e) {
            Log.e("error", "Use case binding failed", e);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        mCameraView = rootView.findViewById(R.id.camera);
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(getActivity(), CAMERA_PERMISSION, 0);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (hasCameraPermission()) {
            startCamera();
        } else {
            Toast.makeText(getContext(), "Please accept the permission", Toast.LENGTH_LONG).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (hasCameraPermission()) {
            startCamera();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}