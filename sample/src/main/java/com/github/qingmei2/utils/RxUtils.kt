@file:Suppress("UNREACHABLE_CODE")

package com.github.qingmei2.utils

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.core.GlobalErrorTransformer
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.ConnectFailedAlertDialogException
import com.github.qingmei2.entity.TokenExpiredException
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.Observable
import org.json.JSONException
import java.net.ConnectException

object RxUtils {

    var hasRefreshToken = false

    /**
     * Status code
     */
    private const val STATUS_OK = 200
    private const val STATUS_UNAUTHORIZED = 401
    private const val FORBIDDEN = 403
    private const val NOT_FOUND = 404
    private const val REQUEST_TIMEOUT = 408
    private const val INTERNAL_SERVER_ERROR = 500
    private const val BAD_GATEWAY = 502
    private const val SERVICE_UNAVAILABLE = 503
    private const val GATEWAY_TIMEOUT = 504

    fun <T : BaseEntity<*>> handleGlobalError(activity: FragmentActivity): GlobalErrorTransformer<T> = GlobalErrorTransformer(

            // 通过onNext流中数据的状态进行操作
            onNextInterceptor = {
                when (it.statusCode) {
                    STATUS_UNAUTHORIZED -> Observable.error(TokenExpiredException)
                    else -> Observable.just(it)
                }
            },

            // 通过onError中Throwable状态进行操作
            onErrorResumeNext = { error ->
                when (error) {
                    is ConnectException ->
                        Observable.error<T>(ConnectFailedAlertDialogException)
                    is TokenExpiredException ->
                        Observable.error<T>(TokenExpiredProcessResult.WaitLoginInQueue)
                    else -> Observable.error<T>(error)
                }
            },

            onErrorRetrySupplier = { error ->
                when (error) {
                    is ConnectFailedAlertDialogException ->
                        RetryConfig.simpleInstance {
                            RxDialog.showErrorDialog(activity, "ConnectException")
                        }
                    is TokenExpiredProcessResult.WaitLoginInQueue ->
                        RetryConfig.simpleInstance(delay = 1000) {
                            Observable.error<Boolean>(error)
                                    .compose(GlobalErrorProcessorHolder.tokenExpiredProcessor(activity))
                                    .firstOrError()
                        }
                    else -> RetryConfig.none()      // 其它异常都不重试
                }
            },

            onErrorConsumer = { error ->
                when (error) {
                    is JSONException -> Log.w("rx stream Exception", "Json解析异常:${error.message}")
                    else -> Log.w("rx stream Exception", "其它异常:${error.message}")
                }
            }
    )
}