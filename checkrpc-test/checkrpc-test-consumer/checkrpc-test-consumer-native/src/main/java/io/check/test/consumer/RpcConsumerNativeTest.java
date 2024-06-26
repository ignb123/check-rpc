package io.check.test.consumer;

import io.check.rpc.consumer.RpcClient;
import io.check.rpc.proxy.api.async.IAsyncObjectProxy;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.test.api.DemoService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcConsumerNativeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcConsumerNativeTest.class);

    public static void main(String[] args){
        RpcClient rpcClient = new RpcClient("127.0.0.1:2181", "zookeeper",
                "leastconnections","cglib",
                "1.0.0", "check", "protostuff",
                3000, false, false,3000, 6000,
                1000, 3,false, 10000,
                false,"127.0.0.1:27880",true,
                16,16,"print",true,2,
                "jdk","io", true, "counter", 100,
                1000);
        DemoService demoService = rpcClient.create(DemoService.class);
        String result = demoService.hello("check");
        LOGGER.info("返回的结果数据===>>> " + result);
        rpcClient.shutdown();
    }


    private RpcClient rpcClient;

    @Before
    public void initRpcClient(){
        rpcClient = new RpcClient("127.0.0.1:2181", "zookeeper",
                "leastconnections","cglib",
                "1.0.0", "check", "protostuff",
                3000, false, false,3000, 6000,
                1000, 3,false, 10000,
                false,"127.0.0.1:27880",true,
                16,16, "print",true,2,
                "jdk","io",true, "counter", 100, 1000);

    }

    @Test
    public void testInterfaceRpc(){
        DemoService demoService = rpcClient.create(DemoService.class);
        String result = demoService.hello("check");
        LOGGER.info("返回的结果数据===>>> " + result);
        rpcClient.shutdown();
        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testAsyncInterfaceRpc() throws Exception {
        IAsyncObjectProxy demoService = rpcClient.createAsync(DemoService.class);
        RPCFuture future = demoService.call("hello", "check");
        LOGGER.info("返回的结果数据===>>> " + future.get());
        rpcClient.shutdown();
    }

}
