package org.zhongweixian.client.tcp.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.client.AuthorizationToken;
import org.zhongweixian.listener.ConnectionListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caoliang on 2019-10-11
 */
@ChannelHandler.Sharable
public class SimpleClientHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(SimpleClientHandler.class);

    private ConnectionListener listener;

    private AuthorizationToken authorizationToken;

    private ScheduledExecutorService executorService = null;

    private Boolean active = false;

    public SimpleClientHandler(ConnectionListener listener, AuthorizationToken authorizationToken) {
        this.listener = listener;
        this.authorizationToken = authorizationToken;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg == null) {
            return;
        }
        try {
            listener.onMessage(ctx.channel(), msg.toString());
        } catch (Exception e) {
            logger.error("read message:{} error:{}", msg, e);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    //100秒没有读到服务端，则认为服务端已经停止
                    logger.warn("response from:{} timeout", ctx.channel().remoteAddress());
                    if (executorService != null) {
                        executorService.shutdownNow();
                    }
                    ctx.channel().close();
                    active = false;
                    break;
                case WRITER_IDLE:
                    //向服务端发送消息
                    ctx.writeAndFlush(authorizationToken.getPing());
                    logger.debug("send ping success,channelId:{}", ctx.channel().id());
                    break;
                case ALL_IDLE:
                    break;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("socket connect success:{}", ctx.channel());
        active = true;
        //客户端和服务端建立连接时调用
        if (StringUtils.isNoneBlank(authorizationToken.getPayload())) {
            ctx.channel().writeAndFlush(authorizationToken.getPayload());
        }
        if (authorizationToken.getTimeHeart()) {
            logger.info("start send ping, timeHeart:{}s, channel:{}", authorizationToken.getHeart(), ctx.channel().remoteAddress());
            if (executorService != null) {
                executorService.shutdownNow();
            }
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(() -> {
                try {
                    if (!active) {
                        logger.info("channel is not active:{}", ctx.channel());
                        return;
                    }
                    ctx.channel().writeAndFlush(authorizationToken.getPing());
                    logger.debug("send ping success,channelId:{}", ctx.channel().id());
                } catch (Exception e) {
                    logger.error("{}", e);
                }
            }, 5, authorizationToken.getHeart(), TimeUnit.SECONDS);
        }
        listener.connect(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //异常时断开连接
        logger.error("exceptionCaught", cause);
        if (executorService != null) {
            logger.info("shutdown thread:{}", ctx.channel());
            executorService.shutdownNow();
        }
        active = false;
        listener.onClose(ctx.channel(), 501, cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.error("socket close, channel active:{}", ctx.channel().isActive());
        active = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        listener.onClose(ctx.channel(), 500, "connect to server close");
        ctx.channel().close();
    }
}
