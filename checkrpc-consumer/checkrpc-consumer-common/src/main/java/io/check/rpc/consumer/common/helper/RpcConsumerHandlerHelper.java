package io.check.rpc.consumer.common.helper;

import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcConsumerHandlerHelper {

    private static Map<String, RpcConsumerHandler> rpcConsumerHandlerMap;

    static{
        rpcConsumerHandlerMap = new ConcurrentHashMap<>();
    }

    private static String getKey(ServiceMeta key){
        return key.getServiceAddr().concat("_").concat(String.valueOf(key.getServicePort()));
    }

    public static void put(ServiceMeta key, RpcConsumerHandler value){
        rpcConsumerHandlerMap.put(getKey(key), value);
    }

    public static RpcConsumerHandler get(ServiceMeta key){
        return rpcConsumerHandlerMap.get(getKey(key));
    }

    public static void closeRpcClientHandler() {
        Collection<RpcConsumerHandler> rpcClientHandlers = rpcConsumerHandlerMap.values();
        if(rpcClientHandlers != null){
            rpcClientHandlers.forEach(rpcClientHandler -> {
                rpcClientHandler.close();
            });
        }
        rpcClientHandlers.clear();
    }

    public static int size() {
        return rpcConsumerHandlerMap.size();
    }

    public static void remove(Channel channel) {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        String address = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        rpcConsumerHandlerMap.remove(generateKey(address, port));
    }
    private static String generateKey(String address, int port){
        return address.concat("_").concat(String.valueOf(port));
    }
}
