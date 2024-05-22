package io.check.rpc.provider;

import io.check.rpc.common.scanner.server.RpcServiceScanner;
import io.check.rpc.provider.common.server.base.BaseServer.BaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSingleServer extends BaseServer {

    private final Logger logger = LoggerFactory.getLogger(RpcSingleServer.class);

    public RpcSingleServer(String serverAddress, String scanPackage, String reflectType) {
        //调用父类构造方法
        super(serverAddress,reflectType);
        try {
            this.handlerMap = RpcServiceScanner
                    .doScannerWithRpcServiceAnnotationFilterAndRegistryService(scanPackage);
        }catch (Exception e){
            logger.error("RPC Server init error", e);
        }
    }
}
