package io.check.rpc.codec;

import io.check.rpc.serialization.jdk.JdkSerialization;
import io.check.rpc.serialization.api.Serialization;

public interface RpcCodec {

    default Serialization getJdkSerialization(){
        return new JdkSerialization();
    }
}
