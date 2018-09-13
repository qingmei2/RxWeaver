package com.github.qingmei2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import qingmei2.github.qingmei2.R
import java.net.ConnectException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSimpleError.setOnClickListener {
            fetchSimpleError()
        }
    }

    private fun fetchSimpleError() {
        Observable.error(ConnectException())
    }
}