package com.dingxin.baselibrary.test.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 接口 json 数据对应的 bean
 */
public class TestResponse implements Serializable {
    public int code  ;
    public List<Data> data;
    public class Data implements Serializable  {
        public String picUrl  ;    // 图片链接
        public int id  ;
        public String title  ;      // 标题
    }
}
