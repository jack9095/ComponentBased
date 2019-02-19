package com.example.fly.componentbased.test;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.example.fly.componentbased.R;
import com.example.fly.componentbased.retrofit.TestThreadExecutor;
import com.example.fly.componentbased.retrofit.test.ServiceApi;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//@Route(path = "/test/target")
public class TestActivity extends AppCompatActivity {

    @Autowired
    public String key3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);
        ARouter.getInstance().inject(this);
        Toast.makeText(this, key3, Toast.LENGTH_LONG).show();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.xxx.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .callbackExecutor(new TestThreadExecutor())
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
                System.out.println("*******" + Thread.currentThread().getName() + "&&&&&&&&");
            }

            //请求失败时候的回调
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable throwable) {
                System.out.println("连接失败");
                System.out.println("*******" + Thread.currentThread().getName() + "&&&&&&&&");
            }
        });
    }
}
