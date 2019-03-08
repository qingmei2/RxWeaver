package com.github.qingmei2.entity

data class BaseEntity<out T>(
        val statusCode: Int,
        val message: String,
        val data: T?
)

data class UserInfo(
        val username: String,
        val age: Int
)