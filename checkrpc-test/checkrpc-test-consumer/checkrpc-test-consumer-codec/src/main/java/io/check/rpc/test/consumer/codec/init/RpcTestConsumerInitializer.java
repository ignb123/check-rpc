package io.check.rpc.test.consumer.codec.init;

import io.check.rpc.codec.RpcDecoder;
import io.check.rpc.codec.RpcEncoder;
import io.check.rpc.flow.processor.FlowPostProcessor;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.check.rpc.test.consumer.codec.handler.RpcTestConsumerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class RpcTestConsumerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();
        cp.addLast(new RpcEncoder(ExtensionLoader.getExtension(FlowPostProcessor.class,"print")));
        cp.addLast(new RpcDecoder(ExtensionLoader.getExtension(FlowPostProcessor.class,"print")));
        cp.addLast(new RpcTestConsumerHandler());
    }
}
