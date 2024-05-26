package io.check.rpc.demo.spring.boot.consumer.service.impl;

import io.check.rpc.annotation.RpcReference;
import io.check.rpc.demo.spring.boot.consumer.service.ConsumerDemoService;
import org.springframework.stereotype.Service;

@Service
public class ConsumerDemoServiceImpl implements ConsumerDemoService {

    @RpcReference
    private ConsumerDemoService demoService;

    @Override
    public String hello(String name) {
        return demoService.hello(name);
    }
}
