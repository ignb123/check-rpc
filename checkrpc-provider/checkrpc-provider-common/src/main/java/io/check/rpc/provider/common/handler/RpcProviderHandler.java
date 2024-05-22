package io.check.rpc.provider.common.handler;

import com.alibaba.fastjson2.JSONObject;
import io.check.rpc.common.helper.RpcServiceHelper;
import io.check.rpc.common.threadpool.ServerThreadPool;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcStatus;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

public class RpcProviderHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    private final Logger logger = LoggerFactory.getLogger(RpcProviderHandler.class);

    private final Map<String,Object> handlerMap;

    public RpcProviderHandler(Map<String,Object> handlerMap) {
        this.handlerMap = handlerMap;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        ServerThreadPool.submit(() -> {
            RpcHeader header = protocol.getHeader();
            //将header中的消息类型设置为响应类型的消息
            header.setMsgType((byte) RpcType.RESPONSE.getType());
            RpcRequest request = protocol.getBody();
            logger.debug("Receive request " + header.getRequestId());
            //构建响应协议数据
            RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<>();

            RpcResponse response = new RpcResponse();
            try {
                Object result = handle(request);
                response.setResult(result);
                response.setAsync(request.isAsync());
                response.setOneway(request.isOneway());
                header.setStatus((byte) RpcStatus.SUCCESS.getCode());
            }catch (Throwable t){
                response.setError(t.toString());
                header.setStatus((byte) RpcStatus.FAIL.getCode());
                logger.error("RPC Server handle request error",t);
            }

            responseRpcProtocol.setHeader(header);
            responseRpcProtocol.setBody(response);
            /**
             * 将指定的responseRpcProtocol通过与上下文关联的通道写入并刷新。
             * 同时，在写入和刷新操作完成时，会记录一条调试日志。
             *
             * @param ctx 与发送响应相关的通道上下文，提供了与通道交互的方法，如写入和刷新数据。
             * @param responseRpcProtocol 要发送的响应数据的RpcProtocol对象。此对象将被序列化并通过通道在网络上传输。
             */
            ctx.writeAndFlush(responseRpcProtocol)
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            // 记录一条调试日志，表示已完成针对特定请求的响应发送。
                            logger.debug("Send response for request " + header.getRequestId());
                        }
                    });

        });
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

        return invokeMethod(serviceBean,serviceClass,methodName, parameterTypes, parameters);
    }

    private Object invokeMethod(Object serviceBean, Class<?> serviceClass, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Throwable {
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(serviceBean, parameters);
    }
}
