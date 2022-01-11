package org.zhongweixian.server.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;

import java.net.InetSocketAddress;

/**
 * websocket服务启动类
 */
public class WebSocketServer {
    private Logger logger = LoggerFactory.getLogger(WebSocketServer.class);


    private Integer port;
    private Integer heart = 60;
    private ConnectionListener connectionListener;
    private String path = "ws";
    private Integer parentGroupSize = 2;
    private Integer childGroupSize = 4;

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workGroup = null;

    public WebSocketServer(int port, ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        this.port = port;
    }

    public WebSocketServer(int port, String path, ConnectionListener connectionListener) {
        this.port = port;
        this.path = path;
        this.connectionListener = connectionListener;
    }


    public WebSocketServer(int port, Integer heart, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.connectionListener = connectionListener;
    }

    public WebSocketServer(int port, Integer heart, String path, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.path = path;
        this.connectionListener = connectionListener;
    }

    public WebSocketServer(int port, Integer heart, String path, Integer parentGroupSize, Integer childGroupSize, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.path = path;
        this.parentGroupSize = parentGroupSize;
        this.childGroupSize = childGroupSize;
        this.connectionListener = connectionListener;
    }

    public void start() {
        bossGroup = new NioEventLoopGroup(parentGroupSize);
        workGroup = new NioEventLoopGroup(childGroupSize);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            /*SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();*/
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("idle", new IdleStateHandler(heart, 0, 0));
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new WebSocketServerHandler(heart, connectionListener));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/" + path, null, true, 65535 * 10));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind().sync();
            if (channelFuture.isSuccess()) {
                logger.info("websocket started on port:{}, path:{}", port, path);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void close() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
    }


}
