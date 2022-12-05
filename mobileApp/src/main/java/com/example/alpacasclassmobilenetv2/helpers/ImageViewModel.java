package com.example.alpacasclassmobilenetv2.helpers;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

public class ImageViewModel extends ViewModel {

    private MutableLiveData<Uri> _imageUri = new MutableLiveData<>();
    private MutableLiveData<Map<String, Double>> _classResult = new MutableLiveData<>();

    public void setImageUri(Uri uri) {
        _imageUri.setValue(uri);
    }

    public LiveData<Uri> getImageUri() {
        return _imageUri;
    }

    public void setClassResult(Map<String, Double> result) {
        _classResult.setValue(result);
    }

    public LiveData<Map<String, Double>> getClassResult() {
        return _classResult;
    }
}
