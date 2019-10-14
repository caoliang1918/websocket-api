package com.yuntongxun.api.client.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
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
        channel.close();
    }

    @Override
    public void sendText(String payload) {
        channel.writeAndFlush(payload);
    }

    @Override
    public void sendByte(byte[] bytes) {

    }

    @Override
    public void sendPing() {
        PingWebSocketFrame frame = new PingWebSocketFrame();
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(frame);
        }
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
