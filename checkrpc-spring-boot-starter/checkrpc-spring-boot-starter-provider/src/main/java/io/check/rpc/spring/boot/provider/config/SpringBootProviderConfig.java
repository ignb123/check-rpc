package io.check.rpc.spring.boot.provider.config;

public final class SpringBootProviderConfig {

    /**
     * 服务地址
     */
    private String serverAddress;

    /**
     * 注册中心地址
     */
    private String registryAddress;

    /**
     * 注册类型
     */
    private String registryType;

    /**
     * 负载均衡类型
     */
    private String registryLoadBalanceType;

    /**
     * 反射类型
     */
    private String reflectType;

    /**
     * 心跳时间间隔
     */
    private int heartbeatInterval;

    /**
     * 扫描并清理不活跃连接的时间间隔
     */
    private int scanNotActiveChannelInterval;

    /**
     * 是否开启结果缓存
     */
    private boolean enableResultCache;

    /**
     * 结果缓存的时长
     */
    private int resultCacheExpire;

    private int corePoolSize;

    private int maximumPoolSize;

    private String flowType;

    private int maxConnections;
    private String disuseStrategyType;

    private boolean enableBuffer;
    private int bufferSize;

    /**
     * 是否开启限流
     */
    private boolean enableRateLimiter;
    /**
     * 限流类型
     */
    private String rateLimiterType;
    /**
     * 在milliSeconds毫秒内最多能够通过的请求个数
     */
    private int permits;
    /**
     * 毫秒数
     */
    private int milliSeconds;

    /**
     * 当限流失败时的处理策略
     */
    private String rateLimiterFailStrategy;


    public SpringBootProviderConfig() {
    }

    public SpringBootProviderConfig(final String serverAddress, final String serverRegistryAddress, final String registryAddress,
                                    final String registryType, final String registryLoadBalanceType, final String reflectType,
                                    final int heartbeatInterval, int scanNotActiveChannelInterval, final boolean enableResultCache,
                                    final int resultCacheExpire, final int corePoolSize, final int maximumPoolSize, String flowType,
                                    final int maxConnections, final String disuseStrategyType, final boolean enableBuffer, final int bufferSize,
                                    final boolean enableRateLimiter, final String rateLimiterType, final int permits, final int milliSeconds,
                                    final String rateLimiterFailStrategy) {
        this.serverAddress = serverAddress;
        this.registryAddress = registryAddress;
        this.registryType = registryType;
        this.registryLoadBalanceType = registryLoadBalanceType;
        this.reflectType = reflectType;
        if (heartbeatInterval > 0){
            this.heartbeatInterval = heartbeatInterval;
        }
        this.scanNotActiveChannelInterval = scanNotActiveChannelInterval;
        this.enableResultCache = enableResultCache;
        this.resultCacheExpire = resultCacheExpire;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.flowType = flowType;
        this.maxConnections = maxConnections;
        this.disuseStrategyType = disuseStrategyType;
        this.enableBuffer = enableBuffer;
        this.bufferSize = bufferSize;
        this.enableRateLimiter = enableRateLimiter;
        this.rateLimiterType = rateLimiterType;
        this.permits = permits;
        this.milliSeconds = milliSeconds;
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;

    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
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

    public String getRegistryLoadBalanceType() {
        return registryLoadBalanceType;
    }

    public void setRegistryLoadBalanceType(String registryLoadBalanceType) {
        this.registryLoadBalanceType = registryLoadBalanceType;
    }

    public String getReflectType() {
        return reflectType;
    }

    public void setReflectType(String reflectType) {
        this.reflectType = reflectType;
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

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getDisuseStrategyType() {
        return disuseStrategyType;
    }

    public void setDisuseStrategyType(String disuseStrategyType) {
        this.disuseStrategyType = disuseStrategyType;
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

    public boolean isEnableRateLimiter() {
        return enableRateLimiter;
    }

    public void setEnableRateLimiter(boolean enableRateLimiter) {
        this.enableRateLimiter = enableRateLimiter;
    }

    public String getRateLimiterType() {
        return rateLimiterType;
    }

    public void setRateLimiterType(String rateLimiterType) {
        this.rateLimiterType = rateLimiterType;
    }

    public int getPermits() {
        return permits;
    }

    public void setPermits(int permits) {
        this.permits = permits;
    }

    public int getMilliSeconds() {
        return milliSeconds;
    }

    public void setMilliSeconds(int milliSeconds) {
        this.milliSeconds = milliSeconds;
    }

    public String getRateLimiterFailStrategy() {
        return rateLimiterFailStrategy;
    }

    public void setRateLimiterFailStrategy(String rateLimiterFailStrategy) {
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;
    }
}
