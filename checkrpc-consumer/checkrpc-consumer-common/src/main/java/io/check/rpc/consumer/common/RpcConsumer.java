package io.check.rpc.consumer.common;

import io.binghe.rpc.constants.RpcConstants;
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

import java.net.ConnectException;
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

    //心跳间隔时间，默认30秒
    private int heartbeatInterval = 30000;

    //扫描并移除空闲连接时间，默认60秒
    private int scanNotActiveChannelInterval = 60000;

    //重试间隔时间
    private int retryInterval = 1000;

    //重试次数
    private int retryTimes = 3;

    //当前重试次数
    private volatile int currentConnectRetryTimes = 0;

    private RpcConsumer(int heartbeatInterval, int scanNotActiveChannelInterval, int retryInterval, int retryTimes) {
        this.retryInterval = retryInterval <= 0 ? RpcConstants.DEFAULT_RETRY_INTERVAL : retryInterval;
        this.retryTimes = retryTimes <= 0 ? RpcConstants.DEFAULT_RETRY_TIMES : retryTimes;
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        if (scanNotActiveChannelInterval > 0){
            this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        }
        localIp = IpUtils.getLocalHostIp();
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new RpcConsumerInitializer(heartbeatInterval));
        this.startHeartbeat();
    }

    public static RpcConsumer getInstance(int heartbeatInterval, int scanNotActiveChannelInterval, int retryInterval, int retryTimes){
        if(instance == null){
            synchronized (RpcConsumer.class){
                if(instance == null){
                    instance = new RpcConsumer(heartbeatInterval, scanNotActiveChannelInterval,retryInterval, retryTimes);
                }
            }
        }
        return instance;
    }

    private RpcConsumerHandler getRpcConsumerHandlerWithRetry(ServiceMeta serviceMeta) throws InterruptedException {
        logger.info("服务消费者连接服务提供者...");
        RpcConsumerHandler handler = null;
        try {
            handler = this.getRpcConsumerHandlerWithCache(serviceMeta);
        }catch (Exception e){
            //连接异常
            if (e instanceof ConnectException){
                //启动重试机制
                if (handler == null) {
                    if (currentConnectRetryTimes < retryTimes){
                        currentConnectRetryTimes++;
                        logger.info("服务消费者连接服务提供者第【{}】次重试...", currentConnectRetryTimes);
                        handler = this.getRpcConsumerHandlerWithRetry(serviceMeta);
                        Thread.sleep(retryInterval);
                    }
                }
            }
        }
        return handler;
    }

    /**
     * 从缓存中获取RpcConsumerHandler，缓存中没有，再创建
     */
    private RpcConsumerHandler getRpcConsumerHandlerWithCache(ServiceMeta serviceMeta) throws InterruptedException{
        RpcConsumerHandler handler = RpcConsumerHandlerHelper.get(serviceMeta);
        //缓存中无RpcClientHandler
        if (handler == null){
            handler = getRpcConsumerHandler(serviceMeta);
            RpcConsumerHandlerHelper.put(serviceMeta, handler);
        }else if (!handler.getChannel().isActive()){  //缓存中存在RpcClientHandler，但不活跃
            handler.close();
            handler = getRpcConsumerHandler(serviceMeta);
            RpcConsumerHandlerHelper.put(serviceMeta, handler);
        }
        return handler;
    }

    /**
     * 从注册中心获取服务的元数据信息。
     *
     * @param registryService 注册中心服务接口，用于从注册中心获取服务元数据
     * @param serviceKey 服务的关键字，用于定位具体服务
     * @param invokerHashCode 调用方的哈希码，用于区分不同的调用者
     * @return 返回服务的元数据信息，如果无法获取到，则返回null
     * @throws Exception 如果获取服务元数据过程中出现异常，则抛出
     */
    private ServiceMeta getServiceMeta(RegistryService registryService, String serviceKey, int invokerHashCode) throws Exception {
        // 首次尝试获取服务元数据
        logger.info("获取服务提供者元数据...");
        ServiceMeta serviceMeta = registryService.discovery(serviceKey, invokerHashCode, localIp);

        // 如果首次尝试未获取到服务元数据，则启动重试机制
        if (serviceMeta == null) {
            for (int i = 1; i <= retryTimes; i++){
                logger.info("获取服务提供者元数据第【{}】次重试...", i);
                // 尝试重获取服务元数据
                serviceMeta = registryService.discovery(serviceKey, invokerHashCode, localIp);
                // 如果获取成功，则跳出重试循环
                if(serviceMeta != null){
                    break;
                }
                // 等待一段时间后再次尝试
                Thread.sleep(retryInterval);
            }
        }
        return serviceMeta;
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
        }, 10, scanNotActiveChannelInterval, TimeUnit.MILLISECONDS);

        // 定时任务：发送ping消息
        // 3秒后开始第一次发送ping消息，之后每隔30秒发送一次
        executorService.scheduleAtFixedRate(() -> {
            logger.info("=============broadcastPingMessageFromConsumer============");
            ConsumerConnectionManager.broadcastPingMessageFromConsumer(this);
        }, 3, heartbeatInterval, TimeUnit.MILLISECONDS);
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
        ServiceMeta serviceMeta = getServiceMeta(registryService, serviceKey, invokerHashCode);
        RpcConsumerHandler handler = null;
        if (serviceMeta != null){
            handler = getRpcConsumerHandlerWithRetry(serviceMeta);
        }
        RPCFuture rpcFuture = null;
        if (handler != null){
            rpcFuture = handler.sendRequest(protocol, request.isAsync(), request.isOneway());
        }
        return rpcFuture;
    }


    /**
     * 创建连接并返回RpcClientHandler
     */
    private RpcConsumerHandler getRpcConsumerHandler(ServiceMeta serviceMeta) throws InterruptedException{
        return getRpcConsumerHandlerWithAddressAndPort(serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
    }

    public RpcConsumerHandler getRpcConsumerHandlerWithAddressAndPort(String address, int port) throws InterruptedException {
        ChannelFuture channelFuture =
                bootstrap.connect(address, port).sync();
        channelFuture.addListener((ChannelFutureListener) listener -> {
            if (channelFuture.isSuccess()) {
                logger.info("connect rpc server {} on port {} success.", address, port);
                //添加连接信息，在服务消费者端记录每个服务提供者实例的连接次数
                ServiceMeta serviceMeta = new ServiceMeta();
                serviceMeta.setServiceAddr(address);
                serviceMeta.setServicePort(port);
                ConnectionsContext.add(serviceMeta);
                currentConnectRetryTimes = 0;
            } else {
                logger.error("connect rpc server {} on port {} failed.", address, port);
                channelFuture.cause().printStackTrace();
                eventLoopGroup.shutdownGracefully();
            }
        });
        return channelFuture.channel().pipeline().get(RpcConsumerHandler.class);
    }
}
