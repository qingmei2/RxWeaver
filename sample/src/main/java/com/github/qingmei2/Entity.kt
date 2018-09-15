package com.github.qingmei2

open class BaseEntity {
    var statusCode: Int = 0
    var message: String = ""
}

data class UserInfo(val username: String = "",
                    val age: Int = 0) : BaseEntity()