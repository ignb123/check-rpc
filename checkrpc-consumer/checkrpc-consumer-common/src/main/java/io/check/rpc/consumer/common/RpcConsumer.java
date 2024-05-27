package io.check.rpc.consumer.common;

import io.check.rpc.common.exception.RpcException;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.common.ip.IpUtils;
import io.check.rpc.common.threadpool.ClientThreadPool;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.consumer.common.helper.RpcConsumerHandlerHelper;
import io.check.rpc.consumer.common.initializer.RpcConsumerInitializer;
import io.check.rpc.consumer.common.manager.ConsumerConnectionManager;
import io.check.rpc.flow.processor.FlowPostProcessor;
import io.check.rpc.loadbalancer.context.ConnectionsContext;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.proxy.api.consumer.Consumer;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.check.rpc.threadpool.ConcurrentThreadPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
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

    //是否开启直连服务
    private boolean enableDirectServer = false;

    //直连服务的地址,格式为IP:PORT
    private String directServerUrl;

    //是否开启延迟连接
    private boolean enableDelayConnection = false;

    //未开启延迟连接时，是否已经初始化连接
    private volatile boolean initConnection = false;

    private ConcurrentThreadPool concurrentThreadPool;

    private FlowPostProcessor flowPostProcessor;

    //是否开启数据缓冲
    private boolean enableBuffer;

    //缓冲区大小
    private int bufferSize;


    private RpcConsumer() {
        localIp = IpUtils.getLocalHostIp();
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
    }

    public RpcConsumer setEnableBuffer(boolean enableBuffer) {
        this.enableBuffer = enableBuffer;
        return this;
    }

    public RpcConsumer setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public RpcConsumer setEnableDirectServer(boolean enableDirectServer) {
        this.enableDirectServer = enableDirectServer;
        return this;
    }

    public RpcConsumer setDirectServerUrl(String directServerUrl) {
        this.directServerUrl = directServerUrl;
        return this;
    }

    public RpcConsumer setHeartbeatInterval(int heartbeatInterval) {
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        return this;
    }

    public RpcConsumer setConcurrentThreadPool(ConcurrentThreadPool concurrentThreadPool) {
        this.concurrentThreadPool = concurrentThreadPool;
        return this;
    }

    public RpcConsumer setScanNotActiveChannelInterval(int scanNotActiveChannelInterval) {
        if (scanNotActiveChannelInterval > 0){
            this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        }
        return this;
    }

    public RpcConsumer setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval <= 0 ? RpcConstants.DEFAULT_RETRY_INTERVAL : retryInterval;
        return this;
    }

    public RpcConsumer setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes <= 0 ? RpcConstants.DEFAULT_RETRY_TIMES : retryTimes;
        return this;
    }

    public RpcConsumer setEnableDelayConnection(boolean enableDelayConnection) {
        this.enableDelayConnection = enableDelayConnection;
        return this;
    }

    public RpcConsumer setFlowPostProcessor(String flowType){
        if (StringUtils.isEmpty(flowType)){
            flowType = RpcConstants.FLOW_POST_PROCESSOR_PRINT;
        }
        this.flowPostProcessor = ExtensionLoader.getExtension(FlowPostProcessor.class, flowType);
        return this;
    }


    public static RpcConsumer getInstance(){
        if (instance == null){
            synchronized (RpcConsumer.class){
                if (instance == null){
                    instance = new RpcConsumer();
                }
            }
        }
        return instance;

    }

    /**
     * 实例化bootstrap对象，并返回this对象
     * @return
     */
    public RpcConsumer buildNettyGroup(){
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new RpcConsumerInitializer(enableBuffer, bufferSize, heartbeatInterval, concurrentThreadPool, flowPostProcessor));
        return this;
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

    private RpcConsumerHandler getRpcConsumerHandlerWithRetry(RegistryService registryService, String serviceKey, int invokerHashCode) throws Exception{
        logger.info("获取服务消费者处理器...");
        RpcConsumerHandler handler = getRpcConsumerHandler(registryService, serviceKey, invokerHashCode);
        //获取的handler为空，启动重试机制
        if (handler == null){
            for (int i = 1; i <= retryTimes; i++){
                logger.info("获取服务消费者处理器第【{}】次重试...", i);
                handler = getRpcConsumerHandler(registryService, serviceKey, invokerHashCode);
                if (handler != null){
                    break;
                }
                Thread.sleep(retryInterval);
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
    private ServiceMeta getServiceMetaWithRetry(RegistryService registryService, String serviceKey,
                                                int invokerHashCode) throws Exception {
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
     * 在服务消费者启动时，与服务提供者进行连接
     * @param registryService
     */
    private void initConnection(RegistryService registryService) {
        List<ServiceMeta> serviceMetaList = new ArrayList<>();
        try {
            if (enableDirectServer){
                if (!directServerUrl.contains(RpcConstants.RPC_MULTI_DIRECT_SERVERS_SEPARATOR)){
                    serviceMetaList.add(this.getDirectServiceMetaWithCheck(directServerUrl));
                }else{
                    serviceMetaList.addAll(this.getMultiServiceMeta(directServerUrl));
                }
            }else {
                serviceMetaList = registryService.discoveryAll();
            }
        }catch (Exception e){
            logger.error("init connection throws exception, the message is: {}", e.getMessage());
        }

        for(ServiceMeta serviceMeta : serviceMetaList){
            RpcConsumerHandler handler = null;
            try {
                handler = this.getRpcConsumerHandler(serviceMeta);
            } catch (InterruptedException e) {
                logger.error("call getRpcConsumerHandler() method throws InterruptedException, the message is: {}", e.getMessage());
                continue;
            }
            RpcConsumerHandlerHelper.put(serviceMeta, handler);
        }
    }

    /**
     * 判断服务消费者是否配置了延迟连接，当服务消费者未配置延迟连接，
     * 并且非初始化连接时，会调用initConnection()方法建立连接，
     * 并将是否初始化连接的成员变量initConnection设置为true
     * @param registryService
     * @return
     */
    public RpcConsumer buildConnection(RegistryService registryService){
        //未开启延迟连接，并且未初始化连接
        if (!enableDelayConnection && !initConnection){
            this.initConnection(registryService);
            this.initConnection = true;
        }
        //TODO 启动心跳，后续优化
        this.startHeartbeat();
        return this;
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
        RpcConsumerHandler handler = getRpcConsumerHandlerWithRetry(registryService, serviceKey, invokerHashCode);
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

    private RpcConsumerHandler getRpcConsumerHandler(RegistryService registryService, String serviceKey, int invokerHashCode) throws Exception {
        ServiceMeta serviceMeta = this.getDirectServiceMetaOrWithRetry(registryService, serviceKey, invokerHashCode);
        RpcConsumerHandler handler = null;
        if (serviceMeta != null){
            handler = getRpcConsumerHandlerWithRetry(serviceMeta);
        }
        return handler;
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

    /**
     * 通过enableDirectServer的值决定开启直连模式，则直接返回直连的ServiceMeta对象，否则通过注册中心获取服务元数据
     */
    private ServiceMeta getDirectServiceMetaOrWithRetry(RegistryService registryService, String serviceKey, int invokerHashCode) throws Exception {
        ServiceMeta serviceMeta = null;
        if (enableDirectServer){
            serviceMeta = this.getServiceMeta(directServerUrl,registryService,invokerHashCode);
        }else {
            serviceMeta = this.getServiceMetaWithRetry(registryService, serviceKey, invokerHashCode);
        }
        return serviceMeta;
    }

    /**
     * 根据传入的服务提供者地址是单个地址还是多个地址来调用不同的方法，如果传入的是单个服务提供者地址，
     * 则调用getDirectServiceMetaWithCheck()方法获取ServiceMeta对象。如果传入的是多个服务提供者地址，
     * 首先调用getMultiServiceMeta()方法获取ServiceMeta列表，
     * 再通过registryService的select()方法从ServiceMeta列表中根据一定的负载均衡规则获取一个ServiceMeta对象
     * @param directServerUrl
     * @param registryService
     * @param invokerHashCode
     * @return
     */
    private ServiceMeta getServiceMeta(String directServerUrl, RegistryService registryService, int invokerHashCode){
        logger.info("服务消费者直连服务提供者...");
        //只配置了一个服务提供者地址
        if (!directServerUrl.contains(RpcConstants.RPC_MULTI_DIRECT_SERVERS_SEPARATOR)){
            return getDirectServiceMetaWithCheck(directServerUrl);
        }
        return registryService.select(this.getMultiServiceMeta(directServerUrl), invokerHashCode, localIp);
    }

    /**
     * 检测单个服务提供者地址的合法性，
     * 并调用getDirectServiceMeta()方法将其封装成ServiceMeta并返回
     * @param directServerUrl
     * @return
     */
    private ServiceMeta getDirectServiceMetaWithCheck(String directServerUrl){
        if (StringUtils.isEmpty(directServerUrl)){
            throw new RpcException("direct server url is null ...");
        }
        if (!directServerUrl.contains(RpcConstants.IP_PORT_SPLIT)){
            throw new RpcException("direct server url not contains : ");
        }
        return this.getDirectServiceMeta(directServerUrl);
    }

    /**
     * 直接解析单个服务提供者的地址，并将其封装成ServiceMeta对象
     * @param directServerUrl
     * @return
     */
    private ServiceMeta getDirectServiceMeta(String directServerUrl) {
        ServiceMeta serviceMeta = new ServiceMeta();
        String[] directServerUrlArray = directServerUrl.split(RpcConstants.IP_PORT_SPLIT);
        serviceMeta.setServiceAddr(directServerUrlArray[0].trim());
        serviceMeta.setServicePort(Integer.parseInt(directServerUrlArray[1].trim()));
        return serviceMeta;
    }

    /**
     * 处理服务消费者直连多个服务提供者的逻辑，将多个服务提供者的地址封装成一个ServiceMeta类型的集合
     * @param directServerUrl
     * @return
     */
    private List<ServiceMeta> getMultiServiceMeta(String directServerUrl){
        List<ServiceMeta> serviceMetaList = new ArrayList<>();
        String[] directServerUrlArray = directServerUrl.split(RpcConstants.RPC_MULTI_DIRECT_SERVERS_SEPARATOR);
        if (directServerUrlArray != null && directServerUrlArray.length > 0){
            for (String directUrl : directServerUrlArray){
                serviceMetaList.add(getDirectServiceMeta(directUrl));
            }
        }
        return serviceMetaList;
    }
}
