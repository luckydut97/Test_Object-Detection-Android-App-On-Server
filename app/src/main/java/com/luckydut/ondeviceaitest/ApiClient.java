package com.luckydut.ondeviceaitest;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://1.214.191.211:5052/"; // 211 실제서버
    //private static final String BASE_URL = "http://1.214.191.212:3000/"; // 212 테스트서버
    // 주소 변경 시 res/xml/network-security-config.xml 에서도 주소 변경해 줘야함 !
    // Why? 안드로이드에서 앱이 HTTP 연결을 허용하지 않으면 'CLEARTEXT communication to 1.214.191.212 not permitted by' 오류가 발생함.
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}