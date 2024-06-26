package io.check.rpc.protocol.header;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.common.id.IdFactory;
import io.check.rpc.protocol.enumeration.RpcType;

public class RpcHeaderFactory {

    public static RpcHeader getRequestHeader(String serializationType) {
        RpcHeader rpcHeader = new RpcHeader();
        rpcHeader.setRequestId(IdFactory.getId());
        rpcHeader.setMagic(RpcConstants.MAGIC);
        rpcHeader.setMsgType((byte) RpcType.REQUEST.getType());
        rpcHeader.setStatus((byte) 0x1);
        rpcHeader.setSerializationType(serializationType);
        return rpcHeader;
    }

    public static RpcHeader getRequestHeader(String serializationType, int msgType){
        RpcHeader rpcHeader = new RpcHeader();
        rpcHeader.setRequestId(IdFactory.getId());
        rpcHeader.setMagic(RpcConstants.MAGIC);
        rpcHeader.setMsgType((byte) RpcType.findByType(msgType).getType());
        rpcHeader.setStatus((byte) 0x1);
        rpcHeader.setSerializationType(serializationType);
        return rpcHeader;
    }
}
