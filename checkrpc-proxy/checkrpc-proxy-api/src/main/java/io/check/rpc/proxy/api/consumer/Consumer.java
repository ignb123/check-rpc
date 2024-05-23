package io.check.rpc.proxy.api.consumer;

import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.registry.api.RegistryService;

public interface Consumer {
    /**
     * 消费者发送 request 请求
     */
    RPCFuture sendRequest(RpcProtocol<RpcRequest> protocol, RegistryService registryService) throws Exception;
}
