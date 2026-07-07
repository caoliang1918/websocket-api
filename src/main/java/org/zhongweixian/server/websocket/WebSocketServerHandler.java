package org.zhongweixian.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.entity.Message;
import org.zhongweixian.listener.ConnectionListener;
import org.zhongweixian.util.UrlUtil;

import java.time.Instant;
import java.util.Map;

@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private final Integer heart;

    /**
     * 回调消息类
     */
    private final ConnectionListener listener;

    public WebSocketServerHandler(Integer heart, ConnectionListener listener) {
        this.heart = heart;
        this.listener = listener;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            HttpHeaders headers = request.headers();
            String uri = request.uri();
            Map<String, Object> params = UrlUtil.parseQuery(uri);
            //如果url包含参数，需要处理
            String newUri = UrlUtil.stripQuery(uri);
            if (newUri != null && !newUri.equals(uri)) {
                request.setUri(newUri);
            }
            String ip = headers.get("X-Real-IP");
            if (StringUtils.isBlank(ip)) {
                String xForwardedFor = headers.get("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    // X-Forwarded-For: client, proxy1, proxy2
                    ip = xForwardedFor.split(",")[0].trim();
                }
            }
            // 兜底：如果都没有，使用直连地址 (通常是 Nginx 的内网 IP)
            if (StringUtils.isBlank(ip)) {
                String remoteAddress = ctx.channel().remoteAddress().toString();
                int slashIdx = remoteAddress.indexOf('/');
                int colonIdx = remoteAddress.lastIndexOf(':');
                if (slashIdx >= 0 && colonIdx > slashIdx) {
                    ip = remoteAddress.substring(slashIdx + 1, colonIdx);
                }
            }
            if (ip != null) {
                params.put("ip", ip);
            }
            super.channelRead(ctx, msg);
            listener.connect(ctx.channel(), params);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            try {
                String text = frame.text();
                JSONObject jsonObject = JSONObject.parseObject(text);
                if (jsonObject == null) {
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("received client:{}, message:{}", ctx.channel().id(), jsonObject);
                }
                if ("ping".equals(jsonObject.getString("cmd"))) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"type\":\"pong\",\"code\":0,\"sequence\":" + Instant.now().toEpochMilli() + "}"));
                    return;
                }
                listener.onMessage(ctx.channel(), text);
            } catch (Exception e) {
                logger.error("解析json异常", e);
                JSONObject error = new JSONObject();
                error.put("message", e.getMessage());
                error.put("code", 500);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(error.toJSONString()));
            } finally {
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
        listener.onClose(ctx.channel(), 500, "channelInactive");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //异常时断开连接
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
                    logger.warn("channelId:{} no heartbeat message received in {} seconds", ctx.channel().id(), heart);
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
}
