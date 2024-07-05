package com.luckydut.ondeviceaitest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApp extends Application {

    private static MyApp instance;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        apiService = ApiClient.getClient().create(ApiService.class);

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("UncaughtException", "Uncaught exception in thread " + thread.getName(), e);

            // 예외 발생 시 로그를 문자열로 저장
            String crashLog = getCrashLog(e);
            if (crashLog != null) {
                uploadCrashLog(crashLog);
            }

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplicationContext().startActivity(intent);

            System.exit(2); // Restart the process after a small delay
        });
    }

    private String getCrashLog(Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            return sw.toString();
        } catch (Exception ex) {
            Log.e("MyApp", "Failed to get crash log", ex);
            return null;
        }
    }

    private void uploadCrashLog(String crashLog) {
        try {
            // 현재 시간을 기반으로 파일 이름 생성
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "crash_" + timestamp + ".log";

            // 문자열을 RequestBody로 변환
            RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), crashLog);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);

            Call<ResponseBody> call = apiService.uploadCrashFile(body);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.d("MyApp", "Crash log uploaded successfully");
                    } else {
                        Log.e("MyApp", "Failed to upload crash log: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("MyApp", "Error uploading crash log", t);
                }
            });
        } catch (Exception ex) {
            Log.e("MyApp", "Failed to upload crash log", ex);
        }
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}

/*내장메모리 Documnets 디렉토리에도 저장하는 버전
public class MyApp extends Application {

    private static MyApp instance;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        apiService = ApiClient.getClient().create(ApiService.class);

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("UncaughtException", "Uncaught exception in thread " + thread.getName(), e);

            File logFile = saveCrashLog(e);  // Save the crash log in internal storage
            if (logFile != null) {
                uploadCrashFile(logFile);
            }

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplicationContext().startActivity(intent);

            System.exit(2); // Restart the process after a small delay
        });
    }

    // 안드로이드 (내장메모리/Documents/crash.logyyyyMMdd_HHmmss 로 저장됨.)
    private File saveCrashLog(Throwable e) {
        File logFile = null;
        try {
            // 'Documents' 폴더에 접근합니다
            File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!documents.exists()) {
                documents.mkdirs();//폴더가 없다면 생성
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            logFile = new File(documents, "crash_" + timestamp + ".log");

            FileOutputStream fos = new FileOutputStream(logFile, true);
            PrintWriter pw = new PrintWriter(fos);
            e.printStackTrace(pw); // 스택 트레이스 저장
            pw.close();
            fos.close();
        } catch (Exception ex) {
            Log.e("MyApp", "Failed to save crash log", ex);
        }
        return logFile;
    }

    private void uploadCrashFile(File file) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Call<ResponseBody> call = apiService.uploadCrashFile(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("MyApp", "Crash log uploaded successfully");
                } else {
                    Log.e("MyApp", "Failed to upload crash log: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("MyApp", "Error uploading crash log", t);
            }
        });
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
 */