package io.check.rpc.spring.boot.consumer.config;

public final class SpringBootConsumerConfig {

    /**
     * 注册地址
     */
    private String registryAddress;

    /**
     * 注册类型
     */
    private String registryType;

    /**
     * 负载均衡类型
     */
    private String loadBalanceType;

    /**
     * 代理
     */
    private String proxy;

    /**
     * 版本号
     */
    private String version;

    /**
     * 分组
     */
    private String group;

    /**
     * 序列化类型
     */
    private String serializationType;

    /**
     * 超时时间
     */
    private int timeout;

    /**
     * 是否异步
     */
    private boolean async;

    /**
     * 是否单向调用
     */
    private boolean oneway;

    /**
     * 心跳检测
     */
    private int heartbeatInterval;

    /**
     * 扫描并移除不活跃连接的时间间隔
     */
    private int scanNotActiveChannelInterval;

    //重试间隔时间
    private int retryInterval = 1000;

    //重试次数
    private int retryTimes = 3;

    private boolean enableResultCache;

    private int resultCacheExpire;

    private boolean enableDirectServer;

    private String directServerUrl;

    //是否开启延迟连接
    private boolean enableDelayConnection;

    private int corePoolSize;

    private int maximumPoolSize;

    private String flowType;

    private boolean enableBuffer;

    private int bufferSize;

    private String reflectType;

    private String fallbackClassName;

    public SpringBootConsumerConfig() {
    }

    public SpringBootConsumerConfig(final String registryAddress, final String registryType, final String loadBalanceType,
                                    final String proxy, final String version, final String group, final String serializationType,
                                    final int timeout, final boolean async, final boolean oneway, final int heartbeatInterval,
                                    final int scanNotActiveChannelInterval, final int retryInterval, final int retryTimes,
                                    final boolean enableResultCache, final int resultCacheExpire,
                                    final boolean enableDirectServer, final String directServerUrl,
                                    final boolean enableDelayConnection,
                                    final int corePoolSize, final int maximumPoolSize, final String flowType,
                                    boolean enableBuffer, int bufferSize, String reflectType, String fallbackClassName) {
        this.registryAddress = registryAddress;
        this.registryType = registryType;
        this.loadBalanceType = loadBalanceType;
        this.proxy = proxy;
        this.version = version;
        this.group = group;
        this.serializationType = serializationType;
        this.timeout = timeout;
        this.async = async;
        this.oneway = oneway;
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        this.retryInterval = retryInterval;
        this.retryTimes = retryTimes;
        this.enableResultCache = enableResultCache;
        this.resultCacheExpire = resultCacheExpire;
        this.enableDirectServer = enableDirectServer;
        this.directServerUrl = directServerUrl;
        this.enableDelayConnection = enableDelayConnection;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.flowType = flowType;
        this.enableBuffer = enableBuffer;
        this.bufferSize = bufferSize;
        this.reflectType = reflectType;
        this.fallbackClassName = fallbackClassName;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public String getLoadBalanceType() {
        return loadBalanceType;
    }

    public void setLoadBalanceType(String loadBalanceType) {
        this.loadBalanceType = loadBalanceType;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getScanNotActiveChannelInterval() {
        return scanNotActiveChannelInterval;
    }

    public void setScanNotActiveChannelInterval(int scanNotActiveChannelInterval) {
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
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

    public boolean isEnableDirectServer() {
        return enableDirectServer;
    }

    public void setEnableDirectServer(boolean enableDirectServer) {
        this.enableDirectServer = enableDirectServer;
    }

    public String getDirectServerUrl() {
        return directServerUrl;
    }

    public void setDirectServerUrl(String directServerUrl) {
        this.directServerUrl = directServerUrl;
    }

    public boolean isEnableDelayConnection() {
        return enableDelayConnection;
    }

    public void setEnableDelayConnection(boolean enableDelayConnection) {
        this.enableDelayConnection = enableDelayConnection;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public boolean isEnableBuffer() {
        return enableBuffer;
    }

    public void setEnableBuffer(boolean enableBuffer) {
        this.enableBuffer = enableBuffer;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getReflectType() {
        return reflectType;
    }

    public void setReflectType(String reflectType) {
        this.reflectType = reflectType;
    }

    public String getFallbackClassName() {
        return fallbackClassName;
    }

    public void setFallbackClassName(String fallbackClassName) {
        this.fallbackClassName = fallbackClassName;
    }
}
