package io.check.demo.consumer;

import io.check.rpc.consumer.RpcClient;
import io.check.rpc.demo.api.DemoService;
import io.check.rpc.proxy.api.async.IAsyncObjectProxy;
import io.check.rpc.proxy.api.future.RPCFuture;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerNativeDemoService {


    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerNativeDemoService.class);

    private RpcClient rpcClient;

    @Before
    public void initRpcClient(){
        rpcClient = new RpcClient("127.0.0.1:2181", "zookeeper",
                "zkconsistenthash","cglib",
                "1.0.0", "check", "protostuff",
                3000, false, false,30000, 60000,
                1000, 3, false, 10000,
                true,"127.0.0.1:27880",
                true, 16, 16,"print",
                false,2,"jdk",
                "io.check.demo.consumer.hello.FallbackDemoServiceImpl",
                false, "guava", 1, 5000,"fallback",
                true, "percent", 10, 10000,"print");
    }

    @Test
    public void testInterfaceRpc() throws InterruptedException {
        DemoService demoService = rpcClient.create(DemoService.class);
        for(int i = 0; i < 5; i++){
            String result = demoService.hello("check");
            LOGGER.info("返回的结果数据===>>> " + result);
        }
        //rpcClient.shutdown();
        while (true){
            Thread.sleep(1000);
        }
    }

    @Test
    public void testInterfaceRpc1() throws InterruptedException {
        DemoService demoService = rpcClient.create(DemoService.class);
        String result = demoService.hello("check");
        LOGGER.info("返回的结果数据===>>> " + result);
        while (true){
            Thread.sleep(1000);
        }
    }

    @Test
    public void testAsyncInterfaceRpc() throws Exception {
        IAsyncObjectProxy demoService = rpcClient.createAsync(DemoService.class);
        RPCFuture future = demoService.call("hello", "check");
        LOGGER.info("返回的结果数据===>>> " + future.get());
        while (true){
            Thread.sleep(1000);
        }
    }

}
