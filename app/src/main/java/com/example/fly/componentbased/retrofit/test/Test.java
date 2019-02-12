package com.example.fly.componentbased.retrofit.test;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Test {

    public static void main(String[] args) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.xxx.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ServiceApi service = retrofit.create(ServiceApi.class);
        // @Query
        Call<String> call = service.tag("android");

        // @QueryMap
        // 实现的效果与上面相同，但要传入Map
        Map<String, Object> map = new HashMap<>();
        map.put("type", "android");
//        Call<String> call = service.tag(map);

        //发送网络请求(异步)
        call.enqueue(new Callback<String>() {
            //请求成功时回调
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                //请求处理,输出结果
                assert response.body() != null;
                response.body().toString();
            }

            //请求失败时候的回调
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable throwable) {
                System.out.println("连接失败");
            }
        });
    }
}
