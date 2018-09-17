package com.github.qingmei2.core

import com.github.qingmei2.retry.FlowableRetryDelay
import com.github.qingmei2.retry.ObservableRetryDelay
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers

class GlobalErrorTransformer<T> constructor(
        private val globalOnNextRetryInterceptor: (T) -> Single<RxThrowable> = { Single.just(RxThrowable.EMPTY) },
        private val globalOnErrorResumeRetryTransformer: (Throwable) -> Single<RxThrowable> = { Single.just(RxThrowable.EMPTY) },
        private val retryConfigProvider: (Throwable) -> RetryConfig = { RetryConfig() },
        private val globalDoOnNextConsumer: (T) -> Unit = { },
        private val globalDoOnErrorConsumer: (Throwable) -> Unit = { },
        private val globalDoOnCompleteComsumer: () -> Unit = { },
        private val upStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() },
        private val downStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() }
) : ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    override fun apply(upstream: Observable<T>): Observable<T> =
            upstream
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .flatMapObservable { rxerror ->
                                    if (rxerror !== RxThrowable.EMPTY) Observable.error(rxerror) else Observable.just(it)
                                }
                                .onErrorResumeNext { throwable: Throwable ->
                                    globalOnErrorResumeRetryTransformer(throwable)
                                            .flatMapObservable { rxerror ->
                                                Observable.error<T> {
                                                    if (rxerror !== RxThrowable.EMPTY) rxerror else throwable
                                                }
                                            }
                                }
                                .retryWhen(ObservableRetryDelay(retryConfigProvider))
                    }
                    .observeOn(upStreamSchedulerProvider())
                    .doOnNext(globalDoOnNextConsumer)
                    .doOnComplete(globalDoOnCompleteComsumer)
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Completable): Completable =
            upstream
                    .onErrorResumeNext {
                        globalOnErrorResumeRetryTransformer(it)
                                .flatMapCompletable { rxerror -> Completable.error(if (rxerror !== RxThrowable.EMPTY) rxerror else it) }
                                .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    }
                    .observeOn(upStreamSchedulerProvider())
                    .doOnComplete(globalDoOnCompleteComsumer)
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Flowable<T>): Flowable<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .flatMapPublisher { rxerror ->
                                    if (rxerror !== RxThrowable.EMPTY) Flowable.error(rxerror) else Flowable.just(it)
                                }
                                .onErrorResumeNext { throwable: Throwable ->
                                    globalOnErrorResumeRetryTransformer(throwable)
                                            .flatMapPublisher { rxerror ->
                                                Flowable.error<T> {
                                                    if (rxerror !== RxThrowable.EMPTY) rxerror else throwable
                                                }
                                            }
                                }
                                .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    }
                    .observeOn(upStreamSchedulerProvider())
                    .doOnNext(globalDoOnNextConsumer)
                    .doOnComplete(globalDoOnCompleteComsumer)
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Maybe<T>): Maybe<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .flatMapMaybe { rxerror ->
                                    if (rxerror !== RxThrowable.EMPTY) Maybe.error(rxerror) else Maybe.just(it)
                                }
                                .onErrorResumeNext { throwable: Throwable ->
                                    globalOnErrorResumeRetryTransformer(throwable)
                                            .flatMapMaybe { rxerror ->
                                                Maybe.error<T> {
                                                    if (rxerror !== RxThrowable.EMPTY) rxerror else throwable
                                                }
                                            }
                                }
                                .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    }
                    .observeOn(upStreamSchedulerProvider())
                    .doOnSuccess(globalDoOnNextConsumer)
                    .doOnComplete(globalDoOnCompleteComsumer)
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Single<T>): Single<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .flatMap { rxerror ->
                                    if (rxerror !== RxThrowable.EMPTY) Single.error(rxerror) else Single.just(it)
                                }
                                .onErrorResumeNext { throwable ->
                                    globalOnErrorResumeRetryTransformer(throwable)
                                            .flatMap { rxerror -> Single.error<T>(if (rxerror !== RxThrowable.EMPTY) rxerror else throwable) }
                                }
                                .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    }
                    .observeOn(upStreamSchedulerProvider())
                    .doOnSuccess(globalDoOnNextConsumer)
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())
}
