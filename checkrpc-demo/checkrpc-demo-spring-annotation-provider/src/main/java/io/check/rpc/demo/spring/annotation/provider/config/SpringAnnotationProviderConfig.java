package io.check.rpc.demo.spring.annotation.provider.config;


import io.check.rpc.provider.spring.RpcSpringServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


/**
 * @author check
 * @version 1.0.0
 * @description 基于注解的配置类
 */
@Configuration
@ComponentScan(value = {"io.check.rpc.demo"})
@PropertySource(value = {"classpath:rpc.properties"})
public class SpringAnnotationProviderConfig {

    @Value("${registry.address}")
    private String registryAddress;

    @Value("${registry.type}")
    private String registryType;

    @Value("${registry.loadbalancer.type}")
    private String registryLoadbalanceType;

    @Value("${server.address}")
    private String serverAddress;

    @Value("${reflect.type}")
    private String reflectType;

    @Value("${server.heartbeatInterval}")
    private int heartbeatInterval;

    @Value("${server.scanNotActiveChannelInterval}")
    private int scanNotActiveChannelInterval;
    @Value("${maximumPoolSizeserver.enableResultCache}")
    private boolean enableResultCache;
    @Value("${server.resultCacheExpire}")
    private int resultCacheExpire;
    @Value("${server.corePoolSize}")
    private int corePoolSize;
    @Value("${server.maximumPoolSize}")
    private int maximumPoolSize;

    @Value("${server.flowType}")
    private String flowType;

    @Value("${server.maxConnections}")
    private int maxConnections;

    @Value("${server.disuseStrategyType}")
    private String disuseStrategyType;

    @Value("${server.enableBuffer}")
    private boolean enableBuffer;

    @Value("${server.bufferSize}")
    private int bufferSize;

    @Value("${server.enableRateLimiter}")
    private boolean enableRateLimiter;

    @Value("${server.rateLimiterType}")
    private String rateLimiterType;

    @Value("${server.permits}")
    private int permits;

    @Value("${server.milliSeconds}")
    private int milliSeconds;

    @Value("${server.rateLimiterFailStrategy}")
    private String rateLimiterFailStrategy;

    @Value("${server.enableFusing}")
    private boolean enableFusing;

    @Value("${server.fusingType}")
    private String fusingType;

    @Value("${server.totalFailure}")
    private double totalFailure;

    @Value("${server.fusingMilliSeconds}")
    private int fusingMilliSeconds;


    @Bean
    public RpcSpringServer rpcSpringServer(){
        return new RpcSpringServer(serverAddress, registryAddress, registryType, registryLoadbalanceType, reflectType,
                heartbeatInterval, scanNotActiveChannelInterval, enableResultCache, resultCacheExpire, corePoolSize,
                maximumPoolSize,flowType,maxConnections, disuseStrategyType,enableBuffer,bufferSize,enableRateLimiter,
                rateLimiterType, permits, milliSeconds, rateLimiterFailStrategy,  enableFusing, fusingType, totalFailure,
                fusingMilliSeconds);
    }


}
