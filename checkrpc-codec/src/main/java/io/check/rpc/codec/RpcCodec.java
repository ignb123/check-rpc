package io.check.rpc.codec;

import io.check.rpc.flow.processor.FlowPostProcessor;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.serialization.api.Serialization;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.check.rpc.threadpool.FlowPostProcessorThreadPool;


public interface RpcCodec {

    /**
     * 根据serializationType通过SPI获取序列化句柄
     * @param serializationType 序列化方式
     * @return Serialization对象
     */
    default Serialization getJdkSerialization(String serializationType){
        return ExtensionLoader.getExtension(Serialization.class, serializationType);
    }

    default void postFlowProcessor(FlowPostProcessor postProcessor, RpcHeader header){
        //异步调用流控分析后置处理器
        FlowPostProcessorThreadPool.submit(() -> {
            postProcessor.postRpcHeaderProcessor(header);
        });
    }
}
