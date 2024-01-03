package org.zhongweixian.client.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
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

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 支持批量websocket链接
 */
public class WebSocketClient {
    private Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private URI websocketURI;
    private int port;
    private SslContext sslContext;
    private EventLoopGroup group = null;
    private Bootstrap bootstrap = null;
    private Channel channel;

    private static final int HEART_TIME = 10;

    public WebSocketClient(String url, Integer threads) throws URISyntaxException, SSLException {

        if (threads == null || threads < 0) {
            threads = 0;
        }
        group = new NioEventLoopGroup(threads);
        bootstrap = new Bootstrap();

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
                        pipeline.addLast("idle", new IdleStateHandler(0, HEART_TIME, 0));
                        pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192));
                        pipeline.addLast("hookedHandler", new WebSocketClientHandler());
                    }
                });
    }

    public Connection connection(ConnectionListener listener) throws Exception {
        return connection(listener, null, 5000);
    }

    /**
     * @param listener          监听接口
     * @param payload           认证的json
     * @param connectionTimeout 超时毫秒数
     * @return
     * @throws Exception
     */
    public Connection connection(ConnectionListener listener, String payload, int connectionTimeout) throws Exception {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout);
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13,
                null, true, httpHeaders, 196608);
        Channel channel = bootstrap.connect(websocketURI.getHost(), port).sync().channel();
        WebSocketClientHandler clientHandler = (WebSocketClientHandler) channel.pipeline().get("hookedHandler");
        clientHandler.setConnectionListener(payload, listener);
        clientHandler.setHandshaker(handshaker);
        handshaker.handshake(channel);
        //可以使用ChannelFuture来做断开检测
        ChannelFuture channelFuture = clientHandler.handshakeFuture().sync();
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("channel:{} connected , channelFuture result:{}", channel, channelFuture.isSuccess());
                if (StringUtils.isNoneBlank(payload)) {
                    channel.writeAndFlush(new TextWebSocketFrame(payload));
                }
            }
        });
        this.channel = channel;
        return new WsConnection(channel);
    }


    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
            group.shutdownGracefully();
        }
    }
}
