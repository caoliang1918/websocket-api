package org.zhongweixian.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.zhongweixian.listener.ConnectionListener;


public class WebSocketServerInitializer extends ChannelInitializer<Channel> {

    private SslContext sslContext;

    private Integer heart;
    private String path;

    private ConnectionListener connectionListener;

    public WebSocketServerInitializer(ConnectionListener connectionListener, Integer heart, String path, SslContext sslContext) {
        this.connectionListener = connectionListener;
        this.heart = heart;
        this.path = path;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(channel.alloc()));
        }
        //心跳检测
        if (heart > 0) {
            pipeline.addLast("ping", new IdleStateHandler(heart, 0, 0));
        }
        //HttpServerCodec: 针对http协议进行编解码
        pipeline.addLast(new HttpServerCodec());
        //ChunkedWriteHandler分块写处理
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(65535));
        pipeline.addLast(new WebSocketServerProtocolHandler("/" + path));
        //自定义的处理器
        pipeline.addLast(new WebSocketServerHandler(heart, connectionListener));
    }
}
