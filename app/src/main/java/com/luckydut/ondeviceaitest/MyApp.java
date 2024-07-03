package com.luckydut.ondeviceaitest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyApp extends Application {

    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Setup default uncaught exception handler to restart the app and save logs internally
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("UncaughtException", "Uncaught exception in thread " + thread.getName(), e);

            saveCrashLog(e);  // Save the crash log in internal storage

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplicationContext().startActivity(intent);

            System.exit(2); // Restart the process after a small delay
        });
    }

    //안드로이드 (내장메모리/Documents/crash.log_yyyyMMdd_HHmmss 로 저장됨.)
    private void saveCrashLog(Throwable e) {
        try {
            // 'Documents' 폴더에 접근합니다.
            File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!documents.exists()) {
                documents.mkdirs(); // 폴더가 없다면 생성
            }

            // 현재 시간을 기반으로 파일 이름 생성
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File logFile = new File(documents, "crash_" + timestamp + ".log");

            FileOutputStream fos = new FileOutputStream(logFile, true);
            PrintWriter pw = new PrintWriter(fos);
            e.printStackTrace(pw); // 스택 트레이스 저장
            pw.close();
            fos.close();
        } catch (Exception ex) {
            Log.e("MyApp", "Failed to save crash log", ex);
        }
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}