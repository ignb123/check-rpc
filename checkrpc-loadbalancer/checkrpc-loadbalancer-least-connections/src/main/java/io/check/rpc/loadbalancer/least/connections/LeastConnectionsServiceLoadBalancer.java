package io.check.rpc.loadbalancer.least.connections;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.loadbalancer.context.ConnectionsContext;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 首先遍历元数据列表，从元数据列表中获取连接数为空的元数据并返回。
 * 如果没有获取到连接数为空的元数据，则从元数据列表中获取连接数最少的元数据并返回
 */
@SPIClass
public class LeastConnectionsServiceLoadBalancer implements ServiceLoadBalancer<ServiceMeta> {

    private final Logger logger = LoggerFactory.getLogger(LeastConnectionsServiceLoadBalancer.class);

    @Override
    public ServiceMeta select(List<ServiceMeta> servers, int hashCode, String sourceIp) {
        logger.info("基于最少连接数的负载均衡策略...");
        if (servers == null || servers.isEmpty()){
            return null;
        }
        ServiceMeta serviceMeta = this.getNullServiceMeta(servers);
        if (serviceMeta == null){
            serviceMeta = this.getServiceMeta(servers);
        }
        return serviceMeta;
    }

    //获取服务元数据列表中连接数为空的元数据，说明没有连接
    private ServiceMeta getNullServiceMeta(List<ServiceMeta> servers) {
        for (int i = 0; i < servers.size(); i++){
            ServiceMeta serviceMeta = servers.get(i);
            if (ConnectionsContext.getValue(serviceMeta) == null){
                return serviceMeta;
            }
        }
        return null;
    }

    private ServiceMeta getServiceMeta(List<ServiceMeta> servers) {
        ServiceMeta serviceMeta = servers.get(0);
        Integer serviceMetaCount = ConnectionsContext.getValue(serviceMeta);
        for (int i = 1; i < servers.size(); i++){
            ServiceMeta meta = servers.get(i);
            Integer metaCount = ConnectionsContext.getValue(meta);
            if (serviceMetaCount > metaCount){
                serviceMetaCount = metaCount;
                serviceMeta = meta;
            }
        }
        return serviceMeta;
    }
}
