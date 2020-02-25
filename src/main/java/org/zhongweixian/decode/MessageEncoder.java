package org.zhongweixian.decode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by caoliang on 2020-02-25
 */
public class MessageEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, ByteBuf byteBuf) throws Exception {
        byte[] bytes = s.getBytes();
        byteBuf.writeInt(4 + bytes.length);
        byteBuf.writeBytes(bytes);
    }
}
