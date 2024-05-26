package io.check.rpc.consumer;

import io.check.rpc.common.exception.RegistryException;
import io.check.rpc.consumer.common.RpcConsumer;
import io.check.rpc.proxy.api.ProxyFactory;
import io.check.rpc.proxy.api.async.IAsyncObjectProxy;
import io.check.rpc.proxy.api.config.ProxyConfig;
import io.check.rpc.proxy.api.object.ObjectProxy;
import io.check.rpc.proxy.jdk.JdkProxyFactory;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.registry.zookeeper.ZookeeperRegistryService;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {

    private final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    /**
     * 服务版本
     */
    private String serviceVersion;

    /**
     * 服务分组
     */
    private String serviceGroup;

    /**
     * 序列化类型
     */
    private String serializationType;

    /**
     * 超时时间
     */
    private long timeout;

    /**
     * 是否异步调用
     */
    private boolean async;

    /**
     * 是否单向调用
     */
    private boolean oneway;

    /**
     * 动态代理方式
     */
    private String proxy;

    private RegistryService registryService;

    //心跳间隔时间，默认30秒
    private int heartbeatInterval;

    //扫描空闲连接时间，默认60秒
    private int scanNotActiveChannelInterval;

    //重试间隔时间
    private int retryInterval = 1000;

    //重试次数
    private int retryTimes = 3;

    /**
     * 是否开启结果缓存
     */
    private boolean enableResultCache;

    /**
     * 缓存结果的时长，单位是毫秒
     */
    private int resultCacheExpire;

    /**
     * 是否开启直连服务
     */
    private boolean enableDirectServer;

    /**
     * 直连服务的地址
     */
    private String directServerUrl;

    public RpcClient(String registryAddress, String registryType, String registryLoadBalanceType, String proxy,
                     String serviceVersion, String serviceGroup, String serializationType, long timeout, boolean async,
                     boolean oneway,int heartbeatInterval, int scanNotActiveChannelInterval,
                     int retryInterval, int retryTimes, boolean enableResultCache, int resultCacheExpire,
                     boolean enableDirectServer, String directServerUrl) {
        this.retryInterval = retryInterval;
        this.retryTimes = retryTimes;
        this.serviceVersion = serviceVersion;
        this.proxy = proxy;
        this.timeout = timeout;
        this.serviceGroup = serviceGroup;
        this.serializationType = serializationType;
        this.async = async;
        this.oneway = oneway;
        this.registryService = this.getRegistryService(registryAddress, registryType, registryLoadBalanceType);
        this.heartbeatInterval = heartbeatInterval;
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        this.enableResultCache = enableResultCache;
        this.resultCacheExpire = resultCacheExpire;
        this.enableDirectServer = enableDirectServer;
        this.directServerUrl = directServerUrl;
    }

    private RegistryService getRegistryService(String registryAddress, String registryType, String registryLoadBalanceType) {
        if (StringUtils.isEmpty(registryType)){
            throw new IllegalArgumentException("registry type is null");
        }

        RegistryService registryService = ExtensionLoader.getExtension(RegistryService.class, registryType);
        try {
            registryService.init(new RegistryConfig(registryAddress, registryType,registryLoadBalanceType));
        }catch (Exception e){
            logger.error("RpcClient init registry service throws exception:{}", e);
            throw new RegistryException(e.getMessage(), e);
        }
        return registryService;
    }

    public <T> T create(Class<T> interfaceClass) {
        ProxyFactory proxyFactory = ExtensionLoader.getExtension(ProxyFactory.class, proxy);
        proxyFactory.init(new ProxyConfig(interfaceClass, serviceVersion, serviceGroup, timeout,
                RpcConsumer.getInstance()
                        .setHeartbeatInterval(heartbeatInterval)
                        .setRetryInterval(retryInterval)
                        .setDirectServerUrl(directServerUrl)
                        .setEnableDirectServer(enableDirectServer)
                        .setRetryTimes(retryTimes)
                        .setScanNotActiveChannelInterval(scanNotActiveChannelInterval),
                serializationType, async, oneway, registryService, enableResultCache, resultCacheExpire));
        return proxyFactory.getProxy(interfaceClass);
    }

    public <T> IAsyncObjectProxy createAsync(Class<T> interfaceClass) {
        return new ObjectProxy<T>(interfaceClass, serviceVersion, serviceGroup, serializationType, timeout,
                RpcConsumer.getInstance()
                        .setHeartbeatInterval(heartbeatInterval)
                        .setRetryInterval(retryInterval)
                        .setDirectServerUrl(directServerUrl)
                        .setEnableDirectServer(enableDirectServer)
                        .setRetryTimes(retryTimes)
                        .setScanNotActiveChannelInterval(scanNotActiveChannelInterval)
                ,
                async, oneway, registryService,enableResultCache, resultCacheExpire);    }
    public void shutdown() {
        RpcConsumer.getInstance().close();
    }
}
