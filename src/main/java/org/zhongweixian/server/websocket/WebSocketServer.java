package org.zhongweixian.server.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * websocket服务启动类
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final Integer port;
    private final Integer heart;
    private final ConnectionListener connectionListener;
    private final String path;
    private final Integer bossGroupSize;
    private final Integer workerGroupSize;

    private MultiThreadIoEventLoopGroup bossGroup = null;
    private MultiThreadIoEventLoopGroup workerGroup = null;

    public WebSocketServer(int port, ConnectionListener connectionListener) {
        this(port, 60, "ws", 1, 4, connectionListener);
    }

    public WebSocketServer(int port, String path, ConnectionListener connectionListener) {
        this(port, 60, path, 1, 4, connectionListener);
    }

    public WebSocketServer(int port, Integer heart, ConnectionListener connectionListener) {
        this(port, heart, "ws", 1, 4, connectionListener);
    }

    public WebSocketServer(int port, Integer heart, String path, ConnectionListener connectionListener) {
        this(port, heart, path, 1, 4, connectionListener);
    }

    public WebSocketServer(int port, Integer heart, String path, Integer parentGroupSize, Integer childGroupSize, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.path = path;
        this.bossGroupSize = parentGroupSize;
        this.workerGroupSize = childGroupSize;
        this.connectionListener = connectionListener;
    }

    public void start() {
        bossGroup = new MultiThreadIoEventLoopGroup(bossGroupSize, new DefaultThreadFactory("ws-boss-group", true), NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(workerGroupSize, new DefaultThreadFactory("ws-worker-group", true), NioIoHandler.newFactory());
        boolean bindSuccess = false;
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast("idle", new IdleStateHandler(heart, 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new WebSocketServerHandler(heart, connectionListener));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/" + path, null, true, 65535 * 10));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind().sync();
            bindSuccess = channelFuture.isSuccess();
            if (bindSuccess) {
                logger.info("websocket started on port:{}, path:{}", port, path);
            }
        } catch (Exception e) {
            logger.error("websocket server start failed, port:{}, path:{}", port, path, e);
        } finally {
            if (!bindSuccess) {
                close();
            }
        }
    }

    public void close() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }


}
