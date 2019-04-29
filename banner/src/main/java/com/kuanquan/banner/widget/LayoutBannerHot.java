package com.kuanquan.banner.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kuanquan.banner.R;
import com.kuanquan.banner.bean.BannerBean;
import com.kuanquan.banner.utils.BannerDataUtil;
import com.kuanquan.banner.utils.LogUtil;

import java.util.ArrayList;

/**
 * Created by fei.wang on 2019/4/29.
 */
public class LayoutBannerHot extends FrameLayout implements AppBanner.OnPageClickListener {

    AppBanner mAppBanner;
    LinearLayout mLinearLayout;
    ArrayList<ImageView> dotsList = new ArrayList<>();

    public LayoutBannerHot(@NonNull Context context) {
        super(context);
        initView();
    }

    public LayoutBannerHot(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public LayoutBannerHot(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LayoutBannerHot(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        View root = LayoutInflater.from(getContext()).inflate(R.layout.layout_banner_hot, this, true);
        mAppBanner = root.findViewById(R.id.app_banner);
        mLinearLayout = root.findViewById(R.id.ll_hot);


        mAppBanner.setData(BannerDataUtil.getBannerData(), this);
        mAppBanner.setScrollSpeed(mAppBanner);
        if (BannerDataUtil.getBannerData() != null && BannerDataUtil.getBannerData().size() > 0) {
            try {
                dotsList.clear();
                mLinearLayout.removeAllViews();
                for (int i = 0; i < BannerDataUtil.getBannerData().size(); i++) {
                    ImageView view = new ImageView(getContext());
                    if (i == 0) {
                        view.setImageResource(R.drawable.dots_focus);
                    } else {
                        view.setImageResource(R.drawable.dots_normal);
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(18, 18);
                    params.setMargins(5, 0, 5, 0);
                    mLinearLayout.addView(view, params);
                    dotsList.add(view);
                }
            } catch (Exception e) {
                LogUtil.e("小圆点异常 = " + e);
            }
        }
    }

    @Override
    public void onPageClick(BannerBean info) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i < dotsList.size(); i++) {
            if (position % dotsList.size() == i) {
                dotsList.get(i).setImageResource(R.drawable.dots_focus);
            } else {
                dotsList.get(i).setImageResource(R.drawable.dots_normal);
            }
        }
    }
}
