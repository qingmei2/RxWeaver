package com.github.qingmei2.utils

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RxDialog {

    /**
     * 简单的示例，弹出一个dialog提示用户，将用户的操作转换为一个流并返回
     */
    @JvmStatic
    fun showErrorDialog(
            context: Context,
            message: String
    ): Single<Boolean> {
        return Single
                .create<Boolean> { emitter ->
                    AlertDialog.Builder(context)
                            .setTitle("错误")
                            .setMessage("您收到了一个异常:$message,是否重试本次请求？")
                            .setCancelable(false)
                            .setPositiveButton("重试") { _, _ ->
                                Toast.makeText(context, "重新请求", Toast.LENGTH_SHORT).show()
                                emitter.onSuccess(true)
                            }
                            .setNegativeButton("取消") { _, _ ->
                                Toast.makeText(context, "用户点击了取消", Toast.LENGTH_SHORT).show()
                                emitter.onSuccess(false)
                            }
                            .show()
                }
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
    }

}