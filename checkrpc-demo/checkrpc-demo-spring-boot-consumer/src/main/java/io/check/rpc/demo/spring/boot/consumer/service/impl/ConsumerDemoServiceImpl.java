package io.check.rpc.demo.spring.boot.consumer.service.impl;

import io.check.rpc.annotation.RpcReference;
import io.check.rpc.demo.api.DemoService;
import io.check.rpc.demo.spring.boot.consumer.service.ConsumerDemoService;
import org.springframework.stereotype.Service;

@Service
public class ConsumerDemoServiceImpl implements ConsumerDemoService {
    @RpcReference(registryType = "zookeeper",
            registryAddress = "127.0.0.1:2181", loadBalanceType = "zkconsistenthash",
            version = "1.0.0", group = "check", serializationType = "protostuff",
            proxy = "cglib", timeout = 30000, async = false, oneway = false)
    private DemoService demoService;

    @Override
    public String hello(String name) {
        return demoService.hello(name);
    }
}
