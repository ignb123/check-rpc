package io.binghe.rpc.constants;

public class RpcConstants {

    /**
     * 协议魔数，用于在接收端快速识别数据包是否为本协议的数据。
     * 这是一个示例值，实际应用中应设置为特定的字节序列。
     */
    public static final short MAGIC = 0xCAE;

    public static final int HEADER_TOTAL_LEN = 32;
}
