package com.github.qingmei2.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.UserInfo
import com.github.qingmei2.utils.RxUtils
import com.github.qingmei2.utils.appendLine
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_01_simple_toast.*
import org.json.JSONException
import qingmei2.github.qingmei2.R

/**
 * 简单的异常全局处理，比如弹一个toast，JSONException
 */
class A01SimpleToastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_03_token_expired)

        mFabRequest.setOnClickListener { queryUserInfo() }
    }

    @SuppressLint("CheckResult")
    private fun queryUserInfo() {
        Observable.error<BaseEntity<UserInfo>>(JSONException("JSONException"))
                .compose(RxUtils.processGlobalError<BaseEntity<UserInfo>>(this)) // 仅仅是弹一个toast
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("接口请求失败，异常信息：$error")
                })
    }
}