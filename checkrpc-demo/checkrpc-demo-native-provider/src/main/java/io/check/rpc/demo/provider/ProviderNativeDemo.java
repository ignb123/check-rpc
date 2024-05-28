package io.check.rpc.demo.provider;

import io.check.rpc.provider.RpcSingleServer;
import org.junit.Test;

public class ProviderNativeDemo {

    @Test
    public void startRpcSingleServer(){
        RpcSingleServer singleServer = new RpcSingleServer(
                "127.0.0.1:27880", "127.0.0.1:2181",
                "zookeeper", "random","io.check.rpc.demo",
                "jdk",30000, 60000,
                true, 10000,16,16,"print",
                1,"lru",true,2,true,
                "guava", 1, 5000,"fallback");
        singleServer.startNettyServer();
    }

}
