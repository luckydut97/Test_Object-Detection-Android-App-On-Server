package com.luckydut.ondeviceaitest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/upload_image")
    Call<DetectionResult> uploadImage(@Body ImageData imageData);
}