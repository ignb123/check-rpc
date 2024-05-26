package io.check.rpc.registry.api;

import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.spi.annotation.SPI;

import java.io.IOException;
import java.util.List;

@SPI
public interface RegistryService {

    /** 服务注册
     * @param serviceMeta 服务元数据
     * @throws Exception 抛出异常
     */
    void register(ServiceMeta serviceMeta) throws Exception;

    /**
     * 服务取消注册
     * @param serviceMeta 服务元数据
     * @throws Exception 抛出异常
     */
    void unRegister(ServiceMeta serviceMeta) throws Exception;

    /**
     * 服务发现
     * @param serviceName 服务名称
     * @param invokerHashCode HashCode值
     * @return 服务元数据
     * @throws Exception 抛出异常
     */
    ServiceMeta discovery(String serviceName, int invokerHashCode, String sourceIp) throws Exception;

    /**
     * 服务销毁
     * @throws IOException 抛出异常
     */
    void destroy() throws IOException;

    /**
     * 默认初始化方法
     */
    default void init(RegistryConfig registryConfig) throws Exception {

    }

    ServiceMeta select(List<ServiceMeta> serviceMetaList, int invokerHashCode, String sourceIp);
}
