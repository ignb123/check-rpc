package io.check.rpc.common.utils;

/**
 * @author check
 * @version 1.0.0
 * @description 字符串工具类
 */
public class StringUtils {

    public static boolean isEmpty(String str){
        return str == null || str.trim().isEmpty();
    }
}

