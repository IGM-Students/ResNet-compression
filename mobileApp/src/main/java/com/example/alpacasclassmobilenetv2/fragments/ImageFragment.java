package com.example.alpacasclassmobilenetv2.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.alpacasclassmobilenetv2.R;
import com.example.alpacasclassmobilenetv2.helpers.ImageViewModel;

import java.util.Map;

public class ImageFragment extends Fragment {

    private ImageViewModel viewModel;
    private ImageView mImageView;

    public ImageFragment() {
        super(R.layout.fragment_camera);
    }

    private final String[] STORAGE_PERMISSION = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        viewModel.getImageUri().observe(requireActivity(), newUri -> {
            if (newUri != null) {
                mImageView.setImageURI(newUri);
            }
        });

        if (!hasStoragePermission()) {
            ActivityCompat.requestPermissions(getActivity(), STORAGE_PERMISSION, 0);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image, container, false);

        mImageView = rootView.findViewById(R.id.image_container);

        return  rootView;
    }



    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

}

