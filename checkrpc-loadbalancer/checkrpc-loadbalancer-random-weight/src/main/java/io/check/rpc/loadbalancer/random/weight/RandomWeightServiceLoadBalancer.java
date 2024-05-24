package io.check.rpc.loadbalancer.random.weight;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.loadbalancer.base.BaseEnhancedServiceLoadBalancer;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@SPIClass
public class RandomWeightServiceLoadBalancer<T> extends BaseEnhancedServiceLoadBalancer{

    private final Logger logger = LoggerFactory.getLogger(RandomWeightServiceLoadBalancer.class);

    /**
     * 基于加权随机算法的负载均衡选择方法。
     * 该方法通过给定的服务器列表和哈希码，选择一个服务器进行负载。
     *
     * @param servers 服务器列表，类型为泛型T的列表。该列表不应为null且不能为空。
     * @param hashCode 一个整数哈希码，用于计算选择服务器的权重。
     * @return 返回从服务器列表中根据加权随机算法选择出的一个服务器实例。如果输入列表为空或null，则返回null。
     */
    @Override
    public ServiceMeta select(List<ServiceMeta> servers, int hashCode, String sourceIp) {
        logger.info("基于增强型加权随机算法的负载均衡策略...");
        servers = this.getWeightServiceMetaList(servers);
        if (servers == null || servers.isEmpty()){
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(servers.size());
        return servers.get(index);
    }
}
