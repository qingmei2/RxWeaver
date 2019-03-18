package com.github.qingmei2.utils

import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.activity.widget.NavigatorFragment
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object GlobalErrorProcessorHolder {

    var isLoginBlocking: Boolean = false

    fun tokenExpiredProcessor(
            currentActivity: FragmentActivity,
            currentWaitLoginInQueue: TokenExpiredProcessResult.WaitLoginInQueue
    ): Observable<Boolean> = Observable
            .defer {
                synchronized(GlobalErrorProcessorHolder::class.java) {
                    when (isLoginBlocking) {
                        true -> {
                            return@synchronized Observable.error<Boolean>(currentWaitLoginInQueue)
                        }
                        false -> {
                            isLoginBlocking = true
                            return@synchronized NavigatorFragment
                                    .startLoginForResult(currentActivity)
                                    .doOnSuccess { isLoginBlocking = false }
                                    .observeOn(Schedulers.io())
                                    .toObservable()
                                    .flatMap { loginResult ->
                                        when (loginResult) {
                                            true -> Observable.error<Boolean>(
                                                    TokenExpiredProcessResult.LoginSuccess(123)
                                            )
                                            false -> Observable.error<Boolean>(
                                                    TokenExpiredProcessResult.LoginFailed(123)
                                            )
                                        }
                                    }
                        }
                    }
                }
            }
}

sealed class TokenExpiredProcessResult : Exception() {

    data class LoginSuccess(val lastRefreshStamp: Long) : TokenExpiredProcessResult()

    data class LoginFailed(val lastRefreshStamp: Long) : TokenExpiredProcessResult()

    data class WaitLoginInQueue(val lastRefreshStamp: Long) : TokenExpiredProcessResult()
}