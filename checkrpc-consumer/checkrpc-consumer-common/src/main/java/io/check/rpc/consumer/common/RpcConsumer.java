package io.check.rpc.consumer.common;

import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.common.ip.IpUtils;
import io.check.rpc.common.threadpool.ClientThreadPool;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.consumer.common.helper.RpcConsumerHandlerHelper;
import io.check.rpc.consumer.common.initializer.RpcConsumerInitializer;
import io.check.rpc.consumer.common.manager.ConsumerConnectionManager;
import io.check.rpc.loadbalancer.context.ConnectionsContext;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RpcConsumer implements Consumer {

    private final Logger logger = LoggerFactory.getLogger(RpcConsumer.class);
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private static volatile RpcConsumer instance;
    private static Map<String, RpcConsumerHandler> handlerMap = new ConcurrentHashMap<>();

    private final String localIp;

    /**
     * 定时任务类型的线程池，后续在服务消费者端会使用这个定时任务线程池向服务提供者定时发送心跳数据
     */
    private ScheduledExecutorService executorService;

    private RpcConsumer() {
        localIp = IpUtils.getLocalHostIp();
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new RpcConsumerInitializer());
        //TODO 启动心跳，后续优化
        this.startHeartbeat();
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

    /**
     * 启动心跳监测线程。该方法会初始化一个定时任务线程池，并调度两个定时任务：
     * 1. 扫描并处理所有不活跃的连接；
     * 2. 定期发送ping消息。
     */
    private void startHeartbeat() {
        // 初始化线程池，用于执行定时任务
        executorService = Executors.newScheduledThreadPool(2);

        // 定时任务：扫描并处理所有不活跃的连接
        // 10秒后开始第一次扫描，之后每隔60秒扫描一次
        executorService.scheduleAtFixedRate(() -> {
            logger.info("=============scanNotActiveChannel============");
            ConsumerConnectionManager.scanNotActiveChannel();
        }, 10, 60, TimeUnit.SECONDS);

        // 定时任务：发送ping消息
        // 3秒后开始第一次发送ping消息，之后每隔30秒发送一次
        executorService.scheduleAtFixedRate(() -> {
            logger.info("=============broadcastPingMessageFromConsumer============");
            ConsumerConnectionManager.broadcastPingMessageFromConsumer();
        }, 3, 30, TimeUnit.SECONDS);
    }


    public void close() {
        // 关闭RPC客户端处理器，断开所有与服务器的连接
        RpcConsumerHandlerHelper.closeRpcClientHandler();

        // 优雅关闭事件循环组，等待当前处理的事件完成后再彻底关闭
        eventLoopGroup.shutdownGracefully();

        // 关闭客户端线程池，停止接受新的任务并等待已提交任务完成
        ClientThreadPool.shutdown();

        executorService.shutdown();

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
                handler = getRpcConsumerHandler(serviceMeta);
                RpcConsumerHandlerHelper.put(serviceMeta, handler);
            }else if (!handler.getChannel().isActive()){ //缓存中存在RpcClientHandler，但不活跃
                handler.close();
                handler = getRpcConsumerHandler(serviceMeta);
                RpcConsumerHandlerHelper.put(serviceMeta, handler);
            }
            return handler.sendRequest(protocol, request.isAsync(), request.isOneway());
        }
        return null;
    }


    /**
     * 创建连接并返回RpcClientHandler
     */
    private RpcConsumerHandler getRpcConsumerHandler(ServiceMeta serviceMeta) throws InterruptedException{
        ChannelFuture channelFuture  = bootstrap.connect(serviceMeta.getServiceAddr(), serviceMeta.getServicePort()).sync();
        channelFuture.addListener((ChannelFutureListener) listener -> {
            if (channelFuture.isSuccess()) {
                logger.info("connect rpc server {} on port {} success.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                //添加连接信息，在服务消费者端记录每个服务提供者实例的连接次数
                ConnectionsContext.add(serviceMeta);
            }
            else {
                logger.error("connect rpc server {} on port {} failed.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                channelFuture.cause().printStackTrace();
                eventLoopGroup.shutdownGracefully();
            }
        });
        return channelFuture.channel()
                .pipeline().get(RpcConsumerHandler.class);
    }
}
