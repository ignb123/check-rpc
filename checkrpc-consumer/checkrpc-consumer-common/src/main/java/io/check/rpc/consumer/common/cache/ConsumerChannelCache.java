package io.check.rpc.consumer.common.cache;

import io.binghe.rpc.constants.RpcConstants;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在服务消费者端缓存连接服务提供者成功的Channel
 */
public class ConsumerChannelCache {


    private static final Logger logger = LoggerFactory.getLogger(ConsumerChannelCache.class);

    /**
     * 维护心跳待响应次数
     */
    private static volatile Map<String, AtomicInteger> waitingPongTimesMap = new ConcurrentHashMap<>();

    private static volatile Set<Channel> channelCache = new CopyOnWriteArraySet<>();

    public static void add(Channel channel){
        channelCache.add(channel);
        waitingPongTimesMap.put(getKey(channel),new AtomicInteger(0));
    }

    public static void remove(Channel channel){
        if (channel == null) {
            logger.error("传入的channel参数为null，无法移除");
            return;
        }else{
            channelCache.remove(channel);
            waitingPongTimesMap.remove(getKey(channel));
        }
    }

    public static Set<Channel> getChannelCache(){
        logger.info("当前的 channelCache 中数量 {} ， waitingPongTimesMap 的数量 {}", channelCache.size(), waitingPongTimesMap.size());
        return channelCache;
    }

    private static String getKey(Channel channel) {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        return socketAddress.getAddress().getHostAddress().concat("_").concat(String.valueOf(socketAddress.getPort()));
    }

    /**
     * 收到服务提供者pong后，对应channel 等待数归零
     */
    public static int decreaseWaitTimes(Channel channel) {
        AtomicInteger count = waitingPongTimesMap.get(getKey(channel));
        if (count != null) {
            count.set(0);
        }
        return 0;
    }

    /**
     * 给服务提供者发送ping后，对应channel 等待数加1
     */
    public static int increaseWaitTimes(Channel channel) {
        AtomicInteger count = waitingPongTimesMap.get(getKey(channel));
        if (count != null) {
            return count.incrementAndGet();
        }
        return 0;
    }

    /**
     *  检查是否超过3次心跳没有响应
     */
    public static boolean isWaitTimesOverflow(Channel channel) {
        AtomicInteger count = waitingPongTimesMap.get(getKey(channel));
        if (count != null) {
            return count.get() >= RpcConstants.MAX_WAITING_PONG_TIMES;
        }
        return false;
    }
}
