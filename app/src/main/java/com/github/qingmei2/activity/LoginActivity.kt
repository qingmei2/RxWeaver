package com.github.qingmei2.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import qingmei2.github.qingmei2.R

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnSuccess.setOnClickListener {
            login(true)
        }
        btnFailed.setOnClickListener {
            login(false)
        }
    }

    private fun login(success: Boolean) {
        val intent = Intent().putExtra(EXTRA_SUCCESS, success)

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_SUCCESS = "EXTRA_SUCCESS"
    }
}
