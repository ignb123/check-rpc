package io.check.rpc.test.provider.single;

import io.check.rpc.provider.RpcSingleServer;
import org.junit.Test;

public class RpcSingleServerTest {
    @Test
    public void startRpcSingleServer(){
        RpcSingleServer singleServer = new RpcSingleServer("127.0.0.1:27880","127.0.0.1:2181",
                "zookeeper", "io.check.rpc.test","javassist");
        singleServer.startNettyServer();
    }
}
