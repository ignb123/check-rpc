package io.check.rpc.threadpool;

import io.check.rpc.constants.RpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ConcurrentThreadPool {

    private final Logger logger = LoggerFactory.getLogger(ConcurrentThreadPool.class);

    /**
     * 创建一个线程池执行器的实例。
     * 这个线程池执行器将被用来管理和执行任务。
     */
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 静态变量，用于保持ConcurrentThreadPool的单例实例。
     * 使用volatile关键字确保多线程环境下的正确可见性。
     */
    private static volatile ConcurrentThreadPool instance;

    private ConcurrentThreadPool(){

    }

    private ConcurrentThreadPool(int corePoolSize, int maximumPoolSize) {
        if (corePoolSize <= 0) {
            corePoolSize = RpcConstants.DEFAULT_CORE_POOL_SIZE;
        }
        if (maximumPoolSize <= 0) {
            maximumPoolSize = RpcConstants.DEFAULT_MAXI_NUM_POOL_SIZE;
        }
        if (corePoolSize > maximumPoolSize) {
            maximumPoolSize = corePoolSize;
        }
        this.threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                RpcConstants.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(RpcConstants.DEFAULT_QUEUE_CAPACITY));

    }

    public static ConcurrentThreadPool getInstance(int corePoolSize, int maximumPoolSize) {
        if(instance == null){
            synchronized (ConcurrentThreadPool.class){
                if(instance == null){
                    instance = new ConcurrentThreadPool(corePoolSize,maximumPoolSize);
                }
            }
        }
        return instance;
    }

    public void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }


    public <T> T submit(Callable<T> task){
        Future<T> future = threadPoolExecutor.submit(task);
        if (future == null){
            return null;
        }
        try {
            return future.get();
        } catch (Exception e) {
            logger.error("submit callable task exception:{}", e.getMessage());
        }
        return null;
    }


    public void stop(){
        threadPoolExecutor.shutdown();
    }
}
