package io.check.rpc.test.provider.service.impl;

import io.check.rpc.annotation.RpcService;


import io.check.rpc.test.api.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(interfaceClass = DemoService.class,
        interfaceClassName = "io.check.rpc.test.api.DemoService",
        version = "1.0.0",
        group = "check")
public class ProviderDemoServiceImpl implements DemoService {
    private final Logger logger = LoggerFactory.getLogger(ProviderDemoServiceImpl.class);
    @Override
    public String hello(String name) {
        logger.info("调用hello方法传入的参数为===>>>{}", name);
        return "hello " + name;
    }

}
