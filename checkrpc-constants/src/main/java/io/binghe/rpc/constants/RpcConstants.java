package io.binghe.rpc.constants;

/**
 * @author check
 * @version 1.0.0
 * @description 常量类
 */
public class RpcConstants {

    /**
     * 消息头，固定32个字节
     */
    public static final int HEADER_TOTAL_LEN = 32;

    /**
     * 魔数
     */
    public static final short MAGIC = 0x10;

    /**
     * 版本号
     */
    public static final byte VERSION = 0x1;

    /**
     * REFLECT_TYPE_JDK
     */
    public static final String REFLECT_TYPE_JDK = "jdk";

    /**
     * REFLECT_TYPE_CGLIB
     */
    public static final String REFLECT_TYPE_CGLIB = "cglib";

    /**
     * JDK动态代理
     */
    public static final String PROXY_JDK = "jdk";

    /**
     * javassist动态代理
     */
    public static final String PROXY_JAVASSIST = "javassist";

    /**
     * cglib动态代理
     */
    public static final String PROXY_CGLIB = "cglib";


}
