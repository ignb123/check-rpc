package io.check.rpc.test.scanner.consumer.service.impl;

import io.check.rpc.annotation.RpcReference;
import io.check.rpc.test.scanner.consumer.service.ConsumerBusinessService;
import io.check.rpc.test.scanner.service.DemoService;

public class ConsumerBusinessServiceImpl implements ConsumerBusinessService {
    @RpcReference(
            registryType = "zookeeper",
            registryAddress = "127.0.0.1:2181",
            version = "1.0.0", group = "check"
    )
    private DemoService demoService;
}
