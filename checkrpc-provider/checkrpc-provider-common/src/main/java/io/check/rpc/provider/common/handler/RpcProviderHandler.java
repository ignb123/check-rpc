package io.check.rpc.provider.common.handler;

import com.alibaba.fastjson2.JSONObject;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RpcProviderHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    private final Logger logger = LoggerFactory.getLogger(RpcProviderHandler.class);

    private final Map<String,Object> handlerMap;

    public RpcProviderHandler(Map<String,Object> handlerMap) {
        this.handlerMap = handlerMap;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        logger.info("RPC提供者收到的数据为====>>> " + JSONObject.toJSONString(protocol));
        logger.info("handlerMap中存放的数据如下所示：");
        for (Map.Entry<String,Object> entry : handlerMap.entrySet()){
            logger.info(entry.getKey() + " === " + entry.getValue());
        }

        RpcHeader header = protocol.getHeader();
        RpcRequest request = protocol.getBody();
        //将header中的消息类型设置为响应类型的消息
        header.setMsgType((byte) RpcType.RESPONSE.getType());
        //构建响应协议数据
        RpcProtocol<RpcResponse> responseRpcProtocol = new RpcProtocol<RpcResponse>();

        RpcResponse response = new RpcResponse();
        response.setResult("数据交互成功");
        response.setAsync(request.isAsync());
        response.setOneway(request.isOneway());

        responseRpcProtocol.setHeader(header);
        responseRpcProtocol.setBody(response);

        ctx.writeAndFlush(responseRpcProtocol);
    }
}
