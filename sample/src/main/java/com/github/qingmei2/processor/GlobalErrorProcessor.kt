@file:Suppress("UNREACHABLE_CODE")

package com.github.qingmei2.processor

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.core.GlobalErrorTransformer
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.Errors
import com.github.qingmei2.processor.tokens.AuthorizationErrorProcessResult
import com.github.qingmei2.processor.tokens.AuthorizationErrorProcessor
import com.github.qingmei2.retry.RetryConfig
import com.github.qingmei2.utils.RxDialog
import io.reactivex.Observable
import org.json.JSONException
import java.net.ConnectException

object GlobalErrorProcessor {

    /**
     * Status code
     */
    const val STATUS_OK = 200
    const val STATUS_UNAUTHORIZED = 401

    fun <T : BaseEntity<*>> processGlobalError(fragmentActivity: FragmentActivity): GlobalErrorTransformer<T> = GlobalErrorTransformer(

            // 通过onNext流中数据的状态进行操作
            onNextInterceptor = {
                when (it.statusCode) {
                    STATUS_UNAUTHORIZED -> Observable.error(
                            Errors.AuthorizationError(timeStamp = System.currentTimeMillis())
                    )
                    else -> Observable.just(it)
                }
            },

            // 通过onError中Throwable状态进行操作
            onErrorResumeNext = { error ->
                when (error) {
                    is ConnectException ->
                        Observable.error<T>(Errors.ConnectFailedException)
                    // 这个错误会在onErrorRetrySupplier()中处理
                    is Errors.AuthorizationError -> Observable.error<T>(error)
                    else -> Observable.error<T>(error)
                }
            },

            onErrorRetrySupplier = { retrySupplierError ->
                when (retrySupplierError) {
                    // 网络连接异常，弹出dialog，并根据用户选择结果进行错误重试处理
                    Errors.ConnectFailedException ->
                        RetryConfig.simpleInstance {
                            RxDialog.showErrorDialog(fragmentActivity, "ConnectException")
                        }
                    // 用户认证失败，弹出login界面
                    is Errors.AuthorizationError ->
                        RetryConfig.simpleInstance {
                            val waitLogin = AuthorizationErrorProcessResult.WaitLoginInQueue(
                                    lastRefreshStamp = retrySupplierError.timeStamp
                            )
                            AuthorizationErrorProcessor.processTokenExpiredError(fragmentActivity, waitLogin)
                                    .onErrorReturn { processorError ->
                                        when (processorError) {
                                            is AuthorizationErrorProcessResult.LoginSuccess -> true
                                            is AuthorizationErrorProcessResult.LoginFailed,
                                            is AuthorizationErrorProcessResult.WaitLoginInQueue -> false
                                            else -> false
                                        }
                                    }
                                    .firstOrError()
                        }
                    else -> RetryConfig.none()      // 其它异常都不重试
                }
            },

            onErrorConsumer = { error ->
                when (error) {
                    is JSONException -> {
                        Toast.makeText(fragmentActivity, "$error", Toast.LENGTH_SHORT).show()
                        Log.w("rx stream Exception", "Json解析异常:${error.message}")
                    }
                    else -> Log.w("rx stream Exception", "其它异常:${error.message}")
                }
            }
    )
}