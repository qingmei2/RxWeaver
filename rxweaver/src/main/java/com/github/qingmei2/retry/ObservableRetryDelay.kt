package com.github.qingmei2.retry

import com.github.qingmei2.core.RxThrowable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit

class ObservableRetryDelay(
        val retryConfigProvider: (RxThrowable) -> RetryConfig
) : Function<Observable<Throwable>, ObservableSource<*>> {

    private var retryCount: Int = 0

    @Throws(Exception::class)
    override fun apply(@NonNull throwableObs: Observable<Throwable>): ObservableSource<*> {
        return throwableObs
                .flatMap(Function<Throwable, ObservableSource<*>> { error ->
                    if (error !is RxThrowable)
                        return@Function Observable.error<Any>(error)

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
                })
    }
}