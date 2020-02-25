package org.zhongweixian.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.decode.MessageDecoder;
import org.zhongweixian.decode.MessageEncoder;
import org.zhongweixian.listener.ConnectionListener;

import java.net.InetSocketAddress;

/**
 * Created by caoliang on 2020-02-24
 */
public class NettyServer {
    private Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workGroup = null;

    private Integer parentGroupSize = 2;

    private Integer childGroupSize = 4;


    private Integer port;

    private Integer heart = 60;

    private ConnectionListener connectionListener;


    public NettyServer(int port, ConnectionListener connectionListener) {
        this.port = port;
        this.connectionListener = connectionListener;
    }

    public NettyServer(int port, Integer heart, Integer parentGroupSize, Integer childGroupSize, ConnectionListener connectionListener) {
        this.port = port;
        this.heart = heart;
        this.parentGroupSize = parentGroupSize;
        this.childGroupSize = childGroupSize;
        this.connectionListener = connectionListener;
    }

    public void start() {
        bossGroup = new NioEventLoopGroup(parentGroupSize);
        workGroup = new NioEventLoopGroup(childGroupSize);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        try {
            serverBootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new IdleStateHandler(heart, 0, 0))
                                    .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 4))
                                    .addLast("decoder", new MessageDecoder())
                                    .addLast("encoder", new MessageEncoder())
                                    .addLast(new NettyServerHandler(connectionListener));

                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            if (channelFuture.isSuccess()) {
                logger.info("netty server start , port :{}", port);
            }
        } catch (Exception e) {
            logger.error("{}", e);
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
