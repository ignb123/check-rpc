package io.check.rpc.test.consumer.codec;

import io.check.rpc.test.consumer.codec.init.RpcTestConsumerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RpcTestConsumer {

    public static void main(String[] args) throws InterruptedException{
        /**
         * 初始化Netty的Bootstrap对象和EventLoopGroup对象。
         * <p>
         * Bootstrap是Netty中的启动器，用于配置和启动客户端或服务端的Netty程序。
         * EventLoopGroup是Netty中的事件循环组，负责处理I/O操作。这里使用NioEventLoopGroup实现，
         * 并指定创建4个事件循环，用于处理客户端的连接和I/O操作。
         */
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        try {
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcTestConsumerInitializer());
            bootstrap.connect("127.0.0.1", 27880).sync();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            Thread.sleep(2000);
            eventLoopGroup.shutdownGracefully();
        }
    }
}
