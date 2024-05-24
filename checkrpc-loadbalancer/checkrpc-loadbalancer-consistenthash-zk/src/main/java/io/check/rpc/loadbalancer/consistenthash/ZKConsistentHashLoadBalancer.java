package io.check.rpc.loadbalancer.consistenthash;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.spi.annotation.SPIClass;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SPIClass
public class ZKConsistentHashLoadBalancer implements ServiceLoadBalancer<ServiceMeta> {

    private final static int VIRTUAL_NODE_SIZE = 10;

    private final static String VIRTUAL_NODE_SPLIT = "#";

    private final Logger logger = LoggerFactory.getLogger(ZKConsistentHashLoadBalancer.class);

    @Override
    public ServiceMeta select(List<ServiceMeta> servers, int hashCode, String sourceIp) {
        logger.info("基于Zookeeper的一致性Hash算法的负载均衡策略...");
        TreeMap<Integer,ServiceMeta> ring = makeConsistentHashRing(servers);
        return allocateNode(ring, hashCode);
    }

    private ServiceMeta allocateNode(TreeMap<Integer, ServiceMeta> ring, int hashCode) {
        /**
         * 查找并获取大于等于给定hashCode的映射条目的入口。
         * 该方法用于在服务环（ring）中定位特定服务实例的入口，以便进行服务调用或负载均衡等操作。
         */
        Map.Entry<Integer, ServiceMeta> entry = ring.ceilingEntry(hashCode);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        if (entry == null){
            throw new RuntimeException("not discover useful service, please register service in registry center.");
        }
        return entry.getValue();
    }

    private TreeMap<Integer, ServiceMeta> makeConsistentHashRing(List<ServiceMeta> servers) {
        TreeMap<Integer, ServiceMeta> ring = new TreeMap<>();
        for(ServiceMeta instance : servers){
            for (int i = 0; i < VIRTUAL_NODE_SIZE; i++) {
                ring.put((buildServiceInstanceKey(instance) + VIRTUAL_NODE_SPLIT + i).hashCode(), instance);
            }
        }
        return ring;
    }

    private String buildServiceInstanceKey(ServiceMeta instance) {
        return String.join(":",instance.getServiceAddr(),
                String.valueOf(instance.getServicePort()));
    }
}
