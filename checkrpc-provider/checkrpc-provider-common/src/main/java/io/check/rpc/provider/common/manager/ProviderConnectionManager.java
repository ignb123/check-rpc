package io.check.rpc.provider.common.manager;

import io.binghe.rpc.constants.RpcConstants;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.check.rpc.provider.common.cache.ProviderChannelCache;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ProviderConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderConnectionManager.class);

    /**
     * 扫描并移除不活跃的连接
     */
    public static void scanNotActiveChannel(){
        Set<Channel> channelCache = ProviderChannelCache.getChannelCache();
        if (channelCache == null || channelCache.isEmpty()) return;
        channelCache.stream().forEach((channel) -> {
            if (!channel.isOpen() || !channel.isActive()){
                channel.close();
                ProviderChannelCache.remove(channel);
            }
        });
    }

    /**
     * 发送ping消息
     */
    public static void broadcastPingMessageFromConsumer(){
        Set<Channel> channelCache = ProviderChannelCache.getChannelCache();
        if (channelCache == null || channelCache.isEmpty()) return;
        RpcProtocol<RpcResponse> responseRpcProtocol = buildPingProtocol();
        channelCache.stream().forEach((channel) -> {
            if (channel.isOpen() && channel.isActive()){
                // 检查是否查过次数
                boolean overflow = ProviderChannelCache.isWaitTimesOverflow(channel);
                if (overflow) {
                    // 关闭通道并清除缓存等资源
                    closeChannelAndClear(channel);
                } else {
                    // 没有超过次数，正常心跳，心跳后计数+1
                    sendPing(responseRpcProtocol, channel);
                }
            }
        });
    }

    /**
     * 向指定的服务消费者发送心跳消息。
     *
     * @param responseRpcProtocol 包含心跳响应数据的RpcProtocol对象
     * @param channel 用于与服务消费者通信的Channel对象
     */
    private static void sendPing(RpcProtocol<RpcResponse> responseRpcProtocol, Channel channel) {
        // 记录发送心跳消息的日志
        LOGGER.info("send heartbeat message to service consumer, the consumer is: {}, the heartbeat message is: {}",
                channel.remoteAddress(), responseRpcProtocol.getBody().getResult());
        // 写入并刷新心跳消息到通道
        channel.writeAndFlush(responseRpcProtocol);
        // 增加等待次数计数
        int count = ProviderChannelCache.increaseWaitTimes(channel);
        // 记录当前心跳响应等待次数
        LOGGER.info("服务提供者给服务消费者[{}]发送心跳，当前心跳响应等待[{}]次", channel.remoteAddress(), count);
    }

    /**
     * 关闭指定的通道并从缓存中移除。
     *
     * @param channel 需要关闭并移除缓存的Channel对象
     */
    private static void closeChannelAndClear(Channel channel) {
        // 记录关闭通道并删除缓存的日志
        LOGGER.info("服务消费者 {} 超过3次没有回复服务提供者心跳，将关闭通道并删除缓存", channel.remoteAddress());
        // 关闭通道
        channel.close();
        // 从缓存中移除通道
        ProviderChannelCache.remove(channel);
    }

    /**
     * 构建用于发送的心跳协议消息。
     *
     * @return 构建完成的RpcProtocol对象，包含心跳响应数据
     */
    private static RpcProtocol<RpcResponse> buildPingProtocol() {
        // 创建RpcProtocol对象并设置头部信息
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<RpcResponse>();
        RpcHeader header = RpcHeaderFactory.getRequestHeader(RpcConstants.SERIALIZATION_PROTOSTUFF,
                RpcType.HEARTBEAT_FROM_PROVIDER.getType());
        RpcResponse rpcResponse = new RpcResponse();
        // 设置心跳消息内容
        rpcResponse.setResult(RpcConstants.HEARTBEAT_PING);
        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(rpcResponse);
        return responseRpcProtocol;
    }

}
