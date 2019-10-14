package com.yuntongxun.api.client.tcp;

import com.yuntongxun.api.client.tcp.handler.SimpleClientHandler;
import com.yuntongxun.api.listener.ConnectionListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caoliang on 2019-10-11
 */
public class NettyClient implements Runnable {
    private Logger logger = LoggerFactory.getLogger(NettyClient.class);

    /**
     * 连接地址
     */
    private String host;

    /**
     * 连接ip
     */
    private Integer port;
    /**
     * 心跳时间
     */
    private Integer heart = 10;

    /**
     * 客户端自动重连
     */
    private Boolean autoConnect = true;

    /**
     * 当前重连次数
     */
    private AtomicInteger TRY_TIMES = new AtomicInteger(0);

    /**
     * 最大重连次数
     */
    private Integer MAX_TIME = Integer.MAX_VALUE;


    private Channel channel;


    private ConnectionListener listener;

    /**
     * 自定义编解码
     */
    private ChannelHandler[] channelHandlers;


    Bootstrap bootstrap = null;
    EventLoopGroup group = null;
    ChannelFuture channelFuture = null;


    public NettyClient(final String host, final Integer port, ConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        init();
    }

    public NettyClient(final String host, final Integer port, ChannelHandler[] channelHandlers, ConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.channelHandlers = channelHandlers;
        this.listener = listener;
        init();
    }


    private void init() {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, heart, 0))
                                //.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 4))
                                .addLast(new SimpleClientHandler(listener));
                        if (ArrayUtils.isNotEmpty(channelHandlers)) {
                            //自定义的handler
                            pipeline.addLast(channelHandlers);
                        }

                    }
                });
        new Thread(this).start();
    }

    @Override
    public void run() {
        connect();
    }

    private void connect() {
        try {
            channelFuture = bootstrap.connect(host, port).sync();
            if (channelFuture.isSuccess()) {
                logger.info("netty client connect {}:{} success", host, port);
                TRY_TIMES = new AtomicInteger(1);
            }
            channel = channelFuture.channel();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("{}", e);
        } finally {
            if (channelFuture != null) {
                if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                    channelFuture.channel().close();
                }
            }
            if (!autoConnect || TRY_TIMES.get() >= MAX_TIME) {
                return;
            }
            try {
                Thread.sleep(heart * 1000);
            } catch (InterruptedException e) {
                logger.error("{}", e);
            }
            logger.info("准备重连 {}:{} 第 {} 次", host, port, TRY_TIMES.getAndIncrement());
            connect();
        }
    }


    /**
     * 给服务端发送消息
     *
     * @param message
     */
    public void sendMessage(final Object message) {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or clone {}:{}", host, port);
            return;
        }
        channel.writeAndFlush(message);
    }

    /**
     * 给服务端发送消息，异步通知结果；这里只是打印日志，需要拿到异步结果，需要重写接口实现。
     *
     * @param message
     */
    public void sendMessageListener(final Object message) {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or clone {}:{}", host, port);
            return;
        }
        channel.writeAndFlush(message).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("send after result:{}", future);
            }
        });
    }

    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        autoConnect = false;
        logger.info("client close {}:{} , autoConnect:{}", host, port, autoConnect);
    }

    public Boolean getAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(Boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void setMaxReConnect(Integer maxReConnect) {
        this.MAX_TIME = maxReConnect;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
