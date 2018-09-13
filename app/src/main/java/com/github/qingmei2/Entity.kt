package com.github.qingmei2

open class BaseEntity {
    val statusCode: Int = 0
    val message: String = ""
}

data class UserInfo(val username: String,
                    val age: Int) : BaseEntity()