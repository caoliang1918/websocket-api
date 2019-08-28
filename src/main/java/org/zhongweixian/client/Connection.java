package org.zhongweixian.client;


public interface Connection {

    void close();

    void sendText(String payload);

    void sendByte(byte[] bytes);

    void sendPing();

    String getId();

    boolean isActive();

}
