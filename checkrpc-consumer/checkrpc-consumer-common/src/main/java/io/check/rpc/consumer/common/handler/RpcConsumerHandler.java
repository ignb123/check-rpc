package io.check.rpc.consumer.common.handler;

import com.alibaba.fastjson2.JSONObject;
import io.check.rpc.consumer.common.context.RpcContext;
import io.check.rpc.consumer.common.future.RPCFuture;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcConsumerHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcResponse>> {

    private final Logger logger = LoggerFactory.getLogger(RpcConsumerHandler.class);

    // 用于存储与服务提供者建立的通信通道
    private volatile Channel channel;

    // 存储远程对端的地址信息
    private SocketAddress remotePeer;

    //存储请求ID与RPCFuture协议的映射关系
    private Map<Long, RPCFuture> pendingRPC = new ConcurrentHashMap<>();

    // 获取当前的通信通道
    public Channel getChannel() {
        return channel;
    }

    // 获取远程对端的地址
    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    /**
     * 当通道激活时的处理逻辑，即连接到服务提供者成功时。
     * @param ctx 通道上下文，提供对通道的操作和相关信息的访问
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 记录远程对端地址
        this.remotePeer = this.channel.remoteAddress();
    }

    /**
     * 当通道注册到线程池时的处理逻辑。
     * @param ctx 通道上下文
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        // 更新当前通道
        this.channel = ctx.channel();
    }

    /**
     * 处理从服务提供者接收到的数据。
     * @param ctx 通道上下文
     * @param protocol 接收到的RPC协议数据包
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcResponse> protocol) throws Exception {
        if (protocol == null){
            return;
        }
        // 记录接收到的数据
        logger.info("服务消费者接收到的数据===>>>{}", JSONObject.toJSONString(protocol));
        RpcHeader header = protocol.getHeader();
        long requestId = header.getRequestId();
        RPCFuture rpcFuture = pendingRPC.remove(requestId);
        if(rpcFuture != null){
            rpcFuture.done(protocol);
        }
    }

    /**
     * 向服务提供者发送请求的逻辑。
     * @param protocol 要发送的RPC协议数据包
     */
    public RPCFuture sendRequest(RpcProtocol<RpcRequest> protocol, boolean async, boolean oneway) {
        // 记录发送的数据
        logger.info("服务消费者发送的数据===>>>{}", JSONObject.toJSONString(protocol));

        return oneway ? sendRequestOneway(protocol) : async ?
                sendRequestAsync(protocol) : sendRequestSync(protocol);

    }

    private RPCFuture sendRequestSync(RpcProtocol<RpcRequest> protocol) {
        RPCFuture rpcFuture = this.getRpcFuture(protocol);
        channel.writeAndFlush(protocol);
        return rpcFuture;
    }

    private RPCFuture sendRequestAsync(RpcProtocol<RpcRequest> protocol) {
        RPCFuture rpcFuture = this.getRpcFuture(protocol);
        //如果是异步调用，则将RPCFuture放入RpcContext
        RpcContext.getContext().setRPCFuture(rpcFuture);
        channel.writeAndFlush(protocol);
        return null;
    }

    private RPCFuture sendRequestOneway(RpcProtocol<RpcRequest> protocol) {
        channel.writeAndFlush(protocol);
        return null;
    }
    /**
     * 关闭当前通信通道的逻辑。
     */
    public void close() {
        // 发送空缓冲区并监听关闭事件
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    private RPCFuture getRpcFuture(RpcProtocol<RpcRequest> protocol) {
        RPCFuture rpcFuture = new RPCFuture(protocol);
        long requestId = protocol.getHeader().getRequestId();
        pendingRPC.put(requestId, rpcFuture);
        return rpcFuture;
    }
}
