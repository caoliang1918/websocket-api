package org.zhongweixian.server.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhongweixian.listener.ConnectionListener;

import java.net.InetSocketAddress;

/**
 * Created by caoliang on 2020-04-13
 */
public class UdpServer {
    private Logger logger = LoggerFactory.getLogger(UdpServer.class);

    private EventLoopGroup workGroup = null;


    private Integer port;

    private Integer heart = 60;

    private ConnectionListener connectionListener;

    public UdpServer(int port, ConnectionListener connectionListener) {
        this.port = port;
        this.connectionListener = connectionListener;
    }


    public void start() {
        workGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(workGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) throws Exception {
                            //具体怎么拆包？
                            ByteBuf byteBuf = datagramPacket.content();
                            //byteBuf.skipBytes(length);
                            connectionListener.onMessage(ctx.channel(), byteBuf);
                        }


                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            logger.info("channelInactive channel:{}", ctx.channel().id());
                            connectionListener.onClose(ctx.channel(), 500, "socket close");
                            ctx.fireChannelInactive();
                        }

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            logger.info("channelActive channel:{}", ctx.channel().id());
                            connectionListener.connect(ctx.channel());
                            ctx.fireChannelActive();
                        }
                    });

            InetSocketAddress socketAddress = new InetSocketAddress(port);
            bootstrap.bind(socketAddress);
        } catch (Exception e) {
            logger.error("{}", e);
        }

    }

    public void close() {
        workGroup.shutdownGracefully();
    }

}
