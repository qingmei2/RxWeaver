package com.github.qingmei2;

import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.github.qingmei2.core.GlobalErrorTransformer;
import com.github.qingmei2.entity.BaseEntity;
import com.github.qingmei2.exceptions.ConnectFailedAlertDialogException;
import com.github.qingmei2.exceptions.TokenExpiredException;
import com.github.qingmei2.func.Suppiler;
import com.github.qingmei2.model.NavigatorFragment;
import com.github.qingmei2.model.RxDialog;
import com.github.qingmei2.retry.RetryConfig;

import org.json.JSONException;

import java.net.ConnectException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class RxUtils {

    private static final int STATUS_UNAUTHORIZED = 401;

    public static <T extends BaseEntity> GlobalErrorTransformer<T> handleGlobalError(FragmentActivity activity) {

        return new GlobalErrorTransformer<T>(

                // 通过onNext流中数据的状态进行操作
                new Function<T, Observable<T>>() {
                    @Override
                    public Observable<T> apply(T it) throws Exception {
                        switch (it.getStatusCode()) {
                            case STATUS_UNAUTHORIZED:
                                return Observable.error(new TokenExpiredException());
                        }
                        return Observable.just(it);
                    }
                },

                // 通过onError中Throwable状态进行操作
                new Function<Throwable, Observable<T>>() {
                    @Override
                    public Observable<T> apply(Throwable error) throws Exception {
                        if (error instanceof ConnectException) {
                            return Observable.error(new ConnectFailedAlertDialogException());
                        }
                        return Observable.error(error);
                    }
                },

                new Function<Throwable, RetryConfig>() {
                    @Override
                    public RetryConfig apply(Throwable error) throws Exception {

                        if (error instanceof ConnectFailedAlertDialogException) {
                            return new RetryConfig(
                                    new Suppiler<Single<Boolean>>() {
                                        @Override
                                        public Single<Boolean> call() {
                                            return RxDialog.showErrorDialog(activity, "ConnectException")
                                                    .flatMap(new Function<Boolean, SingleSource<? extends Boolean>>() {
                                                        @Override
                                                        public SingleSource<? extends Boolean> apply(Boolean retry) throws Exception {
                                                            return Single.just(retry);
                                                        }
                                                    });
                                        }
                                    });
                        }

                        if (error instanceof TokenExpiredException) {
                            return new RetryConfig(1, 3000,     // 最多重试1次，延迟3000ms
                                    new Suppiler<Single<Boolean>>() {
                                        @Override
                                        public Single<Boolean> call() {
                                            Toast.makeText(activity, "Token失效，跳转到Login重新登录！", Toast.LENGTH_SHORT).show();
                                            return new NavigatorFragment()
                                                    .startLoginForResult(activity)
                                                    .flatMap(new Function<Boolean, SingleSource<Boolean>>() {
                                                        @Override
                                                        public SingleSource<Boolean> apply(Boolean retry) throws Exception {
                                                            return Single.just(retry);
                                                        }
                                                    });
                                        }
                                    }
                            );
                        }

                        return new RetryConfig();   // 其它异常都不重试
                    }
                },

                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof JSONException) {
                            Toast.makeText(activity, "全局异常捕获-Json解析异常！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}