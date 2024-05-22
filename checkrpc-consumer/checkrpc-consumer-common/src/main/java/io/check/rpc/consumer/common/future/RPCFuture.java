package io.check.rpc.consumer.common.future;

import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import io.check.rpc.common.threadpool.ClientThreadPool;
import io.check.rpc.consumer.common.callback.AsyncRPCCallback;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class RPCFuture extends CompletableFuture<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCFuture.class);

    private Sync sync;

    private RpcProtocol<RpcRequest> requestRpcProtocol;

    private RpcProtocol<RpcResponse> responseRpcProtocol;

    /**
     * 存放回调接口
     */
    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();

    /**
     * 添加和执行回调方法时，进行加锁和解锁操作
     */
    private ReentrantLock lock = new ReentrantLock();

    private long startTime;

    private long responseTimeThreshold = 5000; // 响应时间阈值，默认为5000ms

    /**
     * 构造函数初始化RPC请求协议、同步对象和开始时间。
     *
     * @param requestRpcProtocol RPC请求协议
     */
    public RPCFuture(RpcProtocol<RpcRequest> requestRpcProtocol) {
        this.sync = new Sync();
        this.requestRpcProtocol = requestRpcProtocol;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 判断RPC调用是否完成。
     *
     * @return 如果RPC调用已完成，则返回true；否则返回false。
     */
    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    /**
     * 获取RPC调用结果，如果未完成则阻塞。
     *
     * @return RPC调用结果，如果发生异常则抛出相应的异常。
     * @throws InterruptedException 如果线程被中断。
     * @throws ExecutionException 如果获取结果时发生异常。
     */
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.responseRpcProtocol != null) {
            return this.responseRpcProtocol.getBody().getResult();
        } else {
            return null;
        }
    }

    /**
     * 获取RPC调用结果，如果在指定超时时间内未完成则抛出TimeoutException。
     *
     * @param timeout 超时时间。
     * @param unit 超时时间单位。
     * @return RPC调用结果，如果发生异常则抛出相应的异常。
     * @throws InterruptedException 如果线程被中断。
     * @throws ExecutionException 如果获取结果时发生异常。
     * @throws TimeoutException 如果在指定时间内获取结果超时。
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.responseRpcProtocol != null) {
                return this.responseRpcProtocol.getBody().getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.requestRpcProtocol.getHeader().getRequestId()
                    + ". Request class name: " + this.requestRpcProtocol.getBody().getClassName()
                    + ". Request method: " + this.requestRpcProtocol.getBody().getMethodName());
        }
    }

    /**
     * 判断RPC调用是否被取消。当前不支持取消操作，故抛出UnsupportedOperationException。
     *
     * @return 未实现，总是抛出UnsupportedOperationException。
     */
    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试取消RPC调用。当前不支持取消操作，故抛出UnsupportedOperationException。
     *
     * @param mayInterruptIfRunning 是否中断正在执行的调用。
     * @return 未实现，总是抛出UnsupportedOperationException。
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    /**
     * 当RPC调用完成时调用此方法，设置响应协议并释放同步对象。
     *
     * @param responseRpcProtocol RPC响应协议。
     */
    public void done(RpcProtocol<RpcResponse> responseRpcProtocol) {
        this.responseRpcProtocol = responseRpcProtocol;
        sync.release(1);
        //新增的调用invokeCallbacks()方法
        invokeCallbacks();
        // 计算响应时间并记录警告信息（如果响应时间超过阈值）
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            LOGGER.warn("Service response time is too slow. Request id = " +
                    responseRpcProtocol.getHeader().getRequestId() +
                    ". Response Time = " + responseTime + "ms");
        }
    }

    private void runCallback(final AsyncRPCCallback callback) {
        final RpcResponse res = this.responseRpcProtocol.getBody();
        ClientThreadPool.submit(() -> {
            if (!res.isError()) {
                callback.onSuccess(res.getResult());
            } else{
                callback.onException(new RuntimeException("Response error", new Throwable(res.getError())));
            }
        });
    }

    public RPCFuture addCallback(AsyncRPCCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        }finally {
            lock.unlock();
        }
        return this;
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final AsyncRPCCallback callback : pendingCallbacks){
                runCallback(callback);
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * Sync是一个基于AbstractQueuedSynchronizer实现的同步器，
     * 用于管理状态的转换，主要是done和pending两种状态。
     */
    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        // 定义两种状态：完成（done）和等待（pending）
        private final int done = 1;
        private final int pending = 0;

        /**
         * 尝试获取同步状态。
         * @param acquires 获取的资源数量，此处未使用
         * @return 如果当前状态为完成（done），则返回true，否则返回false
         */
        protected boolean tryAcquire(int acquires) {
            return getState() == done;
        }

        /**
         * 尝试释放同步状态。
         * @param releases 释放的资源数量，此处未使用
         * @return 如果当前状态为等待（pending），且成功将状态设置为完成（done），则返回true，否则返回false
         */
        protected boolean tryRelease(int releases) {
            if (getState() == pending) {
                // 使用CAS操作将状态从pending改为done
                if (compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 检查同步状态是否为完成（done）。
         * @return 如果当前状态为完成（done），则返回true，否则返回false
         */
        public boolean isDone() {
            // 获取当前状态以判断是否完成
            getState();
            return getState() == done;
        }

    }
}
