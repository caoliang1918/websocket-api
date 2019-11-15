package org.zhongweixian.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.zhongweixian.listener.ConnectionListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.entity.Message;

@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private Integer heart;

    /**
     * 回调消息类
     */
    private ConnectionListener listener;

    public WebSocketServerHandler(Integer heart, ConnectionListener listener) {
        this.heart = heart;
        this.listener = listener;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        if (StringUtils.isBlank(textWebSocketFrame.text())) {
            return;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(textWebSocketFrame.text());
            logger.debug("received client:{}, message:{}", ctx.channel().id(), jsonObject);
            if (jsonObject != null && "PING".equals(jsonObject.getString("cmd").toUpperCase())) {
                return;
            }
            if (jsonObject != null) {
                listener.onMessage(ctx.channel(), textWebSocketFrame.text());
            }
        } catch (Exception e) {
            logger.error("解析json:{} 异常:{}", textWebSocketFrame.text(), e);
            JSONObject error = new JSONObject();
            error.put("messgae", e.getMessage());
            error.put("code", 500);
            ctx.channel().writeAndFlush(new TextWebSocketFrame(error.toJSONString()));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.error("websocket client :{}, inactive : {}", ctx.channel().id(), ctx.channel().isActive());
        listener.onClose(ctx.channel(), 500, "channelInactive");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //异常时断开连接
        logger.error("websocket client:{}  exceptionCaught:{} ", ctx.channel().id(), cause);
        listener.onClose(ctx.channel(), 505, cause.getMessage());
    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("websocket client:{} , connect success", ctx.channel().id());
        listener.connect(ctx.channel());
        ctx.fireChannelRegistered();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    if (heart <= 0) {
                        return;
                    }
                    logger.warn("No heartbeat message received in {} seconds", heart);
                    //向客户端发送关闭连接消息
                    Message message = new Message();
                    message.setType("close");
                    message.setCode("10005");
                    message.setMessage("no heartbeat message received in " + heart + " seconds , channel closed");
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
                    ctx.close();
                    break;
                case WRITER_IDLE:
                    break;

                default:
                    break;
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
