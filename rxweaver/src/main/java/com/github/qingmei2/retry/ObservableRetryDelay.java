package com.github.qingmei2.retry;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class ObservableRetryDelay implements Function<Observable<Throwable>, ObservableSource<?>> {

    private Function<Throwable, RetryConfig> provider;
    private int retryCount;

    public ObservableRetryDelay(Function<Throwable, RetryConfig> provider) {
        this.provider = provider;
    }

    @Override
    public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
        return throwableObservable
                .flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        RetryConfig retryConfig = provider.apply(throwable);

                        if (retryConfig.isRetryCondition()) {
                            return Observable.error(throwable);
                        }
                        if (++retryCount < retryConfig.getMaxRetries()) {
                            return Observable.timer(retryConfig.getDelay(), TimeUnit.MILLISECONDS);
                        }
                        return Observable.error(throwable);
                    }
                });
    }
}
