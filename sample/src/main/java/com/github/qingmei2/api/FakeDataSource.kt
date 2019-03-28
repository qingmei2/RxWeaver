package com.github.qingmei2.api

import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.UserInfo
import com.github.qingmei2.processor.tokens.AuthorizationErrorProcessor
import com.github.qingmei2.processor.GlobalErrorProcessor

object FakeDataSource {

    /**
     * 模拟服务器返回数据
     *
     * 默认返回Token失效401码，用户需要重新登录，如果登录成功，则刷新重新请求；
     * 15秒后，token会失效，再次请求仍然会得到401码。
     */
    fun queryUserInfo(): BaseEntity<UserInfo> {
        val currentTime = System.currentTimeMillis()
        val lastTokenRefreshTime = AuthorizationErrorProcessor.mLastRefreshTokenTimeStamp
        return when (lastTokenRefreshTime != 0L && currentTime - lastTokenRefreshTime <= 15000) {
            false -> BaseEntity(
                    statusCode = GlobalErrorProcessor.STATUS_UNAUTHORIZED,
                    message = "unauthorized",
                    data = null
            )
            true -> BaseEntity(
                    statusCode = GlobalErrorProcessor.STATUS_OK,
                    message = "success",
                    data = UserInfo("qingmei2", 26)
            )
        }
    }
}