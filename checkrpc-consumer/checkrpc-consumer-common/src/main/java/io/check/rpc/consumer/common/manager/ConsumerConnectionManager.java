package io.check.rpc.consumer.common.manager;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.consumer.common.RpcConsumer;
import io.check.rpc.consumer.common.cache.ConsumerChannelCache;
import io.check.rpc.consumer.common.handler.RpcConsumerHandler;
import io.check.rpc.consumer.common.helper.RpcConsumerHandlerHelper;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.protocol.request.RpcRequest;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;

public class ConsumerConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerConnectionManager.class);

    /**
     * 扫描并移除不活跃的连接
     */
    public static void scanNotActiveChannel(){
        Set<Channel> channelCache = ConsumerChannelCache.getChannelCache();
        if (channelCache == null || channelCache.isEmpty()) return;
        channelCache.stream().forEach((channel) -> {
            if (!channel.isOpen() || !channel.isActive()){
                channel.close();
                ConsumerChannelCache.remove(channel);
                // 清除 RpcConsumerHandlerHelper 缓存
                RpcConsumerHandlerHelper.remove(channel);
            }
        });
    }

    /**
     * 发送ping消息
     */
    public static void broadcastPingMessageFromConsumer(RpcConsumer rpcConsumer){
        Set<Channel> channelCache = ConsumerChannelCache.getChannelCache();
        if (channelCache == null || channelCache.isEmpty()) return;
        RpcProtocol<RpcRequest> requestRpcProtocol = getRpcRequestRpcProtocol();
        channelCache.stream().forEach((channel) -> {
            if (channel.isOpen() && channel.isActive()){
                // 1、检查是否查过次数
                boolean overflow = ConsumerChannelCache.isWaitTimesOverflow(channel);
                if (overflow) {
                    clearAndReconnectProvider(channel, rpcConsumer);
                } else {
                    // 2.没有超过次数，正常心跳，心跳后计数+1
                    sendPing(requestRpcProtocol, channel);
                }
            }
        });
    }

    /** 清除 channel 相关的缓存资源并重新连接服务提供者 */
    private static void clearAndReconnectProvider(Channel channel, RpcConsumer rpcConsumer) {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        String address = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();

        // 1、通道关闭并清理 ConsumerChannelCache
        channel.close();
        ConsumerChannelCache.remove(channel);

        // 2、通过 RpcConsumer 重连获取 RpcConsumerHandler
        try {
            RpcConsumerHandlerHelper.remove(channel);
            LOGGER.info("服务消费者给服务提供者发送心跳超过{}次没有回复，开始重新连接服务提供者 {}", RpcConstants.MAX_WAITING_PONG_TIMES,
                    channel.remoteAddress());
            RpcConsumerHandler consumerHandler = rpcConsumer.getRpcConsumerHandlerWithAddressAndPort(address, port);
            // 3 覆盖 RpcConsumerHandlerHelper 中的缓存
            ServiceMeta serviceMeta = new ServiceMeta();
            serviceMeta.setServiceAddr(address);
            serviceMeta.setServicePort(port);
            RpcConsumerHandlerHelper.put(serviceMeta, consumerHandler);
            LOGGER.info("当前 RpcConsumerHandlerHelper 中缓存的数量 {}", RpcConsumerHandlerHelper.size());
        } catch (InterruptedException e) {
            LOGGER.error("服务消费者重连服务消费者[{}]异常： ", channel.remoteAddress(), e);
            e.printStackTrace();
        }
    }

    private static void sendPing(RpcProtocol<RpcRequest> requestRpcProtocol, Channel channel) {
        LOGGER.info("send heartbeat message to service provider, the provider is: {}, the heartbeat message " +
                "is: {}", channel.remoteAddress(), RpcConstants.HEARTBEAT_PING);
        channel.writeAndFlush(requestRpcProtocol);
        int count = ConsumerChannelCache.increaseWaitTimes(channel);
        LOGGER.info("服务消费者给服务提供者【{}】发送心跳，当前心跳响应等待【{}】次", channel.remoteAddress(), count);
    }

    private static RpcProtocol<RpcRequest> getRpcRequestRpcProtocol() {
        RpcHeader header = RpcHeaderFactory.getRequestHeader(RpcConstants.SERIALIZATION_PROTOSTUFF,
                RpcType.HEARTBEAT_FROM_CONSUMER.getType());
        RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<RpcRequest>();
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setParameters(new Object[]{RpcConstants.HEARTBEAT_PING});
        requestRpcProtocol.setHeader(header);
        requestRpcProtocol.setBody(rpcRequest);
        return requestRpcProtocol;
    }
}
