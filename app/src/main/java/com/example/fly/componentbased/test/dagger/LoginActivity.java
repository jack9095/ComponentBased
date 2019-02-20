package com.example.fly.componentbased.test.dagger;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.example.fly.componentbased.R;
import com.example.fly.componentbased.bean.User;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 有没有注意到loginPresenter并没有初始化？
 * 这里一个重点就是loginPresenter和LoinActivity 解藕了，
 * 后续无论怎样修改loginPresenter的构造方法都不需要改动LoinActivity的代码。
 * 那要改谁的代码呢？当然是CommonModudle了。
 *
 * https://www.jianshu.com/p/626b2087e2b1
 *
 * https://www.jianshu.com/p/39d1df6c877d  很详细，解释的
 *
 */
public class LoginActivity extends AppCompatActivity implements ICommonView {

    @BindView(R.id.btn_login)
    Button btn;

    @Inject // 使用@Inject时，不能用private修饰符修饰类的成员属性
    LoginPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        DaggerCommonComponent.
                builder().
                commonModule(new CommonModule(this)).
                build().
                inject(this);
    }

    @OnClick(R.id.btn_login)
    public void login() {
        presenter.login(new User());
    }

    @Override
    public Context getContext() {
        return this;
    }
}