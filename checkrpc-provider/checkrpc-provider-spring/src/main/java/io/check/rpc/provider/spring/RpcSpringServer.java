package io.check.rpc.provider.spring;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.annotation.RpcService;
import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.provider.common.server.base.BaseServer;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

public class RpcSpringServer extends BaseServer implements ApplicationContextAware, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(RpcSpringServer.class);

    public RpcSpringServer(String serverAddress, String registryAddress, String registryType, String registryLoadBalanceType,
                           String reflectType, int heartbeatInterval, int scanNotActiveChannelInterval,
                           boolean enableResultCache, int resultCacheExpire, int corePoolSize, int maximumPoolSize) {
        super(serverAddress, registryAddress, registryType, registryLoadBalanceType, reflectType,
                heartbeatInterval, scanNotActiveChannelInterval, enableResultCache,
                resultCacheExpire, corePoolSize, maximumPoolSize);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.startNettyServer();
    }

    /**
     * @RpcService注解的扫描操作，并将服务提供者的元数据信息注册到注册中心
     * @param ctx
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                ServiceMeta serviceMeta = new ServiceMeta(this.getServiceName(rpcService), rpcService.version(),
                        rpcService.group(), host, port, getWeight(rpcService.weight()));
                handlerMap.put(
                        RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName(), serviceMeta.getServiceVersion(), serviceMeta.getServiceGroup()),
                        serviceBean);
                try {
                    registryService.register(serviceMeta);
                }catch (Exception e){
                    logger.error("rpc server init spring exception:{}", e);
                }
            }
        }
    }

    private int getWeight(int weight) {
        if (weight < RpcConstants.SERVICE_WEIGHT_MIN) {
            weight = RpcConstants.SERVICE_WEIGHT_MIN;
        }
        if (weight > RpcConstants.SERVICE_WEIGHT_MAX) {
            weight = RpcConstants.SERVICE_WEIGHT_MAX;
        }
        return weight;
    }

    /**
     * 获取serviceName
     */
    private String getServiceName(RpcService rpcService) {
        //优先使用interfaceClass
        Class<?> clazz = rpcService.interfaceClass();
        if (clazz == void.class) {
            return rpcService.interfaceClassName();
        }
        String serviceName = clazz.getName();
        if (serviceName == null || serviceName.trim().isEmpty()) {
            serviceName = rpcService.interfaceClassName();
        }
        return serviceName;
    }

}
