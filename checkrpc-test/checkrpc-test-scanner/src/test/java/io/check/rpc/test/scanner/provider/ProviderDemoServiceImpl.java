package io.check.rpc.test.scanner.provider;

import io.check.rpc.annotation.RpcService;
import io.check.rpc.test.scanner.service.DemoService;

@RpcService(interfaceClass = DemoService.class,
        interfaceClassName = "io.check.rpc.test.scanner.service.DemoService",
        version = "1.0.0", group = "check")
public class ProviderDemoServiceImpl implements DemoService {
}
