package com.github.qingmei2.retry

class RetryConfig(val maxRetries: Int,
                  val delay: Int,
                  val condition: (Throwable) -> Boolean) {

    class Builder {

        private var maxRetries = DEFAULT_RETRY_TIMES
        private var dealyMilliseconds = DEFAULT_DELAY_DURATION
        private var condition: (Throwable) -> Boolean = { _ -> true }

        fun setMaxRetries(maxRetries: Int): Builder {
            this.maxRetries = maxRetries
            return this
        }

        fun setDelay(dealyMilliseconds: Int): Builder {
            this.dealyMilliseconds = dealyMilliseconds
            return this
        }

        fun setCondition(condition: (Throwable) -> Boolean): Builder {
            this.condition = condition
            return this
        }

        fun build(): RetryConfig {
            return RetryConfig(maxRetries, dealyMilliseconds, condition)
        }
    }

    companion object {

        private val DEFAULT_RETRY_TIMES = 1
        private val DEFAULT_DELAY_DURATION = 1000
    }
}
