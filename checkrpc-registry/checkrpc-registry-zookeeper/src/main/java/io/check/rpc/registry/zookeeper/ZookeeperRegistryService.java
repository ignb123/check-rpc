package io.check.rpc.registry.zookeeper;

import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.loadbalancer.random.RandomServiceLoadBalancer;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ZookeeperRegistryService implements RegistryService {

    /**
     * 连接重试的间隔时间
     */
    public static final int BASE_SLEEP_TIME_MS = 1000;

    /**
     * 连接重试的最大重试次数
     */
    public static final int MAX_RETRIES = 3;

    /**
     * 服务注册到Zookeeper的根路径
     */
    public static final String ZK_BASE_PATH = "/check_rpc";

    /**
     * 服务注册与发现的ServiceDiscovery类实例
     */
    private ServiceDiscovery<ServiceMeta> serviceDiscovery;

    /**
     * 服务负载均衡类实例
     */
    private ServiceLoadBalancer<ServiceInstance<ServiceMeta>> serviceLoadBalancer;

    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {
        ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
                .<ServiceMeta>builder()
                .name(RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName()
                        ,serviceMeta.getServiceVersion(),serviceMeta.getServiceGroup()))
                .address(serviceMeta.getServiceAddr())
                .port(serviceMeta.getServicePort())
                .payload(serviceMeta) // 设置服务实例的负载信息
                .build();
        serviceDiscovery.registerService(serviceInstance);
    }

    @Override
    public void unRegister(ServiceMeta serviceMeta) throws Exception {
        ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
                .<ServiceMeta>builder()
                .name(serviceMeta.getServiceName())
                .address(serviceMeta.getServiceAddr())
                .port(serviceMeta.getServicePort())
                .payload(serviceMeta) // 设置服务实例的负载信息
                .build();
        serviceDiscovery.unregisterService(serviceInstance);
    }

    @Override
    public ServiceMeta discovery(String serviceName, int invokerHashCode, String sourceIp) throws Exception {
        Collection<ServiceInstance<ServiceMeta>> serviceInstances =
                serviceDiscovery.queryForInstances(serviceName);
        ServiceInstance<ServiceMeta> instance =
                serviceLoadBalancer.select((List<ServiceInstance<ServiceMeta>>) serviceInstances, invokerHashCode,sourceIp);
        if (instance != null) {
            return instance.getPayload();
        }
        return null;
    }

    @Override
    public void destroy() throws IOException {
        serviceDiscovery.close();
    }

    @Override
    public void init(RegistryConfig registryConfig) throws Exception{


        // 创建 CuratorFramework 客户端实例，并配置重试策略
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(registryConfig.getRegistryAddr(),
                        new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_RETRIES));
        client.start(); // 启动客户端

        // 创建 ServiceMeta 的序列化器
        JsonInstanceSerializer<ServiceMeta> serializer = new JsonInstanceSerializer<>(ServiceMeta.class);

        // 使用上述客户端、序列化器和基础路径构建 ServiceDiscovery 实例
        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceMeta.class)
                .client(client)
                .serializer(serializer)
                .basePath(ZK_BASE_PATH)
                .build();

        this.serviceDiscovery.start(); // 启动服务发现
        this.serviceLoadBalancer = ExtensionLoader.getExtension(ServiceLoadBalancer.class,
                registryConfig.getRegistryLoadBalanceType());
    }
}
