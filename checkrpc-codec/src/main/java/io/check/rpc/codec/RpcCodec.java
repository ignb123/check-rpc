package io.check.rpc.codec;

import io.check.rpc.serialization.api.Serialization;
import io.check.rpc.spi.loader.ExtensionLoader;


public interface RpcCodec {

    /**
     * 根据serializationType通过SPI获取序列化句柄
     * @param serializationType 序列化方式
     * @return Serialization对象
     */
    default Serialization getJdkSerialization(String serializationType){
        return ExtensionLoader.getExtension(Serialization.class, serializationType);
    }
}
