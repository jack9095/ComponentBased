package com.dingxin.baselibrary.utils;


import com.dingxin.baselibrary.R;
import com.dingxin.baselibrary.test.bean.TestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fei.wang on 2019/4/29.
 */

public class BannerDataUtil {
    /**
     * 获取banner数据
     *
     * @return
     */
    public static List<TestResponse> getBannerData() {
        List<TestResponse> listChilds = new ArrayList<>();
        TestResponse banner;

        banner = new TestResponse();
        banner.code = 1;
        listChilds.add(banner);

        return listChilds;
    }
}
