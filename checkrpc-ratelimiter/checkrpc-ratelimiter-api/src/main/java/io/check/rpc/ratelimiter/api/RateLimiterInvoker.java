package io.check.rpc.ratelimiter.api;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.spi.annotation.SPI;

@SPI(RpcConstants.DEFAULT_RATELIMITER_INVOKER)
public interface RateLimiterInvoker {

    /**
     * 尝试获取资源，如果获取成功，则返回true，如果获取失败，则返回false
     * @return
     */
    boolean tryAcquire();

    /**
     * 释放资源
     */
    void release();

    /**
     * @param permits 在milliSeconds毫秒内最多能够获取的资源数量
     * @param milliSeconds 毫秒数
     */
    default void init(int permits, int milliSeconds){}
}
