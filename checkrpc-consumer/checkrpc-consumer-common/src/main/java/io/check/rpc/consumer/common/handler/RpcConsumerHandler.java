package io.check.rpc.consumer.common.handler;

import com.alibaba.fastjson2.JSONObject;
import io.check.rpc.buffer.cache.BufferCacheManager;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.consumer.common.cache.ConsumerChannelCache;
import io.check.rpc.consumer.common.context.RpcContext;

import io.check.rpc.exception.processor.ExceptionPostProcessor;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcStatus;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.threadpool.BufferCacheThreadPool;
import io.check.rpc.threadpool.ConcurrentThreadPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
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

    // 并发处理线程池
    private ConcurrentThreadPool concurrentThreadPool;

    /**
     * 是否开启缓冲区
     */
    private boolean enableBuffer;

    /**
     * 缓冲区管理器
     */
    private BufferCacheManager<RpcProtocol<RpcResponse>> bufferCacheManager;

    /**
     * 异常后置处理器
     */
    private ExceptionPostProcessor exceptionPostProcessor;


    public RpcConsumerHandler(boolean enableBuffer, int bufferSize, ConcurrentThreadPool concurrentThreadPool,
                              ExceptionPostProcessor exceptionPostProcessor){
        this.concurrentThreadPool = concurrentThreadPool;
        this.enableBuffer = enableBuffer;
        this.exceptionPostProcessor = exceptionPostProcessor;
        if (enableBuffer){
            this.bufferCacheManager = BufferCacheManager.getInstance(bufferSize);
            BufferCacheThreadPool.submit(() -> {
                consumerBufferCache();
            });
        }
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
        ConsumerChannelCache.add(channel);
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

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        if(ctx.channel() != null){
            ConsumerChannelCache.remove(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConsumerChannelCache.remove(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            //发送一次心跳数据
            RpcHeader header = RpcHeaderFactory
                    .getRequestHeader(RpcConstants.SERIALIZATION_PROTOSTUFF, RpcType.HEARTBEAT_FROM_CONSUMER.getType());
            RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<RpcRequest>();
            RpcRequest rpcRequest = new RpcRequest();
            rpcRequest.setParameters(new Object[]{RpcConstants.HEARTBEAT_PING});
            requestRpcProtocol.setHeader(header);
            requestRpcProtocol.setBody(rpcRequest);
            ctx.writeAndFlush(requestRpcProtocol);
        }else {
            super.userEventTriggered(ctx, evt);
        }
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
        concurrentThreadPool.submit(() -> {
            // 记录接收到的数据
            this.handlerMessage(protocol,ctx.channel());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exceptionPostProcessor.postExceptionProcessor(cause);
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 不断消费缓冲区中的数据，调用handlerResponseMessage()方法进行处理
     */
    private void consumerBufferCache() {
        //不断消息缓冲区的数据
        while (true){
            RpcProtocol<RpcResponse> protocol = this.bufferCacheManager.take();
            if (protocol != null){
                this.handlerResponseMessage(protocol);
            }
        }
    }

    /**
     * 根据enableBuffer的值进行不同的处理逻辑，
     * 当enableBuffer的值为true时，会将数据放入缓冲区，
     * 当enableBuffer的值为false时，调用handlerResponseMessage()方法
     * @param protocol
     */
    private void handlerResponseMessageOrBuffer(RpcProtocol<RpcResponse> protocol){
        if (enableBuffer){
            logger.info("enable buffer...");
            this.bufferCacheManager.put(protocol);
        }else {
            this.handlerResponseMessage(protocol);
        }
    }

    private void handlerMessage(RpcProtocol<RpcResponse> protocol, Channel channel){
        RpcHeader header = protocol.getHeader();
        //服务提供者响应的心跳消息
        if (header.getMsgType() == (byte) RpcType.HEARTBEAT_TO_CONSUMER.getType()){
            this.handlerHeartbeatMessageToConsumer(protocol, channel);
        }else if (header.getMsgType() == (byte) RpcType.HEARTBEAT_FROM_PROVIDER.getType()){
            this.handlerHeartbeatMessageFromProvider(protocol, channel);
        }else if (header.getMsgType() == (byte) RpcType.RESPONSE.getType()){ //响应消息
            this.handlerResponseMessageOrBuffer(protocol);
        }
    }

    /**
     * 要是接收服务提供者发送过来的心跳ping消息，并响应pong消息
     * @param protocol
     * @param channel
     */
    private void handlerHeartbeatMessageFromProvider(RpcProtocol<RpcResponse> protocol, Channel channel) {
        RpcHeader header = protocol.getHeader();
        header.setMsgType((byte) RpcType.HEARTBEAT_TO_PROVIDER.getType());
        RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<RpcRequest>();
        RpcRequest request = new RpcRequest();
        request.setParameters(new Object[]{RpcConstants.HEARTBEAT_PONG});
        header.setStatus((byte) RpcStatus.SUCCESS.getCode());
        requestRpcProtocol.setHeader(header);
        requestRpcProtocol.setBody(request);
        channel.writeAndFlush(requestRpcProtocol);
    }
    /**
     * 处理心跳消息，这里由于心跳是服务消费者向服务提供者发起，服务提供者接收到心跳消息后，
     * 会立即进行响应。所以，在服务消费者接收到服务提供者响应的心跳消息后，可不必在任何处理，打印日志即可
     * @param protocol
     */
    private void handlerHeartbeatMessageToConsumer(RpcProtocol<RpcResponse> protocol, Channel channel) {
        //此处简单打印即可,实际场景可不做处理
        logger.info("receive service provider heartbeat message, the provider is: {}, the heartbeat message is: {}",
                channel.remoteAddress(), protocol.getBody().getResult());
        // 收到服务提供者pong后，对应channel 等待数归零
        int count = ConsumerChannelCache.decreaseWaitTimes(channel);
        logger.info("服务消费者收到服务提供者[{}]心跳响应，当前心跳响应等待[{}]次", channel.remoteAddress(), count);
    }

    /**
     * 获取到响应的结果信息后，会唤醒阻塞的线程，向客户端响应数据
     * @param protocol
     */
    private void handlerResponseMessage(RpcProtocol<RpcResponse> protocol) {
        long requestId = protocol.getHeader().getRequestId();
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

        return concurrentThreadPool.submit(() -> {
            return oneway ? this.sendRequestOneway(protocol)
                    : async ? sendRequestAsync(protocol) : this.sendRequestSync(protocol);
        });

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
        ConsumerChannelCache.remove(channel);
    }

    private RPCFuture getRpcFuture(RpcProtocol<RpcRequest> protocol) {
        RPCFuture rpcFuture = new RPCFuture(protocol, concurrentThreadPool);
        long requestId = protocol.getHeader().getRequestId();
        pendingRPC.put(requestId, rpcFuture);
        return rpcFuture;
    }
}
