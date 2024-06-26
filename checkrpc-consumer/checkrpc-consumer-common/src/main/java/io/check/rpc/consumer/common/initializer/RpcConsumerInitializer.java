package io.check.rpc.consumer.common.initializer;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.codec.RpcDecoder;
import io.check.rpc.codec.RpcEncoder;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.exception.processor.ExceptionPostProcessor;
import io.check.rpc.flow.processor.FlowPostProcessor;
import io.check.rpc.threadpool.ConcurrentThreadPool;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class RpcConsumerInitializer extends ChannelInitializer<SocketChannel> {

    // 心跳间隔时间
    private int heartbeatInterval;

    private ConcurrentThreadPool concurrentThreadPool;

    private FlowPostProcessor flowPostProcessor;

    private boolean enableBuffer;

    private int bufferSize;

    //异常处理后置处理器
    private ExceptionPostProcessor exceptionPostProcessor;


    public RpcConsumerInitializer(boolean enableBuffer, int bufferSize,int heartbeatInterval, ConcurrentThreadPool concurrentThreadPool,
                                  FlowPostProcessor flowPostProcessor, ExceptionPostProcessor exceptionPostProcessor) {
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        this.concurrentThreadPool = concurrentThreadPool;
        this.flowPostProcessor = flowPostProcessor;
        this.enableBuffer = enableBuffer;
        this.bufferSize = bufferSize;
        this.exceptionPostProcessor = exceptionPostProcessor;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(RpcConstants.CODEC_ENCODER, new RpcEncoder(flowPostProcessor));
        cp.addLast(RpcConstants.CODEC_DECODER, new RpcDecoder(flowPostProcessor));
        cp.addLast(RpcConstants.CODEC_CLIENT_IDLE_HANDLER, new IdleStateHandler(heartbeatInterval, 0, 0, TimeUnit.MILLISECONDS));
        cp.addLast(RpcConstants.CODEC_HANDLER, new RpcConsumerHandler(enableBuffer, bufferSize, concurrentThreadPool, exceptionPostProcessor));
    }
}
