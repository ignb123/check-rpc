package io.check.rpc.ratelimiter.counter;

import io.check.rpc.ratelimiter.api.RateLimiterInvoker;
import io.check.rpc.ratelimiter.base.AbstractRateLimiterInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SPIClass
public class CounterRateLimiterInvoker extends AbstractRateLimiterInvoker {

    private final Logger logger = LoggerFactory.getLogger(CounterRateLimiterInvoker.class);

    private final AtomicInteger currentCounter = new AtomicInteger(0);

    private volatile long lastTimeStamp = System.currentTimeMillis();

    private final ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();

    @Override
    public boolean tryAcquire() {
        logger.info("execute counter rate limiter...");
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //超过一个执行周期
        if(currentTimeStamp - lastTimeStamp >= milliSeconds){
            lastTimeStamp = currentTimeStamp;
            currentCounter.set(0);
            return true;
        }
        //当前请求数小于配置的数量
        if (currentCounter.incrementAndGet() <= permits){
            threadLocal.set(true);
            return true;
        }
        return false;
    }

    @Override
    public void release() {
        if (threadLocal.get()){
            try {
                currentCounter.decrementAndGet();
            }finally {
                threadLocal.remove();
            }
        }
    }
}
