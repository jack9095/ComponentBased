package com.example.fly.componentbased;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.base.commonlib.constant.StateConstants;
import com.bottomnavigation.BottomNavigationBar;
import com.bottomnavigation.BottomNavigationItem;
import com.example.fly.componentbased.fragment.HomeFragment;
import com.example.fly.componentbased.fragment.MineFragment;
import com.example.fly.componentbased.fragment.VideoFragment;
import com.example.fly.componentbased.fragment.WorkFragment;
import com.example.fly.componentbased.lifecycle.MyObserver;

/**
 * https://www.jianshu.com/u/ea71bb3770b4  // 路由分析的文章
 *
 * https://www.cnblogs.com/permanent2012moira/p/5126276.html  混淆配置
 *
 */
@Route(path = "/test/main")
public class MainActivity extends AppCompatActivity {

    private HomeFragment mHomeFragment;

    private WorkFragment mWorkFragment;

    private VideoFragment mVideoFragment;

    private MineFragment mMineFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO 必须在onCreate方法中调用addObserver方法
        Lifecycle lifecycle = getLifecycle();
        getLifecycle().addObserver(new MyObserver());
        initNavBar();
        initFragment(0);
    }

    private void initNavBar() {
        BottomNavigationBar mBottomNavigationBar = findViewById(R.id.bottom_navigation_bar);
        mBottomNavigationBar.setMode(BottomNavigationBar.MODE_FIXED);
        mBottomNavigationBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC);
        mBottomNavigationBar
                .addItem(new BottomNavigationItem(R.mipmap.tab_home_icon, R.string.home_title_name).setInactiveIconResource(R.drawable.live_right))
                .addItem(new BottomNavigationItem(R.mipmap.tab_works_icon, R.string.work_title_name).setInactiveIconResource(R.mipmap.tab_works_icon_def))
                .addItem(new BottomNavigationItem(R.mipmap.tab_course_icon, R.string.video_title_name).setInactiveIconResource(R.mipmap.tab_course_icon_def))
                .addItem(new BottomNavigationItem(R.mipmap.tab_mine_icon, R.string.mine_title_name).setInactiveIconResource(R.mipmap.tab_mine_icon_def))
                .setFirstSelectedPosition(0)
                .initialise();
        mBottomNavigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                initFragment(position);
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });
    }

    private void initFragment(int i) {
        FragmentManager mFragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        hideFragment(fragmentTransaction);
        switch (i) {
            case 0:
                if (mHomeFragment == null) {
                    mHomeFragment = HomeFragment.newInstance();
                    fragmentTransaction.add(R.id.home_content, mHomeFragment, StateConstants.HOME_TAG);
                } else {
                    fragmentTransaction.show(mHomeFragment);
                }
                break;

            case 1:
                if (mWorkFragment == null) {
                    mWorkFragment = new WorkFragment();
                    fragmentTransaction.add(R.id.home_content, mWorkFragment, StateConstants.WORK_TAG);
                } else {
                    fragmentTransaction.show(mWorkFragment);
                }
                break;
            case 2:
                if (mVideoFragment == null) {
                    mVideoFragment = new VideoFragment();
                    fragmentTransaction.add(R.id.home_content, mVideoFragment, StateConstants.VIDEO_TAG);
                } else {
                    fragmentTransaction.show(mVideoFragment);
                }
                break;
            case 3:
                if (mMineFragment == null) {
                    mMineFragment = new MineFragment();
                    fragmentTransaction.add(R.id.home_content, mMineFragment, StateConstants.MINE_TAG);
                } else {
                    fragmentTransaction.show(mMineFragment);
                }
                break;
            default:
                break;
        }
        fragmentTransaction.commit();
    }

    private void hideFragment(FragmentTransaction fragmentTransaction) {
        if (mHomeFragment != null) {
            fragmentTransaction.hide(mHomeFragment);
        }

        if (mWorkFragment != null) {
            fragmentTransaction.hide(mWorkFragment);
        }
        if (mVideoFragment != null) {
            fragmentTransaction.hide(mVideoFragment);
        }

        if (mMineFragment != null) {
            fragmentTransaction.hide(mMineFragment);
        }
    }

    /**
     * Home键切换到后台，调用此方法，但是比如打电话进来切换到后台是不会调用这个方法的
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
    }

    // activity 关联到 Context
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }
}