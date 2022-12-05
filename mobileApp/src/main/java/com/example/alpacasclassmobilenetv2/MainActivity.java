package com.example.alpacasclassmobilenetv2;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alpacasclassmobilenetv2.fragments.CameraFragment;
import com.example.alpacasclassmobilenetv2.fragments.ImageFragment;
import com.example.alpacasclassmobilenetv2.helpers.ClassifierHelper;
import com.example.alpacasclassmobilenetv2.helpers.ImageViewModel;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageFragment ifa = new ImageFragment();
    private CameraFragment cfa = new CameraFragment();
    private ImageViewModel viewModel;
    private Button switchToCameraBtn, loadFromGalleryBtn, changeImageBtn;
    private TextView resultBar;
    private Map<String, Double> storeImageClassResult = new HashMap<>();

    private final String[] STORAGE_PERMISSION = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.switchToCameraBtn:
                buttonClickHandler(R.id.switchToCameraBtn);
                storeImageClassResult = viewModel.getClassResult().getValue();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, cfa, null)
                        .commit();
                updateResultBar(null);
                break;
            case R.id.loadFromGalleryBtn:
                buttonClickHandler(R.id.loadFromGalleryBtn);
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, ifa, null)
                        .commit();

                updateResultBar(storeImageClassResult);
                break;
            case R.id.changeImageBtn:
                if (hasStoragePermission()) {
                    selectImageFromGalleryResult.launch("image/*");
                } else {
                    ActivityCompat.requestPermissions(this, STORAGE_PERMISSION, 0);
                }
                break;
            default:
                break;
        }
    }

    private FragmentManager fragmentManager;

    private ActivityResultLauncher<String> selectImageFromGalleryResult = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {

                Bitmap bitmap = ClassifierHelper.loadBitmap(uri, this);

                Bitmap finalBitmap = ClassifierHelper.preprocessBitmap(this, uri, bitmap);

                Map<String, Double> result =  ClassifierHelper.classifyImage(this, finalBitmap);

                viewModel.setClassResult(result);
                viewModel.setImageUri(uri);
                Log.i("userInt", uri.toString());
            });


    public void updateResultBar(Map<String, Double> res) {
        if (res == null) {
            resultBar.setText("-");
            resultBar.setBackgroundColor(getResources().getColor(R.color.gray));

        } else if (res != null){
            if (res.size() != 0) {
                Map.Entry<String, Double> pair = res.entrySet().iterator().next();

                resultBar.setText(pair.getKey() + " - " + pair.getValue());
                if (pair.getKey().equals("Alpaca")) {
                    resultBar.setBackgroundColor(getResources().getColor(R.color.teal_200));
                } else {
                    resultBar.setBackgroundColor(getResources().getColor(R.color.gray));
                }
            } else {
                resultBar.setText("-");
                resultBar.setBackgroundColor(getResources().getColor(R.color.gray));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, CameraFragment.class, null)
                    .commit();
        }

        switchToCameraBtn = findViewById(R.id.switchToCameraBtn);
        loadFromGalleryBtn = findViewById(R.id.loadFromGalleryBtn);
        changeImageBtn = findViewById(R.id.changeImageBtn);

        switchToCameraBtn.setOnClickListener(this);
        loadFromGalleryBtn.setOnClickListener(this);
        changeImageBtn.setOnClickListener(this);

        resultBar = findViewById(R.id.result);

        viewModel.getClassResult().observe(this, newResult -> {
            if (newResult != null) {
                Map.Entry<String, Double> pair = newResult.entrySet().iterator().next();

                resultBar.setText(pair.getKey() + " - " + pair.getValue());
                if (pair.getKey().equals("Alpaca")) {
                    resultBar.setBackgroundColor(getResources().getColor(R.color.teal_200));
                } else {
                    resultBar.setBackgroundColor(getResources().getColor(R.color.gray));
                }
                resultBar.invalidate();
            }

        });

//        if (!hasCameraPermission()) {
//            ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, 0);
//        }
//        if (!hasStoragePermission()) {
//            ActivityCompat.requestPermissions(this, STORAGE_PERMISSION, 0);
//        }
    }


    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }



    private void buttonClickHandler(int id) {
        switch (id) {
            case R.id.switchToCameraBtn:
                switchToCameraBtn.setVisibility(View.GONE);
                loadFromGalleryBtn.setVisibility(View.VISIBLE);
                changeImageBtn.setVisibility(View.GONE);
                break;
            case R.id.loadFromGalleryBtn:
                switchToCameraBtn.setVisibility(View.VISIBLE);
                loadFromGalleryBtn.setVisibility(View.GONE);
                changeImageBtn.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }


}