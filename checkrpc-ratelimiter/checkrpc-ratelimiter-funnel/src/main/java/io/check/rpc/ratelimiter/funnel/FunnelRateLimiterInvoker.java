package io.check.rpc.ratelimiter.funnel;

import io.check.rpc.ratelimiter.base.AbstractRateLimiterInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@SPIClass
public class FunnelRateLimiterInvoker extends AbstractRateLimiterInvoker {

    private final Logger logger = LoggerFactory.getLogger(FunnelRateLimiterInvoker.class);

    private BlockingQueue<Object> bucket; // 模拟漏桶的队列

    private AtomicInteger currentLevel; // 当前水位

    private Integer maxCapacity; // 桶的最大容量

    private long leakIntervalMillis; // 漏水间隔时间（毫秒）

    private ScheduledExecutorService scheduledExecutorService; // 漏水线程池

    private void startLeakThread() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            drain();
        },leakIntervalMillis,leakIntervalMillis,TimeUnit.MILLISECONDS);
    }

    private void drain() {
        // 模拟漏水过程，每次循环尝试减少一个单位的水
        if (currentLevel.get() > 0) {
            bucket.poll(); // 尝试移除一个元素，表示漏水
            currentLevel.decrementAndGet();
        }
    }

    @Override
    public boolean tryAcquire() {
        logger.info("execute funnel rate limiter...");
        // 尝试向桶中添加水（请求），如果桶已满则返回false
        if(currentLevel.get() < maxCapacity && bucket.offer(new Object())){
            currentLevel.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public void release() {
        // 漏桶算法一般不需要显式释放操作，因为超出速率的请求已被丢弃
    }

    @Override
    public void init(int permits, int milliSeconds) {
        super.init(permits, milliSeconds);
        this.maxCapacity = permits;
        this.bucket = new ArrayBlockingQueue<>(maxCapacity);
        this.currentLevel = new AtomicInteger(0);
        this.leakIntervalMillis = milliSeconds;
        // 启动漏水线程
        startLeakThread();
    }
}
