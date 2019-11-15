package org.zhongweixian.client.websocket;


public interface Connection {

    void close();

    void sendText(String payload);

    void sendByte(byte[] bytes);

    void sendPing();

    String getId();

    boolean isActive();

}
