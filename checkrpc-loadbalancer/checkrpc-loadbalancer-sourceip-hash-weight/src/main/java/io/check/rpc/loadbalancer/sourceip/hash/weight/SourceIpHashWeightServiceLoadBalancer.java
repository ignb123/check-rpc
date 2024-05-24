package io.check.rpc.loadbalancer.sourceip.hash.weight;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

@SPIClass
public class SourceIpHashWeightServiceLoadBalancer<T> implements ServiceLoadBalancer<T> {

    private final Logger logger = LoggerFactory.getLogger(SourceIpHashWeightServiceLoadBalancer.class);
    @Override
    public T select(List<T> servers, int hashCode, String sourceIp) {
        logger.info("基于源IP地址加权Hash的负载均衡策略...");
        if (servers == null || servers.isEmpty()){
            return null;
        }
        //传入的IP地址为空，则默认返回第一个服务实例m
        if (StringUtils.isEmpty(sourceIp)){
            return servers.get(0);
        }

        int count = Math.abs(hashCode) % servers.size();
        if (count == 0){
            count = servers.size();
        }
        int resultHashCode = Math.abs(sourceIp.hashCode() + hashCode);
        return servers.get(resultHashCode % count);
    }
}
