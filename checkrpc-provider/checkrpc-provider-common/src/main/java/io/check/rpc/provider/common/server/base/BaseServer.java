package io.check.rpc.provider.common.server.base;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.codec.RpcDecoder;
import io.check.rpc.codec.RpcEncoder;
import io.check.rpc.flow.processor.FlowPostProcessor;
import io.check.rpc.provider.common.handler.RpcProviderHandler;
import io.check.rpc.provider.common.manager.ProviderConnectionManager;
import io.check.rpc.provider.common.server.api.Server.Server;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaseServer implements Server {
    private final Logger logger = LoggerFactory.getLogger(BaseServer.class);

    //主机域名或IP地址
    protected String host = "127.0.0.1";

    // 端口号
    protected int port = 27110;

    /**
     * 用于存储各种处理器或服务的映射，键值对形式，可根据键获取对应的处理器或服务对象。
     */
    protected Map<String,Object> handlerMap = new HashMap<>();

    /**
     * 注册中心服务，用于服务的注册与发现。
     */
    protected RegistryService registryService;

    /**
     * 用于反射识别的类型标识，可能用于动态加载或识别特定的服务或处理器类型。
     */
    private String reflectType;

    /**
     * 心跳定时任务线程池
     */
    private ScheduledExecutorService executorService;

    //心跳间隔时间，默认30秒
    private int heartbeatInterval = 3000;

    //扫描并移除空闲连接时间，默认60秒
    private int scanNotActiveChannelInterval = 6000;

    //结果缓存过期时长，默认5秒
    private int resultCacheExpire = 5000;

    //是否开启结果缓存
    private boolean enableResultCache;

    //核心线程数
    private int corePoolSize;

    //最大线程数
    private int maximumPoolSize;

    //流控分析后置处理器
    private FlowPostProcessor flowPostProcessor;

    //最大连接限制
    private int maxConnections;

    //拒绝策略类型
    private String disuseStrategyType;

    //是否开启数据缓冲
    private boolean enableBuffer;
    //缓冲区大小
    private int bufferSize;

    //是否开启限流
    private boolean enableRateLimiter;

    //限流类型
    private String rateLimiterType;

    //在milliSeconds毫秒内最多能够通过的请求个数
    private int permits;

    //毫秒数
    private int milliSeconds;

    public BaseServer(String serverAddress, String registryAddress, String registryType,
                      String registryLoadBalanceType, String reflectType,
                      int heartbeatInterval, int scanNotActiveChannelInterval,
                      boolean enableResultCache, int resultCacheExpire, int corePoolSize,
                      int maximumPoolSize, String flowType, int maxConnections, String disuseStrategyType,
                      boolean enableBuffer, int bufferSize, boolean enableRateLimiter, String rateLimiterType,
                      int permits, int milliSeconds){
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        if (scanNotActiveChannelInterval > 0){
            this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        }
        if(!StringUtils.isEmpty(serverAddress)){
            String[] serverArray = serverAddress.split(":");
            this.host = serverArray[0];
            this.port = Integer.parseInt(serverArray[1]);
        }

        this.reflectType = reflectType;
        this.registryService = this.getRegistryService(registryAddress,registryType,registryLoadBalanceType);

        if (resultCacheExpire > 0){
            this.resultCacheExpire = resultCacheExpire;
        }
        this.enableResultCache = enableResultCache;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.flowPostProcessor = ExtensionLoader.getExtension(FlowPostProcessor.class, flowType);
        this.maxConnections = maxConnections;
        this.disuseStrategyType = disuseStrategyType;
        this.enableBuffer = enableBuffer;
        this.bufferSize = bufferSize;
        this.enableRateLimiter = enableRateLimiter;
        this.rateLimiterType = rateLimiterType;
        this.permits = permits;
        this.milliSeconds = milliSeconds;
    }

    private void startHeartbeat() {
        executorService = Executors.newScheduledThreadPool(2);
        // 扫描并处理所有不活跃的连接
        executorService.scheduleAtFixedRate(() -> {
            logger.info("=============scanNotActiveChannel============");
            ProviderConnectionManager.scanNotActiveChannel();
        },10, scanNotActiveChannelInterval, TimeUnit.MILLISECONDS);

        // 发送ping消息
        executorService.scheduleAtFixedRate(()->{
            logger.info("=============broadcastPingMessageFromConsumer============");
            ProviderConnectionManager.broadcastPingMessageFromConsumer();
        }, 3, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    private RegistryService getRegistryService(String registryAddress, String registryType, String registryLoadBalanceType) {

        RegistryService registryService = null;
        try {
            registryService = ExtensionLoader.getExtension(RegistryService.class, registryType);
            registryService.init(new RegistryConfig(registryAddress, registryType,registryLoadBalanceType));
        }catch (Exception e){
            logger.error("RPC Server init error", e);
        }
        return registryService;
    }

    @Override
    public void startNettyServer() {
        // 启用心跳检测
        this.startHeartbeat();
        // 创建用于接受进来的连接的EventLoopGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建用于处理连接的EventLoopGroup
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 初始化ServerBootstrap，配置服务器参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workerGroup)
                    // 指定使用的NIO通道类型
                    .channel(NioServerSocketChannel.class)
                    // 配置子通道的处理器，用于初始化每个新建的连接
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            // 配置ChannelPipeline，添加编解码器和自定义处理器
                            channel.pipeline()
                                    .addLast(RpcConstants.CODEC_DECODER,new RpcDecoder(flowPostProcessor))
                                    .addLast(RpcConstants.CODEC_ENCODER,new RpcEncoder(flowPostProcessor))
                                    /**
                                     * 在Netty的服务端ChannelPipeline中添加一个空闲状态处理器（IdleStateHandler）。
                                     * 该处理器用于处理连接的空闲状态，以支持心跳机制。
                                     * @param new IdleStateHandler(0, 0, heartbeatInterval, TimeUnit.MILLISECONDS) 创建一个IdleStateHandler实例，
                                     *        其中不检测读或写活动的超时（即只检测连接的空闲时间），空闲时间由heartbeatInterval指定，
                                     *        单位为毫秒。这是实现心跳机制的一种方式，确保连接在指定的时间间隔内有活动，
                                     *        如果没有活动则可能触发连接断开或其他预定义的处理逻辑。
                                     */
                                    .addLast(RpcConstants.CODEC_SERVER_IDLE_HANDLER,
                                            new IdleStateHandler(0, 0, heartbeatInterval, TimeUnit.MILLISECONDS))
                                    .addLast(RpcConstants.CODEC_HANDLER,new RpcProviderHandler(handlerMap, enableResultCache,
                                            resultCacheExpire, corePoolSize, maximumPoolSize,reflectType,maxConnections,
                                            disuseStrategyType, enableBuffer, bufferSize, enableRateLimiter,
                                            rateLimiterType, permits, milliSeconds));
                        }
                    })
                    // 配置服务器端连接队列大小
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 配置子通道保持活动状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口并同步等待完成
            ChannelFuture future = bootstrap.bind(host, port).sync();
            // 记录服务器启动信息
            logger.info("Server started on {}:{}", host, port);
            // 同步等待通道关闭
            future.channel().closeFuture().sync();
        }catch (Exception e){
            // 记录启动过程中出现的异常
            logger.error("RPC Server start error", e);
        }finally {
            // 优雅关闭EventLoopGroup
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            executorService.shutdown();
        }

    }
}
