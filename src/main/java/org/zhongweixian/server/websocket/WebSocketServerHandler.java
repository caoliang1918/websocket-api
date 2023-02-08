package org.zhongweixian.server.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.entity.Message;
import org.zhongweixian.listener.ConnectionListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (null != msg && msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            Map params = getUrlParams(uri);
            //如果url包含参数，需要处理
            if (uri.contains("?")) {
                String newUri = uri.substring(0, uri.indexOf("?"));
                request.setUri(newUri);
            }
            super.channelRead(ctx, msg);
            listener.connect(ctx.channel(), params);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            try {
                JSONObject jsonObject = JSONObject.parseObject(frame.text());
                if (jsonObject == null) {
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("received client:{}, message:{}", ctx.channel().id(), jsonObject);
                }
                if ("ping".equals(jsonObject.getString("cmd"))) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"type\":\"pong\",\"status\":0,\"sequence\":" + Instant.now().toEpochMilli() + "}"));
                    return;
                }
                if (jsonObject != null) {
                    listener.onMessage(ctx.channel(), frame.text());
                }
            } catch (Exception e) {
                logger.error("解析json:{} 异常", frame.text(), e);
                JSONObject error = new JSONObject();
                error.put("messgae", e.getMessage());
                error.put("code", 500);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(error.toJSONString()));
            }finally {
                super.channelRead(ctx, msg);
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, WebSocketFrame webSocketFrame) throws Exception {
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
        listener.onClose(ctx.channel(), 501, cause.getMessage());
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
                    message.setType("timeout");
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

    private static Map getUrlParams(String url) {
        Map<String, String> map = new HashMap<>();
        url = url.replace("?", ";");
        if (!url.contains(";")) {
            return map;
        }
        if (url.split(";").length > 0) {
            String[] arr = url.split(";")[1].split("&");
            for (String s : arr) {
                String key = s.split("=")[0];
                String value = s.split("=")[1];
                map.put(key, value);
            }
            return map;

        } else {
            return map;
        }
    }
}
