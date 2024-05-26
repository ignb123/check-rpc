package io.check.rpc.registry.etcd;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.etcd.jetcd.*;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.spi.annotation.SPIClass;

import io.netty.util.CharsetUtil;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SPIClass
public class EtcdRegistryService implements RegistryService {

    private final Logger logger = LoggerFactory.getLogger(EtcdRegistryService.class);

    private Client etcdClient;

    private KV kvClient;

    /**
     * 服务负载均衡类实例
     */
    private ServiceLoadBalancer<ServiceMeta> serviceLoadBalancer;

    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {
        logger.info("基于ETCD的注册中心...");
        final String serviceKey = RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName(),serviceMeta.getServiceVersion(),serviceMeta.getServiceGroup());
        kvClient.put(ByteSequence.from(serviceKey, CharsetUtil.UTF_8),
                ByteSequence.from(JSONObject.toJSONString(serviceMeta), CharsetUtil.UTF_8));

    }

    @Override
    public void unRegister(ServiceMeta serviceMeta) throws Exception {
        final String serviceKey = RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName(),serviceMeta.getServiceVersion(),serviceMeta.getServiceGroup());
        kvClient.delete(ByteSequence.from(serviceKey, CharsetUtil.UTF_8));
    }

    @Override
    public ServiceMeta discovery(String serviceName, int invokerHashCode, String sourceIp) throws Exception {
        List<KeyValue> kvs = kvClient.get(ByteSequence.from(serviceName, CharsetUtil.UTF_8)).get().getKvs();
        List<ServiceMeta> serviceMetaInfoList = kvs.stream()
                .map(kv -> {
                    ServiceMeta serviceMeta = JSON.parseObject(kv.getValue().getBytes(), ServiceMeta.class);
                    return serviceMeta;
                })
                .collect(Collectors.toList());
        return serviceLoadBalancer.select(serviceMetaInfoList, invokerHashCode, sourceIp);
    }

    @Override
    public void destroy() throws IOException {
        kvClient.close();
        etcdClient.close();
    }

    @Override
    public void init(RegistryConfig registryConfig) throws Exception{
        etcdClient = Client.builder()
                .endpoints(registryConfig.getRegistryAddr())
                .build();
        kvClient = etcdClient.getKVClient();
        this.serviceLoadBalancer = ExtensionLoader
                .getExtension(ServiceLoadBalancer.class, registryConfig.getRegistryLoadBalanceType());
    }

    @Override
    public ServiceMeta select(List<ServiceMeta> serviceMetaList, int invokerHashCode, String sourceIp) {
        return this.serviceLoadBalancer.select(serviceMetaList, invokerHashCode, sourceIp);
    }

    /**
     * 能够通过服务发现机制查询并获取所有服务的元数据信息。
     *
     * @return 返回一个包含所有服务元数据的列表。如果查询不到任何服务，则返回空列表。
     * @throws Exception 如果查询过程中发生任何异常，则抛出。
     */
    @Override
    public List<ServiceMeta> discoveryAll() throws Exception {
        return null;
    }

}
