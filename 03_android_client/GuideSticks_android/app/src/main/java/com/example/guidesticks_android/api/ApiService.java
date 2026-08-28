package com.example.guidesticks_android.api;

import com.example.guidesticks_android.model.IdentifyRequest;
import com.example.guidesticks_android.model.IdentifyResponse;
import com.example.guidesticks_android.model.AssistantRequest;
import com.example.guidesticks_android.model.AssistantResponse;
import com.example.guidesticks_android.model.DeviceStatusResponse;
import com.example.guidesticks_android.model.LoginResponse;
import com.example.guidesticks_android.model.RegisterResponse;
import com.example.guidesticks_android.model.SensorContextRequest;
import com.example.guidesticks_android.model.User;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("android/test/")
    Call<ResponseBody> health();

    @GET("android/device/status/")
    Call<DeviceStatusResponse> deviceStatus();

    @POST("android/register/")
    Call<RegisterResponse> register(@Body Map<String, String> params);

    @POST("android/login/")
    Call<LoginResponse> login(@Body User user);

    @POST("android/identify/")
    Call<IdentifyResponse> identify(@Body IdentifyRequest request);

    @POST("android/assistant/chat/")
    Call<AssistantResponse> chat(@Body AssistantRequest request);

    @POST("android/sensor/context/")
    Call<ResponseBody> postSensorContext(@Body SensorContextRequest request);

    @GET("android/find_stick/")
    Call<Void> findStick();

    @GET("android/find_stick_status/")
    Call<Boolean> checkStickStatus();
}
