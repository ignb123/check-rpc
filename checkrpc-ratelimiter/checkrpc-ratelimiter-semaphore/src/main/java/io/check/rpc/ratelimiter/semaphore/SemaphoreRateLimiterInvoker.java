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

    @Override
    public void init(int permits, int milliSeconds) {
        super.init(permits, milliSeconds);
        this.semaphore = new Semaphore(permits);
    }
    @Override
    public boolean tryAcquire() {
        logger.info("execute semaphore rate limiter...");
        return semaphore.tryAcquire();
    }

    @Override
    public void release() {
        semaphore.release();
    }
}
