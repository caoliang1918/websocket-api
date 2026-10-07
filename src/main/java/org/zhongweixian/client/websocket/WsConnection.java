package org.zhongweixian.client.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WsConnection implements Connection {
    private static final Logger logger = LoggerFactory.getLogger(WsConnection.class);

    private final Channel channel;

    public WsConnection(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void sendText(String payload) {
        if (!isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(payload));
    }

    @Override
    public void sendByte(byte[] bytes) {
        if (!isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes)));
    }

    @Override
    public void sendPing() {
        if (!isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        channel.writeAndFlush(new PingWebSocketFrame());
    }

    @Override
    public String getId() {
        return channel != null ? channel.id().toString() : null;
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}