package org.zhongweixian.listener;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;


public interface ConnectionListener {

    /**
     * Invoked after the connection was closed.
     *
     * @param closeCode the RFC 6455 status code
     * @param reason    a string description for the reason of the close
     */
    void onClose(Channel channel, int closeCode, String reason);

    /**
     * Invoked after an error.
     *
     * @param throwable the cause
     */
    void onError(Throwable throwable);

    /**
     * Invoded after fail
     *
     * @param status
     * @param reason
     */
    void onFail(int status, String reason);


    /**
     * 接受客户端消息
     *
     * @param channel
     * @param text
     */
    void onMessage(Channel channel, String text) throws Exception;

    /**
     * @param channel
     * @param byteBuf
     * @throws Exception
     */
    void onMessage(Channel channel, ByteBuf byteBuf) throws Exception;

    /**
     * 连接
     *
     * @param channel
     */
    void connect(Channel channel) throws Exception;
}
