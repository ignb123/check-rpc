package io.check.rpc.loadbalancer.api;

import io.binghe.rpc.constants.RpcConstants;
import io.check.rpc.spi.annotation.SPI;

import java.util.List;

@SPI(RpcConstants.SERVICE_LOAD_BALANCER_RANDOM)
public interface ServiceLoadBalancer<T> {
    /**
     * 以负载均衡的方式选取一个服务节点
     * @param servers 服务列表
     * @param hashCode Hash值
     * @return 可用的服务节点
     */
    T select(List<T> servers, int hashCode, String sourceIp);
}
