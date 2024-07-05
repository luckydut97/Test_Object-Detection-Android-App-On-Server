package com.luckydut.ondeviceaitest;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    // 211 실제 서버
    @POST("/api/upload_image")
    Call<DetectionResult> uploadImage(@Body ImageData imageData);
    @Multipart
    @POST("/api/upload_crash_file")
    Call<ResponseBody> uploadCrashFile(@Part MultipartBody.Part file);

    /*// 212 테스트 서버
    @POST("/upload_image")
    Call<DetectionResult> uploadImage(@Body ImageData imageData);

    @Multipart
    @POST("/upload_crash_file")
    Call<ResponseBody> uploadCrashFile(@Part MultipartBody.Part file);*/

}