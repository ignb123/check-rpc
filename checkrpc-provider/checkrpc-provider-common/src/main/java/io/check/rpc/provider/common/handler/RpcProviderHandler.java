package io.check.rpc.provider.common.handler;

import io.check.rpc.cache.result.CacheResultKey;
import io.check.rpc.cache.result.CacheResultManager;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.common.threadpool.ServerThreadPool;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcStatus;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.check.rpc.provider.common.cache.ProviderChannelCache;
import io.check.rpc.reflect.api.ReflectInvoker;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.check.rpc.threadpool.ConcurrentThreadPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RpcProviderHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    private final Logger logger = LoggerFactory.getLogger(RpcProviderHandler.class);

    private final Map<String,Object> handlerMap;

    /**
     * reflectInvoker用于反射调用服务提供者的方法。
     */
    private ReflectInvoker reflectInvoker;

    /**
     * 结果缓存开关
     */
    private final boolean enableResultCache;

    /**
     * 结果缓存管理器
     */
    private final CacheResultManager<RpcProtocol<RpcResponse>> cacheResultManager;

    /**
     * 并发控制的线程池
     */
    private final ConcurrentThreadPool concurrentThreadPool;

    public RpcProviderHandler(Map<String,Object> handlerMap, boolean enableResultCache,
                              int resultCacheExpire, int corePoolSize, int maximumPoolSize,String reflectType) {
        this.handlerMap = handlerMap;
        this.reflectInvoker = ExtensionLoader.getExtension(ReflectInvoker.class,reflectType);
        if (resultCacheExpire <= 0){
            resultCacheExpire = RpcConstants.RPC_SCAN_RESULT_CACHE_EXPIRE;
        }
        this.enableResultCache = enableResultCache;
        this.cacheResultManager = CacheResultManager.getInstance(resultCacheExpire,enableResultCache);
        this.concurrentThreadPool = ConcurrentThreadPool.getInstance(corePoolSize,maximumPoolSize);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ProviderChannelCache.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProviderChannelCache.remove(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        ProviderChannelCache.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        concurrentThreadPool.submit(() -> {
            RpcProtocol<RpcResponse> responseRpcProtocol = handlerMessage(protocol, ctx.channel());
            ctx.writeAndFlush(responseRpcProtocol).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    logger.debug("Send response for request " + protocol.getHeader().getRequestId());
                }
            });
        });
    }

    /**
     * 当Netty的IdleStateHandler触发超时机制时，会将事件传递到pipeline中的下一个Handler，
     * 也就是RpcProviderHandler，而接收超时事件的方法就是userEventTriggered()方法
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //如果是IdleStateEvent事件
        if(evt instanceof IdleStateEvent) {
            Channel channel = ctx.channel();
            try {
                logger.info("IdleStateEvent triggered, close channel " + channel);
                channel.close();
            } finally {
                /**
                 * 向指定的通道写入空缓冲区并关闭通道。
                 * 这个操作首先会尝试清空缓冲区并写入数据到通道中，接着会关闭这个通道。
                 * @param channel 待写入数据并关闭的通道。
                 */
                channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                        .addListener(ChannelFutureListener.CLOSE);
            }
        }
        super.userEventTriggered(ctx,evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("server caught exception", cause);
        ProviderChannelCache.remove(ctx.channel());
        ctx.close();
    }

    /**
     * 根据成员变量enableResultCache的值来处理结果数据，如果enableResultCache的值为true，
     * 则调用handlerRequestMessageCache()方法处理结果数据，如果enableResultCache的值为false，
     * 则调用handlerRequestMessage()方法处理结果数据
     * @param protocol
     * @param header
     * @return
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageWithCache(RpcProtocol<RpcRequest> protocol, RpcHeader header){
        header.setMsgType((byte) RpcType.RESPONSE.getType());
        if (enableResultCache) return handlerRequestMessageCache(protocol, header);
        return handlerRequestMessage(protocol, header);
    }


    /**
     * 通过缓存来处理服务提供者调用真实方法获取到的结果数据
     * @param protocol
     * @param header
     * @return
     */
    private RpcProtocol<RpcResponse> handlerRequestMessageCache(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        RpcRequest request = protocol.getBody();
        CacheResultKey cacheKey = new CacheResultKey(request.getClassName(), request.getMethodName(),
                request.getParameterTypes(), request.getParameters(), request.getVersion(), request.getGroup());
        RpcProtocol<RpcResponse> responseRpcProtocol = cacheResultManager.get(cacheKey);
        if(responseRpcProtocol == null){
            responseRpcProtocol = handlerRequestMessage(protocol, header);
            //设置保存的时间
            cacheKey.setCacheTimeStamp(System.currentTimeMillis());
            cacheResultManager.put(cacheKey,responseRpcProtocol);
        }
        responseRpcProtocol.setHeader(header);
        return responseRpcProtocol;
    }

    /**
     * 处理消息
     */
    private RpcProtocol<RpcResponse> handlerMessage(RpcProtocol<RpcRequest> protocol, Channel channel) {
        RpcProtocol<RpcResponse> responseRpcProtocol = null;
        RpcHeader header = protocol.getHeader();
        //从服务消费者发起的心跳数据
        if (header.getMsgType() == (byte) RpcType.HEARTBEAT_FROM_CONSUMER.getType()){
            responseRpcProtocol = this.handlerHeartbeatMessageFromConsumer(protocol, header);
        }else if (header.getMsgType() == (byte) RpcType.HEARTBEAT_TO_PROVIDER.getType()){  //服务消费者响应服务提供者的心跳数据
            this.handlerHeartbeatMessageToProvider(protocol, channel);
        }else if (header.getMsgType() == (byte) RpcType.REQUEST.getType()){ //请求消息
            responseRpcProtocol = handlerRequestMessageWithCache(protocol, header);
        }
        return responseRpcProtocol;

    }

    /**
     * 处理服务消费者响应的心跳消息
     */

    private void handlerHeartbeatMessageToProvider(RpcProtocol<RpcRequest> protocol, Channel channel) {
        logger.info("receive service consumer heartbeat message, the consumer is: {}, the heartbeat message is: {}",
                channel.remoteAddress(), protocol.getBody().getParameters()[0]);
        // 收到服务提供者pong后，对应channel 等待数归零
        int count = ProviderChannelCache.decreaseWaitTimes(channel);
        logger.info("服务提供者收到服务消费者【{}】心跳响应，当前心跳响应等待【{}】次", channel.remoteAddress(), count);
    }

    /**
     * 要是接收服务提供者发送过来的心跳ping消息，并响应pong消息
     */
    private RpcProtocol<RpcResponse> handlerHeartbeatMessageFromConsumer(RpcProtocol<RpcRequest> protocol, RpcHeader header){
        header.setMsgType((byte) RpcType.HEARTBEAT_TO_CONSUMER.getType());

        RpcRequest request = protocol.getBody();
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<RpcResponse>();

        RpcResponse response = new RpcResponse();
        response.setResult(RpcConstants.HEARTBEAT_PONG);
        response.setAsync(request.isAsync());
        response.setOneway(request.isOneway());

        header.setStatus((byte) RpcStatus.SUCCESS.getCode());
        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(response);

        return responseRpcProtocol;

    }

    /**
     * 处理服务消费者发送过来的请求消息，按照自定义网络传输协议，解析出调用真实方法的信息，
     * 调用真实方法后，并按照自定义网络传输协议，将调用真实方法的结果封装成响应协议返回给服务消费者
     * @param protocol
     * @param header
     * @return
     */
    private RpcProtocol<RpcResponse> handlerRequestMessage(RpcProtocol<RpcRequest> protocol, RpcHeader header) {
        RpcRequest request = protocol.getBody();

        logger.debug("Receive request " + header.getRequestId());

        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<RpcResponse>();
        RpcResponse response = new RpcResponse();
        try{
            Object result = handle(request);
            response.setResult(result);
            response.setAsync(request.isAsync());
            response.setOneway(request.isOneway());

            header.setStatus((byte) RpcStatus.SUCCESS.getCode());
        }catch (Throwable t) {
            response.setError(t.toString());
            header.setStatus((byte) RpcStatus.FAIL.getCode());
            logger.error("RPC Server handle request error",t);
        }

        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(response);

        return responseRpcProtocol;
    }
    private Object handle(RpcRequest request) throws Throwable {
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(),
                request.getVersion(),request.getGroup());
        Object serviceBean = handlerMap.get(serviceKey);
        if(serviceBean == null){
            throw new RuntimeException(String.format("service not exist: %s:%s",
                    request.getClassName(),
                    request.getMethodName()));
        }
        Class<?> serviceClass  = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        logger.debug(serviceClass.getName());
        logger.debug(methodName);
        if(parameterTypes != null && parameterTypes.length > 0){
            for(int i = 0; i < parameterTypes.length; i++){
                logger.debug(parameterTypes[i].getName());
            }
        }

        if (parameters != null && parameters.length > 0){
            for (int i = 0; i < parameters.length; ++i) {
                logger.debug(parameters[i].toString());
            }
        }

        return this.reflectInvoker.invokeMethod(serviceBean,serviceClass,methodName, parameterTypes, parameters);
    }
}
