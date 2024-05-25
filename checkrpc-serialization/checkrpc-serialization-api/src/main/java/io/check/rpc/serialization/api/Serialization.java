package io.check.rpc.serialization.api;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.spi.annotation.SPI;

@SPI(RpcConstants.SERIALIZATION_JDK)
public interface Serialization {

    /**
     * 序列化
     */
    <T> byte[] serialize(T obj);

    /**
     * 反序列化
     */
    <T> T deserialize(byte[] data, Class<T> cls);
}
