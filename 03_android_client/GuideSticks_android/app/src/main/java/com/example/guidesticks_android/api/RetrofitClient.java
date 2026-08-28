package com.example.guidesticks_android.api;

import android.content.Context;

import com.example.guidesticks_android.AppSettings;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance;
    private static String activeBaseUrl;
    private final ApiService apiService;

    private RetrofitClient(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        String baseUrl = new AppSettings(context).getServerUrl();
        if (instance == null || !baseUrl.equals(activeBaseUrl)) {
            instance = new RetrofitClient(baseUrl);
            activeBaseUrl = baseUrl;
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
        activeBaseUrl = null;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
