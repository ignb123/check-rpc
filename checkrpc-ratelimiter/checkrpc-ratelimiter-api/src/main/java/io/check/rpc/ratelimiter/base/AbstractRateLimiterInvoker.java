package io.check.rpc.ratelimiter.base;

import io.check.rpc.ratelimiter.api.RateLimiterInvoker;

public abstract class AbstractRateLimiterInvoker implements RateLimiterInvoker {
    protected int permits;
    protected int milliSeconds;
    @Override
    public void init(int permits, int milliSeconds) {
        this.permits = permits;
        this.milliSeconds = milliSeconds;
    }
}
