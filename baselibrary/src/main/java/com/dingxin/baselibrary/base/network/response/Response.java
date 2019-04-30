package com.dingxin.baselibrary.base.network.response;

import java.io.Serializable;

/**
 * 返回结果封装
 */

public class Response<T> implements Serializable {

    public int code; // 返回的code
    public T data; // 具体的数据结果
    public String msg; // message 可用来返回接口的说明
}
