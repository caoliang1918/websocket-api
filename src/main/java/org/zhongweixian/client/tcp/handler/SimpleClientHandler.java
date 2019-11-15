package org.zhongweixian.client.tcp.handler;

import org.zhongweixian.listener.ConnectionListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caoliang on 2019-10-11
 */
@ChannelHandler.Sharable
public class SimpleClientHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(SimpleClientHandler.class);

    private ConnectionListener listener;

    public SimpleClientHandler(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            listener.onMessage(ctx.channel(), msg.toString());
        } catch (Exception e) {
            logger.error("read message error:{}", msg);
        }
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
                    ctx.writeAndFlush("{'cmd':'ping' , 'cts':'\" + System.currentTimeMillis() + \"'}");
                    logger.debug("send ping success!");
                    break;
                case ALL_IDLE:
                    break;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //客户端和服务端建立连接时调用
        ctx.fireChannelActive();
        listener.connect(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //异常时断开连接
        logger.error("异常断开:{}", cause);
        listener.onClose(ctx.channel(), 500, cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.error("连接中断, channel active : {}", ctx.channel().isActive());
        listener.onClose(ctx.channel(), 505, "connect to server close");
        ctx.channel().close();
    }
}
