package com.example.fly.componentbased.retrofit.test;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ServiceApi {

    /**
     * method：网络请求的方法（区分大小写）
     * path：网络请求地址路径
     * hasBody：是否有请求体
     */
    @HTTP(method = "GET", path = "blog/{id", hasBody = false)
    Call<ResponseBody> getCall(@Path("id") int id);
    // {id 表示是一个变量
    // method 的值 retrofit 不会做处理，所以要自行保证准确


    /**
     * 表明是一个表单格式的请求（Content-Type:application/x-www-form-urlencoded）
     * Field("username") 表示将后面的 String name 中name的取值作为 username 的值
     */
    @POST("/form")
    @FormUrlEncoded
    Call<ResponseBody> testFormUrl(@Field("username") String name, @Field("password") int pwd);

    /**
     * Map的key作为表单的键
     */
    @POST("/form")
    @FormUrlEncoded
    Call<ResponseBody> testFormUrl(@FieldMap Map<String, Object> map);


    /**
     * Part 后面支持三种类型，RequestBody、okHttp3.MultipartBody.Part 、任意类型
     * 除 okHttp3.MultipartBody.Part 以外，其它类型都必须带上表单字段(okHttp3.MultipartBody.Part 中已经包含了表单字段的信息)，
     */
    @POST("/form")
    @Multipart
    Call<ResponseBody> testFileUpLoad(@Part("name") RequestBody name, @Part("age") RequestBody age, @Part MultipartBody.Part file);

    /**
     * PartMap 注解支持一个Map作为参数，支持 RequestBody 类型，
     * 如果有其它的类型，会被retrofit2.Converter转换，如后面会介绍的 使用Gson的 retrofit2.converter.gson.GsonRequestBodyConverter
     * 文件只能用 @Part MultipartBody.Part
     */
    @POST("/form")
    @Multipart
    Call<ResponseBody> testFileUpLoad(@PartMap Map<String, RequestBody> args, @Part MultipartBody.Part file);

    @POST("/form")
    @Multipart
    Call<ResponseBody> testFileUpLoad(@PartMap Map<String, RequestBody> args);


    // @Header 使用
    @GET("xxx")
    Call<Test> getUser(@Header("Authorization") String authorization);

    // @Headers 使用
    @Headers("Authorization: authorization")
    @GET("xxx")
    Call<Test> getUser();

    @GET("xxx/")
    Call<String> tag(@Query("type") String type);

    @GET("xxx/")
    Call<String> tag(@QueryMap Map<String, Object> args);
    // 访问的API是：https://api.github.com/users/{user}/repos

    @GET("xxx/{yyy}/zzz")
    Call<ResponseBody>  getGitHub(@Path("yyy") String yyy);
    // 访问的API是：https://api.github.com/xxx/{yyy}/zzz
    // 在发起请求时， {yyy} 会被替换为方法的第一个参数 yyy（被@Path注解作用）



}

