package com.base.live;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.base.live.widget.BottomNavigationView;

@Route(path = "/live/main")
public class LiveMainActivity extends AppCompatActivity {

    BottomNavigationView mBottomNavigationView;
    BlankFragment mBlankFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);
        mBottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mBlankFragment = new BlankFragment();

        mBottomNavigationView.initView(this,R.id.main_frame,mBlankFragment,mBlankFragment,mBlankFragment,mBlankFragment);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("index",mBottomNavigationView.getIndex());
//        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int savedIndex = savedInstanceState.getInt("index");
        if (savedIndex != mBottomNavigationView.getIndex()) {
            if (mBottomNavigationView.getFragments()[0].isAdded()) {
                getSupportFragmentManager().beginTransaction().hide(mBottomNavigationView.getFragments()[0]).commit();
            }
            if (savedIndex == 1) {
                mBottomNavigationView.disPlay(1);
            } else if (savedIndex == 2){
                mBottomNavigationView.disPlay(2);
            }else if (savedIndex == 3){
                mBottomNavigationView.disPlay(3);
            }
        }
    }


}
