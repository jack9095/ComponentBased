package com.base.live;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.base.live.widget.BottomNavigationView;

public class TestActivity extends AppCompatActivity {

    BottomNavigationView mBottomNavigationView;
    BlankFragment mBlankFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);
        mBottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mBlankFragment = new BlankFragment();
    }


}
