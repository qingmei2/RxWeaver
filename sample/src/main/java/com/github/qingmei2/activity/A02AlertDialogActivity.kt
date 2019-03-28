package com.github.qingmei2.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.UserInfo
import com.github.qingmei2.processor.GlobalErrorProcessor
import com.github.qingmei2.utils.appendLine
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_02_alert_dialog.*
import qingmei2.github.qingmei2.R
import java.net.ConnectException

/**
 * 比较复杂的异步处理，比如弹出一个dialog，用户操作决定流的下一步走向
 */
class A02AlertDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_03_token_expired)

        mFabRequest.setOnClickListener { queryUserInfo() }
    }

    @SuppressLint("CheckResult")
    private fun queryUserInfo() {
        Observable.error<BaseEntity<UserInfo>>(ConnectException())
                .compose(GlobalErrorProcessor.processGlobalError<BaseEntity<UserInfo>>(this)) // 弹出dialog
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("接口请求失败，异常信息：$error")
                })
    }
}