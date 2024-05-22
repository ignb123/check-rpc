package io.check.rpc.test.provider.service.impl;

import io.check.rpc.annotation.RpcService;
import io.check.rpc.test.provider.service.DemoService;

@RpcService(interfaceClass = DemoService.class,
        interfaceClassName = "io.check.rpc.test.scanner.service.DemoService",
        version = "1.0.0",
        group = "check")
public class ProviderDemoServiceImpl implements DemoService {
    @Override
    public String hello(String name) {
        return name;
    }
}
