/*
아직 적용 안함.
 */
package com.luckydut.ondeviceaitest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 핸들러를 사용하여 2초 지연 후 MainActivity 실행
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // MainActivity로 이동
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(intent);

                // SplashActivity 종료
                finish();
            }
        }, 2000); // 2초(2000밀리초) 후 실행
    }
}
