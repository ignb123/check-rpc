package io.check.rpc.consumer.common.initializer;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.codec.RpcDecoder;
import io.check.rpc.codec.RpcEncoder;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class RpcConsumerInitializer extends ChannelInitializer<SocketChannel> {

    // 心跳间隔时间
    private int heartbeatInterval;

    public RpcConsumerInitializer(int heartbeatInterval) {
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(RpcConstants.CODEC_ENCODER, new RpcEncoder());
        cp.addLast(RpcConstants.CODEC_DECODER, new RpcDecoder());
        cp.addLast(RpcConstants.CODEC_CLIENT_IDLE_HANDLER, new IdleStateHandler(heartbeatInterval, 0, 0, TimeUnit.MILLISECONDS));
        cp.addLast(RpcConstants.CODEC_HANDLER, new RpcConsumerHandler());
    }
}
