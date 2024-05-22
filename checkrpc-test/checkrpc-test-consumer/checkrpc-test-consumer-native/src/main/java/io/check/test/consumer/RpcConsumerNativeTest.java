package io.check.test.consumer;

import io.check.rpc.consumer.RpcClient;
import io.check.rpc.test.api.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcConsumerNativeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcConsumerNativeTest.class);
    public static void main(String[] args) throws Exception {
        RpcClient rpcClient = new RpcClient("1.0.0", "check", "jdk", 3000, false, false);
        DemoService demoService = rpcClient.create(DemoService.class);
        String result = demoService.hello("check");
        LOGGER.info("返回的结果数据===>>> " + result);
        rpcClient.shutdown();

    }
}
