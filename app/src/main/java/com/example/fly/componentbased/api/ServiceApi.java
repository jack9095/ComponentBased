package com.example.fly.componentbased.api;

import com.example.fly.componentbased.bean.HomeResponse;

import retrofit2.http.GET;
import rx.Observable;

public interface ServiceApi {

    @GET("article/listproject/0/json")
    Observable<HomeResponse> requestHomeData();
}
