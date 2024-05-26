package io.check.rpc.proxy.api.config;

import io.check.rpc.proxy.api.consumer.Consumer;
import io.check.rpc.registry.api.RegistryService;

import java.io.Serializable;

public class ProxyConfig<T> implements Serializable {

    private static final long serialVersionUID = 6648940252795742398L;

    /**
     * 接口的Class实例
     */
    private Class<T> clazz;

    /**
     * 服务版本号
     */
    private String serviceVersion;

    /**
     * 服务分组
     */
    private String serviceGroup;

    /**
     * 超时时间
     */
    private long timeout;

    /**
     * 消费者接口
     */
    private Consumer consumer;

    /**
     * 序列化类型
     */
    private String serializationType;

    /**
     * 是否异步调用
     */
    private boolean async;

    /**
     * 是否单向调用
     */
    private boolean oneway;

    /**
     * 服务注册与发现接口
     */
    private RegistryService registryService;

    /**
     * 是否开启结果缓存
     */
    private boolean enableResultCache;

    /**
     * 结果缓存的时长
     */
    private int resultCacheExpire;

    public ProxyConfig(Class<T> clazz, String serviceVersion, String serviceGroup, long timeout, Consumer consumer,
                       String serializationType, boolean async, boolean oneway, RegistryService registryService,
                       boolean enableResultCache, int resultCacheExpire) {
        this.clazz = clazz;
        this.serviceVersion = serviceVersion;
        this.serviceGroup = serviceGroup;
        this.timeout = timeout;
        this.consumer = consumer;
        this.serializationType = serializationType;
        this.async = async;
        this.oneway = oneway;
        this.registryService = registryService;
        this.enableResultCache = enableResultCache;
        this.resultCacheExpire = resultCacheExpire;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public String getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public boolean isEnableResultCache() {
        return enableResultCache;
    }

    public void setEnableResultCache(boolean enableResultCache) {
        this.enableResultCache = enableResultCache;
    }

    public int getResultCacheExpire() {
        return resultCacheExpire;
    }

    public void setResultCacheExpire(int resultCacheExpire) {
        this.resultCacheExpire = resultCacheExpire;
    }
}
