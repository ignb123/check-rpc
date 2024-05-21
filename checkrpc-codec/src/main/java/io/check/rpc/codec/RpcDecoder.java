package io.check.rpc.codec;

import io.binghe.rpc.constants.RpcConstants;
import io.check.rpc.common.utils.SerializationUtils;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.protocol.response.RpcResponse;
import io.check.rpc.serialization.api.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder implements RpcCodec{
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < RpcConstants.HEADER_TOTAL_LEN) {
            return;
        }

        // 标记位置，方便reset回位
        in.markReaderIndex();

        short magic = in.readShort();
        if (magic != RpcConstants.MAGIC) {
            throw new IllegalArgumentException("magic number is illegal, " + magic);
        }

        byte msgType = in.readByte();
        byte status = in.readByte();
        long requestId = in.readLong();

        ByteBuf serializationTypeByteBuf = in.readBytes(SerializationUtils.MAX_SERIALIZATION_TYPE_COUNT);
        String serializationType = SerializationUtils
                .subString(serializationTypeByteBuf.toString(CharsetUtil.UTF_8));

        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);

        RpcType msgTypeEnum = RpcType.findByType(msgType);
        if (msgTypeEnum == null) {
            return;
        }

        RpcHeader header = new RpcHeader();
        header.setMagic(magic);
        header.setStatus(status);
        header.setRequestId(requestId);
        header.setMsgType(msgType);
        header.setSerializationType(serializationType);
        header.setMsgLen(dataLength);

        //TODO Serialization是扩展点
        Serialization serialization = getJdkSerialization();
        switch (msgTypeEnum){
            case REQUEST:
                RpcRequest request = serialization.deserialize(data, RpcRequest.class);
                if (request != null) {
                    RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    out.add(protocol);
                }
                break;

            case RESPONSE:
                RpcResponse response = serialization.deserialize(data,RpcResponse.class);
                if(response != null){
                    RpcProtocol<RpcResponse> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    out.add(protocol);
                }
                break;
            case HEARTBEAT:
                // TODO
                break;
        }
    }
}
