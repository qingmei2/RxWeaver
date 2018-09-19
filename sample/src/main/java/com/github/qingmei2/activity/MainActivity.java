package com.github.qingmei2.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.github.qingmei2.RxUtils;
import com.github.qingmei2.entity.UserInfo;

import org.json.JSONException;

import java.net.ConnectException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import qingmei2.github.qingmei2.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnSimpleError).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchError(obsToastError);
            }
        });

        findViewById(R.id.btnConnectError).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchError(obsConnectError);
            }
        });

        findViewById(R.id.btnTokenError).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchError(obsTokenError);
            }
        });
    }

    // 简单的异常全局处理，比如弹一个toast，JSONException
    private Observable<UserInfo> obsToastError = Observable.error(new JSONException("JSONException"));

    // 复杂的异步处理，比如弹出一个dialog，用户操作决定流的下一步走向
    private Observable<UserInfo> obsConnectError = Observable.error(new ConnectException());

    // 十分复杂的处理，比如token失效，用户跳转login界面，重新登录成功后，继续重新请求
    private Observable<UserInfo> obsTokenError = Observable.just(
            new UserInfo(401, "unauthorized", "XiagMing", 18)
    );

    private void fetchError(Observable<UserInfo> observable) {
        observable
                .compose(RxUtils.handleGlobalError(this))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UserInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(UserInfo userInfo) {
                        Toast.makeText(MainActivity.this, "onNext:" + userInfo.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "onError: " + e, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
