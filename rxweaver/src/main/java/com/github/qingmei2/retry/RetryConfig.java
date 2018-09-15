package com.github.qingmei2.retry;

public class RetryConfig {

    private static int DEFAULT_RETRY_TIMES = 1;
    private static int DEFAULT_DELAY_DURATION = 1000;

    private int maxRetries;
    private int delay;
    private boolean retryCondition;

    public RetryConfig() {
        this(DEFAULT_RETRY_TIMES, DEFAULT_DELAY_DURATION, false);
    }

    public RetryConfig(int maxRetries) {
        this(maxRetries, DEFAULT_DELAY_DURATION, false);
    }

    public RetryConfig(int maxRetries, int delay) {
        this(maxRetries, delay, false);
    }

    public RetryConfig(int maxRetries, int delay, boolean retryCondition) {
        this.maxRetries = maxRetries;
        this.delay = delay;
        this.retryCondition = retryCondition;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isRetryCondition() {
        return retryCondition;
    }
}
