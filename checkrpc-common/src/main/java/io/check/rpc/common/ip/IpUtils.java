package io.check.rpc.common.ip;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * @author check
 * @version 1.0.0
 * @description IP工具类
 */
public class IpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpUtils.class);

    /**
     * 获取本地主机的InetAddress实例。
     *
     * @return 返回本地主机的InetAddress实例，如果发生异常则返回null。
     */
    public static InetAddress getLocalInetAddress()  {
        try{
            return InetAddress.getLocalHost();
        }catch (Exception e){
            LOGGER.error("get local ip address throws exception: {}", e); // 记录获取本地IP地址时抛出的异常
        }
        return null;
    }

    /**
     * 获取本地主机的地址字符串。
     *
     * @return 返回本地主机的地址字符串，形式为IP地址的字符串表示。
     */
    public static String getLocalAddress(){
        return getLocalInetAddress().toString();
    }

    /**
     * 获取本地主机的主机名。
     *
     * @return 返回本地主机的主机名。
     */
    public static String getLocalHostName(){
        return getLocalInetAddress().getHostName();
    }

    /**
     * 获取本地主机的IP地址。
     *
     * @return 返回本地主机的IP地址字符串。
     */
    public static String getLocalHostIp(){
        return getLocalInetAddress().getHostAddress();
    }


}
