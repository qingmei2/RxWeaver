package com.github.qingmei2.core

import com.github.qingmei2.retry.FlowableRetryDelay
import com.github.qingmei2.retry.ObservableRetryDelay
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers

class WeaverTransformer<T> constructor(
        private val upStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() },
        private val downStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() },
        private val globalOnNextInterceptor: (T) -> Single<ThrowableDelegate>,
        private val globalOnErrorResumeTransformer: (Throwable) -> Single<ThrowableDelegate>,
        private val retryConfigProvider: () -> RetryConfig,
        private val globalDoOnErrorConsumer: (Throwable) -> Unit
) : ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    override fun apply(upstream: Observable<T>): Observable<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .flatMapObservable { rxerror ->
                                    if (rxerror !== ThrowableDelegate.EMPTY) Observable.error(rxerror) else Observable.just(it)
                                }
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResumeTransformer(throwable)
                                .flatMapObservable { rxerror ->
                                    Observable.error<T> {
                                        if (rxerror !== ThrowableDelegate.EMPTY) rxerror else throwable
                                    }
                                }
                    }
                    .retryWhen(ObservableRetryDelay(retryConfigProvider()))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Completable): Completable =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .onErrorResumeNext {
                        globalOnErrorResumeTransformer(it)
                                .flatMapCompletable { rxerror -> Completable.error(if (rxerror !== ThrowableDelegate.EMPTY) rxerror else it) }
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider()))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Flowable<T>): Flowable<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .flatMapPublisher { rxerror ->
                                    if (rxerror !== ThrowableDelegate.EMPTY) Flowable.error(rxerror) else Flowable.just(it)
                                }
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResumeTransformer(throwable)
                                .flatMapPublisher { rxerror ->
                                    Flowable.error<T> {
                                        if (rxerror !== ThrowableDelegate.EMPTY) rxerror else throwable
                                    }
                                }
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider()))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Maybe<T>): Maybe<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .flatMapMaybe { rxerror ->
                                    if (rxerror !== ThrowableDelegate.EMPTY) Maybe.error(rxerror) else Maybe.just(it)
                                }
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResumeTransformer(throwable)
                                .flatMapMaybe { rxerror ->
                                    Maybe.error<T> {
                                        if (rxerror !== ThrowableDelegate.EMPTY) rxerror else throwable
                                    }
                                }
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider()))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Single<T>): Single<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .flatMap { rxerror ->
                                    if (rxerror !== ThrowableDelegate.EMPTY) Single.error(rxerror) else Single.just(it)
                                }
                    }
                    .onErrorResumeNext {
                        globalOnErrorResumeTransformer(it)
                                .flatMap { rxerror -> Single.error<T>(if (rxerror !== ThrowableDelegate.EMPTY) rxerror else it) }
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider()))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())
}
