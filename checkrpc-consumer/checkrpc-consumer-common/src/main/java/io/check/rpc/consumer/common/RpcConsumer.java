package io.check.rpc.consumer.common;

import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.common.ip.IpUtils;
import io.check.rpc.common.threadpool.ClientThreadPool;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.consumer.common.helper.RpcConsumerHandlerHelper;
import io.check.rpc.consumer.common.initializer.RpcConsumerInitializer;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.proxy.api.consumer.Consumer;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.registry.api.RegistryService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcConsumer implements Consumer {

    private final Logger logger = LoggerFactory.getLogger(RpcConsumer.class);
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private static volatile RpcConsumer instance;
    private static Map<String, RpcConsumerHandler> handlerMap = new ConcurrentHashMap<>();

    private final String localIp;

    private RpcConsumer() {
        localIp = IpUtils.getLocalHostIp();
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new RpcConsumerInitializer());
    }

    public static RpcConsumer getInstance(){
        if(instance == null){
            synchronized (RpcConsumer.class){
                if(instance == null){
                    instance = new RpcConsumer();
                }
            }
        }
        return instance;
    }

    public void close() {
        // 关闭RPC客户端处理器，断开所有与服务器的连接
        RpcConsumerHandlerHelper.closeRpcClientHandler();

        // 优雅关闭事件循环组，等待当前处理的事件完成后再彻底关闭
        eventLoopGroup.shutdownGracefully();

        // 关闭客户端线程池，停止接受新的任务并等待已提交任务完成
        ClientThreadPool.shutdown();

    }

    @Override
    public RPCFuture sendRequest(RpcProtocol<RpcRequest> protocol, RegistryService registryService) throws Exception{
        RpcRequest request = protocol.getBody();
        String serviceKey = RpcServiceHelper
                .buildServiceKey(request.getClassName(), request.getVersion(), request.getGroup());
        Object[] params = request.getParameters();
        int invokerHashCode = (params == null || params.length == 0) ? serviceKey.hashCode() : params[0].hashCode();
        ServiceMeta serviceMeta = registryService.discovery(serviceKey, invokerHashCode, localIp);
        if(serviceMeta != null){
            RpcConsumerHandler handler = RpcConsumerHandlerHelper.get(serviceMeta);
            // 缓存中无RpcClientHandler
            if(handler == null){
                handler = getRpcConsumerHandler(serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                RpcConsumerHandlerHelper.put(serviceMeta, handler);
            }else if (!handler.getChannel().isActive()){ //缓存中存在RpcClientHandler，但不活跃
                handler.close();
                handler = getRpcConsumerHandler(serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                RpcConsumerHandlerHelper.put(serviceMeta, handler);
            }
            return handler.sendRequest(protocol, request.isAsync(), request.isOneway());
        }
        return null;
    }


    /**
     * 创建连接并返回RpcClientHandler
     */
    private RpcConsumerHandler getRpcConsumerHandler(String serviceAddress, int port) throws InterruptedException{
        ChannelFuture channelFuture  = bootstrap.connect(serviceAddress, port).sync();
        channelFuture.addListener((ChannelFutureListener) listener -> {
            if (channelFuture.isSuccess()) {
                logger.info("connect rpc server {} on port {} success.", serviceAddress, port);
            }
            else {
                logger.error("connect rpc server {} on port {} failed.", serviceAddress, port);
                channelFuture.cause().printStackTrace();
                eventLoopGroup.shutdownGracefully();
            }
        });
        return channelFuture.channel()
                .pipeline().get(RpcConsumerHandler.class);
    }
}
