package com.github.qingmei2.model;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import io.reactivex.Single;

public class RxDialog {

    public static Single<Boolean> showErrorDialog(Context context, String message) {
        return Single.create(emitter ->
                new AlertDialog.Builder(context)
                        .setTitle("错误")
                        .setMessage("您收到了一个异常:" + message + ",是否重试本次请求？")
                        .setCancelable(false)
                        .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                emitter.onSuccess(true);
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                emitter.onSuccess(false);
                            }
                        }).show()
        );
    }
}
