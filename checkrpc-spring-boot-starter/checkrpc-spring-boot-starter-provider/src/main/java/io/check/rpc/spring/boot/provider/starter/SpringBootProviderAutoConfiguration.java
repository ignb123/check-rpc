package io.check.rpc.spring.boot.provider.starter;

import io.check.rpc.provider.spring.RpcSpringServer;
import io.check.rpc.spring.boot.provider.config.SpringBootProviderConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class SpringBootProviderAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "checkrpc.check.provider")
    public SpringBootProviderConfig springBootProviderConfig(){
        return new SpringBootProviderConfig();
    }

    @Bean
    public RpcSpringServer rpcSpringServer(final SpringBootProviderConfig springBootProviderConfig){
        return new RpcSpringServer(springBootProviderConfig.getServerAddress(),
                springBootProviderConfig.getRegistryAddress(),
                springBootProviderConfig.getRegistryType(),
                springBootProviderConfig.getRegistryLoadBalanceType(),
                springBootProviderConfig.getReflectType(),
                springBootProviderConfig.getHeartbeatInterval(),
                springBootProviderConfig.getScanNotActiveChannelInterval(),
                springBootProviderConfig.isEnableResultCache(),
                springBootProviderConfig.getResultCacheExpire(),
                springBootProviderConfig.getCorePoolSize(),
                springBootProviderConfig.getMaximumPoolSize(),
                springBootProviderConfig.getFlowType(),
                springBootProviderConfig.getMaxConnections(),
                springBootProviderConfig.getDisuseStrategyType());
    }
}
