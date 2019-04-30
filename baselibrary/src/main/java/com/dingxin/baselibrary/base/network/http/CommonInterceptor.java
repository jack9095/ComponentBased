package com.dingxin.baselibrary.base.network.http;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.dingxin.baselibrary.utils.LogUtil;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 添加通用请求参数的拦截器
 */
public class CommonInterceptor implements Interceptor {
    private static final String TAG = CommonInterceptor.class.getSimpleName();

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
//        String token = MainDataManager.getInstance().getToken();

        Request request = chain.request().newBuilder()
                .addHeader("Authorization","")
                .addHeader("X-KEY", "")
                .build();

        LogUtil.e(TAG, "request:" + request.toString());
        Response response = chain.proceed(request);
        MediaType mediaType = response.body().contentType();
        String content = response.body().string();
        LogUtil.e(TAG, "response body:" + content);
        return response.newBuilder()
                .body(ResponseBody.create(mediaType, content))
                .build();
    }
}
