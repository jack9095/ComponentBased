package com.kuanquan.banner.utils;

import com.kuanquan.banner.bean.BannerBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fei.wang on 2019/4/29.
 */

public class BannerDataUtil {
    /**
     * 获取banner数据
     * @return
     */
    public static List<BannerBean> getBannerData(){
        List<BannerBean> listChilds = new ArrayList<>();
        BannerBean banner;
        for (int i = 0; i < 5; i++) {
            banner = new BannerBean();
            banner.imageUrl = "http://img5.imgtn.bdimg.com/it/u=3532743473,184108530&fm=200&gp=0.jpg";
            listChilds.add(banner);
        }
        return listChilds;
    }
}
