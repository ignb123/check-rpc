package io.check.rpc.loadbalancer.sourceip.hash.weight;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.loadbalancer.base.BaseEnhancedServiceLoadBalancer;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

@SPIClass
public class SourceIpHashWeightServiceLoadBalancer extends BaseEnhancedServiceLoadBalancer {

    private final Logger logger = LoggerFactory.getLogger(SourceIpHashWeightServiceLoadBalancer.class);
    @Override
    public ServiceMeta select(List<ServiceMeta> servers, int hashCode, String ip) {
        logger.info("增强型基于源IP地址加权Hash的负载均衡策略...");
        servers = this.getWeightServiceMetaList(servers);
        if (servers == null || servers.isEmpty()){
            return null;
        }
        //传入的IP地址为空，则默认返回第一个服务实例m
        if (StringUtils.isEmpty(ip)){
            return servers.get(0);
        }
        int resultHashCode = Math.abs(ip.hashCode() + hashCode);
        return servers.get(resultHashCode % servers.size());
    }
}
