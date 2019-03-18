package com.github.qingmei2.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import qingmei2.github.qingmei2.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBtnSimpleError.setOnClickListener {
            startActivity(Intent(this, A01SimpleToastActivity::class.java))
        }
        mBtnConnectError.setOnClickListener {
            startActivity(Intent(this, A02AlertDialogActivity::class.java))
        }
        mBtnTokenExpired.setOnClickListener {
            startActivity(Intent(this, A03TokenExpiredActivity::class.java))
        }
        mBtnMultiTokenExpired.setOnClickListener {
            startActivity(Intent(this, A04MultiAsyncActivity::class.java))
        }
    }
}