package io.check.rpc.test.provider.single;

import io.check.rpc.provider.RpcSingleServer;
import org.junit.Test;

public class RpcSingleServerTest {
    @Test
    public void startRpcSingleServer(){
        RpcSingleServer singleServer = new RpcSingleServer("127.0.0.1:27880",
                "127.0.0.1:2181",
                "zookeeper","zkconsistenthash", "io.check.rpc.test",
                "javassist",30000, 60000,
                true, 30000,16,16,"print",
                16, "LRU",true, 1024,true,
                "count", 2,3000,"fallback");
        singleServer.startNettyServer();
    }
}
