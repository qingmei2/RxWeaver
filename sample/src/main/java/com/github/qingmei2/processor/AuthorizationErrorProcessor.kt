package com.github.qingmei2.processor

import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.activity.login.NavigatorFragment
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object AuthorizationErrorProcessor {

    var mLastRefreshTokenTimeStamp: Long = 0L
    var mLastCancelRefreshTokenTimeStamp: Long = 0L
    var mIsBlocking: Boolean = false

    fun processTokenExpiredError(
            currentActivity: FragmentActivity,
            currentWaitLoginInQueue: AuthorizationErrorProcessResult.WaitLoginInQueue
    ): Observable<Boolean> {
        val lastRefreshStamp = currentWaitLoginInQueue.lastRefreshStamp
        return Observable.defer {
            synchronized(AuthorizationErrorProcessor::class.java) {
                return@synchronized when (mIsBlocking) {
                    true -> {
                        Observable.error<Boolean>(currentWaitLoginInQueue)
                    }
                    false -> {
                        mIsBlocking = true
                        when (mLastRefreshTokenTimeStamp > lastRefreshStamp) {
                            true -> {
                                processLoginSuccessMessage(lastRefreshStamp)
                            }
                            false -> {
                                processLoginMessage(currentActivity, lastRefreshStamp)
                            }
                        }
                    }
                }
            }
        }.retryWhen(this::loopWaitLoginError)
    }

    private fun processLoginSuccessMessage(
            lastRefreshStamp: Long
    ): Observable<Boolean> {
        return Observable
                .error<Boolean>(AuthorizationErrorProcessResult.LoginSuccess(lastRefreshStamp))
                .doOnError { updateIsBlockingState(false) }
    }

    private fun processLoginFailureMessage(
            lastRefreshStamp: Long
    ): Observable<Boolean> {
        return Observable
                .error<Boolean>(AuthorizationErrorProcessResult.LoginFailed(lastRefreshStamp))
                .doOnError {
                    updateIsBlockingState(false)
                }
    }

    private fun processLoginMessage(
            currentActivity: FragmentActivity,
            lastRefreshStamp: Long
    ): Observable<Boolean> {
        return when (mLastCancelRefreshTokenTimeStamp > lastRefreshStamp) {
            true -> {
                processLoginFailureMessage(lastRefreshStamp)
            }
            false -> {
                NavigatorFragment
                        .startLoginForResult(currentActivity)
                        .doOnSuccess {
                            when (it) {
                                true -> {
                                    updateLoginSuccessTimeStamp()
                                }
                                false -> {
                                    updateLoginFailureTimeStamp()
                                }
                            }
                        }
                        .observeOn(Schedulers.io())
                        .toObservable()
                        .flatMap { loginResult ->
                            when (loginResult) {
                                true -> {
                                    Observable.error<Boolean>(
                                            AuthorizationErrorProcessResult.LoginSuccess(lastRefreshStamp)
                                    )
                                }
                                false -> Observable.error<Boolean>(
                                        AuthorizationErrorProcessResult.LoginFailed(lastRefreshStamp)
                                )
                            }
                        }
                        .doOnError {
                            updateIsBlockingState(false)
                        }
            }
        }
    }

    private fun loopWaitLoginError(obs: Observable<out Throwable>): Observable<in Any> {
        return obs.flatMap { processorError ->
            when (processorError) {
                is AuthorizationErrorProcessResult.WaitLoginInQueue ->
                    Observable.timer(50, TimeUnit.MILLISECONDS)
                else -> Observable.error(processorError)
            }
        }
    }

    @Synchronized
    private fun updateIsBlockingState(value: Boolean) {
        mIsBlocking = value
    }

    @Synchronized
    private fun updateLoginSuccessTimeStamp() {
        mLastRefreshTokenTimeStamp = System.currentTimeMillis()
    }

    @Synchronized
    private fun updateLoginFailureTimeStamp() {
        mLastCancelRefreshTokenTimeStamp = System.currentTimeMillis()
    }
}

sealed class AuthorizationErrorProcessResult : Exception() {

    data class LoginSuccess(val lastRefreshStamp: Long) : AuthorizationErrorProcessResult()

    data class LoginFailed(val lastRefreshStamp: Long) : AuthorizationErrorProcessResult()

    data class WaitLoginInQueue(val lastRefreshStamp: Long) : AuthorizationErrorProcessResult()
}