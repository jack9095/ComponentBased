package com.example.fly.componentbased.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 接口 json 数据对应的 bean
 */
public class HomeResponse implements Serializable {

    public int errorCode  ;
    public String errorMsg  ;
    public  Data data;
    public class Data implements Serializable  {
        public boolean over  ;
        public int pageCount  ;
        public int total  ;
        public int curPage  ;
        public int offset  ;
        public int size  ;
        public List<Datas> datas;
        public class Datas implements Serializable  {
            public String superChapterName  ; // tag
            public long publishTime  ;
            public int visible  ;
            public String niceDate  ;
            public String projectLink  ;
            public String author  ;
            public int zan  ;
            public String origin  ;
            public String chapterName  ;  // 名称
            public String link  ;
            public String title  ;
            public int type  ;
            public int userId  ;
            public String apkLink  ;
            public String envelopePic  ;  // 图片
            public int chapterId  ;
            public int superChapterId  ;
            public int id  ;
            public boolean fresh  ;
            public boolean collect  ;
            public int courseId  ;
            public String desc  ;    // 描述
            public List<Tags> tags;
            public class Tags implements Serializable  {
                public String name  ;
                public String url  ;
            }
        }
    }

}
