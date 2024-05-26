package io.check.rpc.flow.processor;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.spi.annotation.SPI;

@SPI(RpcConstants.FLOW_POST_PROCESSOR_PRINT)
public interface FlowPostProcessor {
    /**
     * 流控分析后置处理器方法
     */
    void postRpcHeaderProcessor(RpcHeader rpcHeader);
}
