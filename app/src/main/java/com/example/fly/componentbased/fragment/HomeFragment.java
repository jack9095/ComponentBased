package com.example.fly.componentbased.fragment;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.example.fly.mvvm.AdapterPool;
import com.example.fly.mvvm.R;
import com.example.fly.mvvm.base.BaseListFragment;
import com.example.fly.mvvm.core.bean.pojo.book.BookList;
import com.example.fly.mvvm.core.bean.pojo.common.TypeVo;
import com.example.fly.mvvm.core.bean.pojo.home.CatagoryVo;
import com.example.fly.mvvm.core.bean.pojo.home.HomeMergeVo;
import com.example.fly.mvvm.core.vm.HomeViewModel;
import com.trecyclerview.multitype.MultiTypeAdapter;

/**
 * 首页
 */
public class HomeFragment extends BaseListFragment<HomeViewModel> {

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void initView(final Bundle state) {
        super.initView(state);
        setTitle(getResources().getString(R.string.home_title_name));
    }

    @Override
    protected void dataObserver() {
        mViewModel.getMergeData().observe(this, new Observer<HomeMergeVo>() {
            @Override
            public void onChanged(@Nullable HomeMergeVo homeMergeVo) {

            }
        });
        mViewModel.getMergeData().observe(this, homeMergeVo -> {
            if (homeMergeVo != null) {
                addItems(homeMergeVo);
            }
        });
    }

    @Override
    protected void lazyLoad() {
        super.lazyLoad();
        getNetWorkData();
    }

    @Override
    protected void onStateRefresh() {
        super.onStateRefresh();
        getNetWorkData();
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        getNetWorkData();
    }

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        return new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
    }

    @Override
    protected MultiTypeAdapter createAdapter() {
        return AdapterPool.newInstance().getHomeAdapter(getActivity());
    }

    protected void getNetWorkData() {
        mViewModel.getRequestMerge();
    }

    /**
     * 列表展示数据
     * @param homeMergeVo
     */
    private void addItems(HomeMergeVo homeMergeVo) {
        newItems.add(homeMergeVo.bannerListVo);
        newItems.add(new CatagoryVo("title"));
        newItems.add(new TypeVo(getResources().getString(R.string.recommend_live_type)));
        if (homeMergeVo.homeListVo.data.live_recommend.size() > 0) {
            newItems.addAll(homeMergeVo.homeListVo.data.live_recommend);
        }
        newItems.add(new TypeVo(getResources().getString(R.string.recommend_video_type)));
        if (homeMergeVo.homeListVo.data.course.size() > 0) {
            newItems.addAll(homeMergeVo.homeListVo.data.course);
        }
        newItems.add(new TypeVo(getResources().getString(R.string.recommend_book_type)));
        if (homeMergeVo.homeListVo.data.publishingbook.size() > 0) {
            newItems.add(new BookList(homeMergeVo.homeListVo.data.publishingbook));
        }
        newItems.add(new TypeVo(getResources().getString(R.string.special_tab_name)));
        if (homeMergeVo.homeListVo.data.matreialsubject.size() > 0) {
            newItems.addAll(homeMergeVo.homeListVo.data.matreialsubject);
        }
        oldItems.clear();
        oldItems.addAll(newItems);
        mRecyclerView.refreshComplete(oldItems, true);
        newItems.clear();

    }

}