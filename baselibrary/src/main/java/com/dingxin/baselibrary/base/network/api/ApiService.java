package com.dingxin.baselibrary.base.network.api;


import com.dingxin.baselibrary.base.network.response.Response;
import com.dingxin.baselibrary.test.bean.TestResponse;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @GET(URLConfig.work_banner_url)
    Observable<TestResponse> requestWorkBannerData();

    @POST("xxx")
    Observable<Response> postAddress(@Body RequestBody body);

    @FormUrlEncoded
    @POST("yyy")
    Observable<Response> postTest(@Field("code") String code);

}
