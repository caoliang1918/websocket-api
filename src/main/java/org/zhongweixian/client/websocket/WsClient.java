package org.zhongweixian.client.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.client.websocket.handler.WebSocketClientHandler;
import org.zhongweixian.listener.ConnectionListener;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Created by caoliang on 2019-09-25
 * <p>
 * 单实例客户端，支持重连，支持自定义心跳
 */
public class WsClient implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);

    private final URI websocketURI;
    private int port;
    private SslContext sslContext;
    private final MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final Bootstrap bootstrap = new Bootstrap();

    private volatile Channel channel;

    /**
     * 心跳时间
     */
    private Integer heart = 10;

    /**
     * 心跳内容
     */
    private String heartCommand;

    /**
     * 客户端自动重连
     */
    private volatile Boolean autoReConnect = true;

    /**
     * 当前重连次数
     */
    private int tryTimes = 0;

    /**
     * 最大重连次数
     */
    private Integer maxTime = Integer.MAX_VALUE;

    public WsClient(String url, final String payload, final ConnectionListener listener) throws Exception {
        this.websocketURI = new URI(url);
        boolean isSsl = "wss".equalsIgnoreCase(websocketURI.getScheme());
        port = websocketURI.getPort();
        if (isSsl) {
            sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            if (port == -1) {
                port = 443;
            }
        }
        bootstrap.option(ChannelOption.TCP_NODELAY, true).group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslContext != null) {
                            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc(), websocketURI.getHost(), port));
                        }
                        pipeline.addLast("idle", new IdleStateHandler(0, heart, 0));
                        pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192));
                        WebSocketClientHandler clientHandler = new WebSocketClientHandler();
                        clientHandler.setConnectionListener(payload, listener);
                        pipeline.addLast("hookedHandler", clientHandler);
                    }
                });
    }

    private void connect() {
        while (autoReConnect && tryTimes < maxTime) {
            ChannelFuture channelFuture = null;
            try {
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
                channelFuture = bootstrap.connect(websocketURI.getHost(), port).sync();
                if (channelFuture.isSuccess()) {
                    tryTimes = 0;
                    channel = channelFuture.channel();
                    logger.info("channel:{} connected", channel);
                    WebSocketClientHandler clientHandler = (WebSocketClientHandler) channel.pipeline().get("hookedHandler");
                    clientHandler.setHeartCommand(heartCommand);
                    HttpHeaders httpHeaders = new DefaultHttpHeaders();
                    WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13,
                            null, true, httpHeaders);
                    clientHandler.setHandshaker(handshaker);
                    handshaker.handshake(channel);
                    if (StringUtils.isNoneBlank(clientHandler.getPayload())) {
                        channel.writeAndFlush(new TextWebSocketFrame(clientHandler.getPayload()));
                    }
                    channel.closeFuture().sync();
                }
            } catch (Exception e) {
                logger.error("connect {}:{} failed", websocketURI.getHost(), port, e);
            } finally {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            }
            if (!autoReConnect || tryTimes >= maxTime) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(heart);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            logger.info("reconnect {}:{} for {} times", websocketURI.getHost(), port, tryTimes + 1);
            tryTimes++;
        }
    }

    /**
     * 发送消息
     *
     * @param message
     */
    public void sendMessage(String message) {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or clone {}:{}", websocketURI.getHost(), port);
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    /**
     * 回调监听消息是否成功
     *
     * @param message
     */
    public void sendMessageListener(String message) {
        if (channel == null || !channel.isActive()) {
            logger.warn("channel is null or clone {}:{}", websocketURI.getHost(), port);
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(message)).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("send after result:{}", future);
            }
        });
    }

    @Override
    public void run() {
        connect();
    }


    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void close() {
        autoReConnect = false;
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        group.shutdownGracefully();
    }

    public Integer getHeart() {
        return heart;
    }

    public void setHeart(Integer heart) {
        this.heart = heart;
    }

    public String getHeartCommand() {
        return heartCommand;
    }

    public void setHeartCommand(String heartCommand) {
        this.heartCommand = heartCommand;
    }

    public Boolean getAutoReConnect() {
        return autoReConnect;
    }

    public void setAutoReConnect(Boolean autoReConnect) {
        this.autoReConnect = autoReConnect;
    }

    public Integer getMAX_TIME() {
        return maxTime;
    }

    public void setMAX_TIME(Integer maxTime) {
        this.maxTime = maxTime;
    }
}
