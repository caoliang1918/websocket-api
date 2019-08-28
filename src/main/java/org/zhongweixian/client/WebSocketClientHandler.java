package org.zhongweixian.client;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;

@ChannelHandler.Sharable
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);


    private ConnectionListener connectionListener;
    private String payload;


    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(String payload, ConnectionListener connectionListener) {
        this.payload = payload;
        this.connectionListener = connectionListener;
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object object) throws Exception {
        logger.debug("channelId:{} , received:{}", ctx.channel().id(), object.toString());
        Channel channel = ctx.channel();
        FullHttpResponse response;
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse) object;
                //握手协议返回，设置结束握手
                this.handshaker.finishHandshake(channel, response);
                //设置成功
                this.handshakeFuture.setSuccess();
                logger.info("WebSocket client:{} connected , response headers[sec-websocket-extensions]:{} ", ctx.channel().id(), response.headers());
                if (StringUtils.isNoneBlank(payload)) {
                    channel.writeAndFlush(new TextWebSocketFrame(payload));
                }
                connectionListener.connect(channel);
            } catch (WebSocketHandshakeException var7) {
                FullHttpResponse res = (FullHttpResponse) object;
                String errorMsg = String.format("WebSocket Client failed to connect,status:%s,reason:%s", res.status(), res.content().toString(CharsetUtil.UTF_8));
                this.handshakeFuture.setFailure(new Exception(errorMsg));
            }
        } else if (object instanceof FullHttpResponse) {
            response = (FullHttpResponse) object;
            this.connectionListener.onFail(response.status().code(), response.content().toString(CharsetUtil.UTF_8));
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            WebSocketFrame frame = (WebSocketFrame) object;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                this.connectionListener.onMessage(channel, textFrame.text());
            } else if (frame instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame;
                connectionListener.onMessage(channel, binFrame.content());
            } else if (frame instanceof PongWebSocketFrame) {
                logger.info("WebSocket Client received pong");
            } else if (frame instanceof CloseWebSocketFrame) {
                logger.info("receive close frame");
                this.connectionListener.onClose(ctx.channel(), ((CloseWebSocketFrame) frame).statusCode(), ((CloseWebSocketFrame) frame).reasonText());
                channel.close();
            }

        }

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("channelRegistered ， channelId:{}", ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.error("连接中断, channelId:{}  , active : {}", ctx.channel().id(), ctx.channel().isActive());
        connectionListener.onClose(ctx.channel(), 500, "channelInactive");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:

                    break;
                case WRITER_IDLE:
                    //向服务端发送消息
                    TextWebSocketFrame frame = new TextWebSocketFrame("{'cmd':'ping' , 'cts':'" + System.currentTimeMillis() + "'}");
                    ctx.writeAndFlush(frame);
                    logger.debug("send ping success!");
                    break;
                case ALL_IDLE:
                    break;
            }
        }
    }

    WebSocketClientHandshaker handshaker;
    ChannelPromise handshakeFuture;


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public WebSocketClientHandshaker getHandshaker() {
        return handshaker;
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

}
