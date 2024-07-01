package com.luckydut.ondeviceaitest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/upload_image") // 211 실제 서버
    //@POST("/upload_image") // 212 테스트 서버
    Call<DetectionResult> uploadImage(@Body ImageData imageData);
}