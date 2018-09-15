package com.github.qingmei2.java.retry;


import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class ObservableRetryDelay implements Function<Observable<Throwable>, ObservableSource<?>> {

    private RetryConfig retryConfig;
    private int retryCount;

    public ObservableRetryDelay(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    @Override
    public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
        return throwableObservable
                .flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        if (retryConfig.isRetryCondition()) {
                            return Observable.error(throwable);
                        }
                        if (++retryCount <= retryConfig.getMaxRetries()) {
                            return Observable.timer(retryConfig.getDelay(), TimeUnit.MILLISECONDS);
                        }
                        return Observable.error(throwable);
                    }
                });
    }
}
