package org.zhongweixian.decode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * Created by caoliang on 2020-02-25
 */
public class MessageEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, ByteBuf byteBuf) throws Exception {
        // 显式使用 UTF-8，与 MessageDecoder 保持一致，避免平台默认字符集导致中文乱码
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byteBuf.writeInt(4 + bytes.length);
        byteBuf.writeBytes(bytes);
    }
}
