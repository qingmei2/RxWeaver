package com.github.qingmei2.retry

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit

class ObservableRetryDelay(
        val retryConfigProvider: (Throwable) -> RetryConfig
) : Function<Observable<Throwable>, ObservableSource<*>> {

    private var retryCount: Int = 0

    override fun apply(throwableObs: Observable<Throwable>): ObservableSource<*> {
        return throwableObs
                .flatMap { error ->
                    val (maxRetries, delay, retryCondition) = retryConfigProvider(error)

                    if (++retryCount <= maxRetries) {
                        retryCondition()
                                .flatMapObservable { retry ->
                                    if (retry)
                                        Observable.timer(delay.toLong(), TimeUnit.MILLISECONDS)
                                    else
                                        Observable.error<Any>(error)
                                }
                    } else Observable.error<Any>(error)
                }
    }
}