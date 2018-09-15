package com.github.qingmei2.kotlin.retry

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit

class ObservableRetryDelay(
        val provider: (Throwable) -> RetryConfig
) : Function<Observable<Throwable>, ObservableSource<*>> {

    private var retryCount: Int = 0

    @Throws(Exception::class)
    override fun apply(@NonNull throwableObservable: Observable<Throwable>): ObservableSource<*> {
        return throwableObservable
                .flatMap(Function<Throwable, ObservableSource<*>> { throwable ->
                    val (maxRetries, delay, retryCondition) = provider(throwable)

                    if (!retryCondition)
                        return@Function Observable.error<Any>(throwable)

                    if (++retryCount < maxRetries) {
                        Observable.timer(delay.toLong(), TimeUnit.MILLISECONDS)
                    } else Observable.error<Any>(throwable)
                })
    }
}
