package io.check.rpc.codec;

import io.check.rpc.common.utils.SerializationUtils;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.serialization.api.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<RpcProtocol<Object>> implements RpcCodec{
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol<Object> msg, ByteBuf byteBuf) throws Exception {
        RpcHeader header = msg.getHeader();
        byteBuf.writeShort(header.getMagic());
        byteBuf.writeByte(header.getMsgType());
        byteBuf.writeByte(header.getStatus());
        byteBuf.writeLong(header.getRequestId());
        String serializationType = header.getSerializationType();
        //TODO Serialization是扩展点
        Serialization serialization = getJdkSerialization();
        byteBuf.writeBytes(SerializationUtils.paddingString(serializationType)
                .getBytes("UTF-8"));
        try {
            byte[] data = serialization.serialize(msg.getBody());
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
