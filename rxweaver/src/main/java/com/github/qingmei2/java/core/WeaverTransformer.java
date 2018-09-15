package com.github.qingmei2.java.core;


import com.github.qingmei2.java.retry.FlowableRetryDelay;
import com.github.qingmei2.java.retry.ObservableRetryDelay;
import com.github.qingmei2.java.retry.RetryConfig;

import org.reactivestreams.Publisher;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class WeaverTransformer<T> implements ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    private Scheduler upStreamSchedulerProvider;
    private Scheduler downStreamSchedlerProvider;
    private Single<Throwable> globalOnNextInterceptor;
    private Single<Throwable> globalOnErrorResumeTransformer;
    private RetryConfig retryConfigProvider;
    private Consumer<Throwable> globalDoOnErrorConsumer;

    public WeaverTransformer(Single<Throwable> globalOnNextInterceptor,
                             Single<Throwable> globalOnErrorResumeTransformer,
                             RetryConfig retryConfigProvider,
                             Consumer<Throwable> globalDoOnErrorConsumer) {
        this(
                AndroidSchedulers.mainThread(),
                AndroidSchedulers.mainThread(),
                globalOnNextInterceptor,
                globalOnErrorResumeTransformer,
                retryConfigProvider,
                globalDoOnErrorConsumer
        );
    }

    public WeaverTransformer(Scheduler upStreamSchedulerProvider,
                             Scheduler downStreamSchedlerProvider,
                             Single<Throwable> globalOnNextInterceptor,
                             Single<Throwable> globalOnErrorResumeTransformer,
                             RetryConfig retryConfigProvider,
                             Consumer<Throwable> globalDoOnErrorConsumer) {
        this.upStreamSchedulerProvider = upStreamSchedulerProvider;
        this.downStreamSchedlerProvider = downStreamSchedlerProvider;
        this.globalOnNextInterceptor = globalOnNextInterceptor;
        this.globalOnErrorResumeTransformer = globalOnErrorResumeTransformer;
        this.retryConfigProvider = retryConfigProvider;
        this.globalDoOnErrorConsumer = globalDoOnErrorConsumer;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider)
                .flatMap(new Function<T, ObservableSource<T>>() {
                    @Override
                    public ObservableSource<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor
                                .flatMapObservable(new Function<Throwable, ObservableSource<? extends T>>() {
                                    @Override
                                    public ObservableSource<? extends T> apply(Throwable throwable) throws Exception {
                                        return (throwable != RxThrowable.EMPTY) ? Observable.<T>error(throwable) : Observable.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends T>>() {
                    @Override
                    public ObservableSource<? extends T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer
                                .flatMapObservable(new Function<Throwable, ObservableSource<? extends T>>() {
                                    @Override
                                    public ObservableSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Observable.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new ObservableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider)
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer
                                .flatMapCompletable(new Function<Throwable, CompletableSource>() {
                                    @Override
                                    public CompletableSource apply(Throwable rxerror) throws Exception {
                                        return Completable.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider)
                .flatMap(new Function<T, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor
                                .flatMapPublisher(new Function<Throwable, Publisher<? extends T>>() {
                                    @Override
                                    public Publisher<? extends T> apply(Throwable throwable) throws Exception {
                                        if (throwable != RxThrowable.EMPTY) {
                                            return Flowable.error(throwable);
                                        }
                                        return Flowable.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer
                                .flatMapPublisher(new Function<Throwable, Publisher<T>>() {
                                    @Override
                                    public Publisher<T> apply(final Throwable throwable) throws Exception {
                                        return globalOnErrorResumeTransformer
                                                .flatMapPublisher(new Function<Throwable, Publisher<T>>() {
                                                    @Override
                                                    public Publisher<T> apply(Throwable rxerror) throws Exception {
                                                        return Flowable.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                                    }
                                                });
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider)
                .flatMap(new Function<T, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor
                                .flatMapMaybe(new Function<Throwable, MaybeSource<? extends T>>() {
                                    @Override
                                    public MaybeSource<T> apply(Throwable throwable) throws Exception {
                                        if (throwable != RxThrowable.EMPTY) {
                                            return Maybe.error(throwable);
                                        }
                                        return Maybe.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer
                                .flatMapMaybe(new Function<Throwable, MaybeSource<? extends T>>() {
                                    @Override
                                    public MaybeSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Maybe.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider)
                .flatMap(new Function<T, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor
                                .flatMap(new Function<Throwable, SingleSource<? extends T>>() {
                                    @Override
                                    public SingleSource<? extends T> apply(Throwable throwable) throws Exception {
                                        return (throwable != RxThrowable.EMPTY) ? Single.<T>error(throwable) : Single.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer
                                .flatMap(new Function<Throwable, SingleSource<? extends T>>() {
                                    @Override
                                    public SingleSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Single.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider);
    }
}
