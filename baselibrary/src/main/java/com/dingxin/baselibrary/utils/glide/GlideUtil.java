package com.dingxin.baselibrary.utils.glide;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.dingxin.baselibrary.R;

/**
 * Created by fei.wang on 2019/4/29.
 */
public class GlideUtil {

    /**
     * 清除磁盘缓存
     * 只能在子线程执行
     */
    public static void clearDiskCache() {
//        ThreadUtil.excute(new Runnable() {
//            @Override
//            public void run() {
//                Glide.get(BaseApplication.getAppContext()).clearDiskCache();
//            }
//        });
    }

    /**
     * 清除内存缓存
     * 可以在UI线程执行
     */
    public static void clearMemory() {
//        Glide.get(WApplication.getInstance()).clearMemory();
    }

    public static void setImageUrl(Context context, String imageUrl, ImageView view) {
        if (!TextUtils.isEmpty(imageUrl)) {
            if (imageUrl.endsWith("gif")) {
                Glide.with(context)
                        .load(imageUrl)
                        .asGif()
                        .into(view);
            } else {
                Glide.with(context)
                        .load(imageUrl)
                        .asBitmap()
                        .into(view);
            }
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_launcher)
                    .asBitmap()
                    .into(view);
        }
    }

    public static void setImageCircle(Context context, String imageUrl, ImageView view) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(context)
                    .load(imageUrl)
                    .asBitmap()
                    .transform(new GlideCircleTransform(context))
                    .into(view);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_launcher)
                    .asBitmap()
                    .transform(new GlideCircleTransform(context))
                    .into(view);
        }
    }

    public static void loadPicture(Context context, String url, ImageView imageView) {
        if (!TextUtils.isEmpty(url) && url.contains(".gif")) {
            Glide
                .with(context.getApplicationContext())
                .load(url)
                .placeholder(R.drawable.ic_launcher)
                .error(R.drawable.ic_launcher)
                .centerCrop()
                .into(imageView);
        } else {
            Glide
                .with(context.getApplicationContext())
                .load(url)
                .asBitmap()
                .placeholder(R.drawable.ic_launcher)
                .error(R.drawable.ic_launcher)
                .into(imageView);
        }
    }
}
