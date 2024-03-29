package org.zhongweixian.server.tcp;

import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;

import java.util.Date;

/**
 * 服务器监听处理器
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * 消息接口
     */
    private ConnectionListener connectionListener;

    public NettyServerHandler(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    /**
     * 绑定
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        logger.info("client connect :{}", byteBuf.toString(CharsetUtil.UTF_8));
    }


    /**
     * 取消绑定
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelInactive channleId:{}", ctx.channel().id());
        try {
            connectionListener.onClose(ctx.channel(), 500, "channelInactive");
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("channelActive channleId:{}", ctx.channel().id());
        try {
            connectionListener.connect(ctx.channel());
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (msg != null) {
            connectionListener.onMessage(ctx.channel(), msg.toString());
        }
    }

    /**
     * 心跳机制  用户事件触发
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case WRITER_IDLE:

                    break;
                case READER_IDLE:
                    logger.warn("No heartbeat message received in 60 seconds");
                    //向客户端发送心跳消息
                    JSONObject messageProtocol = new JSONObject();
                    messageProtocol.put("id", 1L);
                    messageProtocol.put("type", "timeout");
                    messageProtocol.put("message", "No heartbeat message received in 60 seconds");
                    messageProtocol.put("cts", new Date());
                    ByteBuf byteBuf = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(messageProtocol.toString(), CharsetUtil.UTF_8));
                    ctx.writeAndFlush(byteBuf).addListener(ChannelFutureListener.CLOSE);
                    ctx.close();

            }
        }
        super.userEventTriggered(ctx, evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("exceptionCaught:{}", cause);
        connectionListener.onClose(ctx.channel(), 501, cause.getMessage());

    }
}
