package io.check.rpc.protocol.enumeration;

public enum RpcType {

    // 请求消息
    REQUEST(1),

    // 响应消息
    RESPONSE(2),

    // 心跳数据
    HEARTBEAT(3);

    private final int type;

    RpcType(int type) {
        this.type = type;
    }

    public RpcType findType(int searchType) {
        for (RpcType rpcType : RpcType.values()) {
            if (rpcType.getType() == searchType) {
                return rpcType;
            }
        }
        return null;
    }

    public int getType() {
        return this.type;
    }
}
