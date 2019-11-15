package org.zhongweixian.client.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WsConnection implements Connection {
    private Logger logger = LoggerFactory.getLogger(WsConnection.class);

    private Channel channel;

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
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(payload));
    }

    @Override
    public void sendByte(byte[] bytes) {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
        channel.writeAndFlush(frame);
    }

    @Override
    public void sendPing() {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or channel is not active");
            return;
        }
        PingWebSocketFrame frame = new PingWebSocketFrame();
        channel.writeAndFlush(frame);
    }

    @Override
    public String getId() {
        if (channel != null) {
            return channel.id().toString();
        }
        return null;

    }

    @Override
    public boolean isActive() {
        if (channel != null && channel.isActive()) {
            return true;
        }
        return false;
    }
}
