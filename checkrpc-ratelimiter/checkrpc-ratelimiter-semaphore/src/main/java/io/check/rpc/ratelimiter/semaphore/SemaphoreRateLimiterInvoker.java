package io.check.rpc.ratelimiter.semaphore;

import io.check.rpc.ratelimiter.api.RateLimiterInvoker;
import io.check.rpc.ratelimiter.base.AbstractRateLimiterInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@SPIClass
public class SemaphoreRateLimiterInvoker extends AbstractRateLimiterInvoker {

    private final Logger logger = LoggerFactory.getLogger(SemaphoreRateLimiterInvoker.class);
    private Semaphore semaphore;

    private final AtomicInteger currentCounter = new AtomicInteger(0);

    private volatile long lastTimeStamp = System.currentTimeMillis();

    @Override
    public void init(int permits, int milliSeconds) {
        super.init(permits, milliSeconds);
        this.semaphore = new Semaphore(permits);
    }

    /**
     * 尝试获取信号量资源。
     * 此方法实现了对资源访问的速率限制。当超过设定的时间周期后，会重置访问窗口，并释放所有持有的资源。
     * 在每个时间周期内，尝试获取资源，如果获取成功，则增加当前计数。
     *
     * @return boolean 如果成功获取资源，则返回true；否则返回false。
     */
    @Override
    public boolean tryAcquire() {
        logger.info("execute semaphore rate limiter...");
        // 获取当前时间戳
        long currentTimeStamp = System.currentTimeMillis();
        // 检查是否超过一个时间周期
        if (currentTimeStamp - lastTimeStamp >= milliSeconds){
            // 重置窗口开始时间
            lastTimeStamp = currentTimeStamp;
            // 释放所有当前持有的资源
            semaphore.release(currentCounter.get());
            // 重置计数器
            currentCounter.set(0);
        }
        // 尝试获取信号量资源
        boolean result = semaphore.tryAcquire();
        // 如果成功获取资源，则增加当前计数
        if (result){
            currentCounter.incrementAndGet();
        }
        return result;
    }


    @Override
    public void release() {
        //TODO ignore
    }
}
