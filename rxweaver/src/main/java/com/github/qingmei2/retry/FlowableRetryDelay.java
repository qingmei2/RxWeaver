package com.github.qingmei2.retry;

import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

public class FlowableRetryDelay implements Function<Flowable<Throwable>, Publisher<?>> {

    private Function<Throwable, RetryConfig> provider;
    private int retryCount;

    public FlowableRetryDelay(Function<Throwable, RetryConfig> provider) {
        this.provider = provider;
    }

    @Override
    public Publisher<?> apply(@NonNull Flowable<Throwable> throwableFlowable) throws Exception {
        return throwableFlowable
                .flatMap(new Function<Throwable, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(@NonNull Throwable throwable) throws Exception {
                        RetryConfig retryConfig = provider.apply(throwable);

                        if (retryConfig.isRetryCondition()) {
                            return Flowable.error(throwable);
                        }
                        if (++retryCount < retryConfig.getMaxRetries()) {
                            return Flowable.timer(retryConfig.getDelay(), TimeUnit.MILLISECONDS);
                        }
                        return Flowable.error(throwable);
                    }
                });
    }
}
