@file:Suppress("UNREACHABLE_CODE")

package com.github.qingmei2.utils

import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Toast
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.ConnectFailedAlertDialogException
import com.github.qingmei2.entity.TokenExpiredException
import com.github.qingmei2.core.GlobalErrorTransformer
import com.github.qingmei2.activity.widget.NavigatorFragment
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import java.net.ConnectException

object RxUtils {

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
                    STATUS_UNAUTHORIZED -> {
                        Observable.error(TokenExpiredException())
                    }
                    else -> Observable.just(it)
                }
            },

            // 通过onError中Throwable状态进行操作
            onErrorResumeNext = { error ->
                when (error) {
                    is ConnectException -> {
                        Observable.error<T>(ConnectFailedAlertDialogException())
                    }
                    else -> Observable.error<T>(error)
                }
            },

            onErrorRetrySupplier = { error ->
                when (error) {
                    is ConnectFailedAlertDialogException -> RetryConfig.simpleInstance {
                        RxDialog.showErrorDialog(activity, "ConnectException")
                    }
                    is TokenExpiredException -> RetryConfig.simpleInstance(delay = 3000) {
                        // token失效，重新启用Login界面模拟用户请求
                        NavigatorFragment
                                .startLoginForResult(activity)
                                .doOnSuccess { loginSuccess ->
                                    if (loginSuccess) {
                                        Toast.makeText(activity, "登陆成功,3s延迟后重试！", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(activity, "登陆失败,error继续向下游传递", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .observeOn(Schedulers.io()) // 下游的业务继续交给子线程处理
                    }
                    else -> RetryConfig.none()      // 其它异常都不重试
                }
            },

            onErrorConsumer = { error ->
                when (error) {
                    is JSONException -> {
                        Log.w("RxUtils", "全局异常捕获-Json解析异常！")
                    }
                    else -> {

                    }
                }
            }
    )
}
