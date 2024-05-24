package io.check.rpc.loadbalancer.robin.weight;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SPIClass
public class RobinWeightServiceLoadBalancer<T> implements ServiceLoadBalancer<T> {

    private final Logger logger = LoggerFactory.getLogger(RobinWeightServiceLoadBalancer.class);

    private volatile AtomicInteger atomicInteger = new AtomicInteger(0);
    @Override
    public T select(List<T> servers, int hashCode, String sourceIp) {
        logger.info("基于加权轮询算法的负载均衡策略...");
        if (servers == null || servers.isEmpty()){
            return null;
        }
        hashCode = Math.abs(hashCode);
        int count = hashCode%servers.size();
        if(count <= 0) {
            count = servers.size();
        }
        int index = atomicInteger.incrementAndGet();
        /**
         * 这里之所以是大于或者等于Integer的最大值减去10000，
         * 是为了避免在高并发环境下由于竞态条件问题导致AtomicInteger自增后的值超过Integer的最大值，
         * 从而发生范围越界的问题
         */
        if (index >= Integer.MAX_VALUE - 10000){
            atomicInteger.set(0);
        }
        return servers.get(index%count);
    }
}
