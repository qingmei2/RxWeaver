package com.github.qingmei2.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.utils.RxUtils
import com.github.qingmei2.entity.UserInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import qingmei2.github.qingmei2.R
import java.net.ConnectException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSimpleError.setOnClickListener {
            fetchError(obsToastError)
        }
        btnConnectError.setOnClickListener {
            fetchError(obsConnectError)
        }
        btnTokenError.setOnClickListener {
            fetchError(obsTokenError)
        }
        mBtnMultiError.setOnClickListener {
            startActivity(Intent(this, MultiAsyncActivity::class.java))
        }
    }

    /**
     * 简单的异常全局处理，比如弹一个toast，JSONException
     */
    private val obsToastError: Observable<BaseEntity<UserInfo>> =
            Observable.error(JSONException("JSONException"))

    /**
     * 复杂的异步处理，比如弹出一个dialog，用户操作决定流的下一步走向
     */
    private val obsConnectError: Observable<BaseEntity<UserInfo>> =
            Observable.error(ConnectException())

    /**
     * 十分复杂的处理，比如token失效，用户跳转login界面，重新登录成功后，继续重新请求
     */
    private val obsTokenError =
            Observable.create<BaseEntity<UserInfo>> { emitter ->
                val entity = BaseEntity(
                        statusCode = 401,
                        message = "unauthorized",
                        data = null
                )
                emitter.onNext(entity)
            }

    private fun fetchError(observable: Observable<BaseEntity<UserInfo>>) =
            observable
                    .compose(RxUtils.handleGlobalError<BaseEntity<UserInfo>>(this))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onNext = {
                                Toast.makeText(this, "onNext: $it", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(this, "onError: $it", Toast.LENGTH_SHORT).show()
                                it.printStackTrace()
                            }
                    )

}