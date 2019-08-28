# websocket-client


[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.zhongweixian/websocket-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.zhongweixian/websocket-client/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) 


## Getting it
`websocket-api` requires Java 8 or + to run.

With `Maven Central` repositories (stable releases only):

```xml
    <dependency>
        <groupId>org.zhongweixian</groupId>
        <artifactId>websocket-api</artifactId>
        <version>1.0.0</version>
    </dependency>
```

## start server

```java
 WebSocketServer webSocketServer = new WebSocketServer(9599, 60, "ws", new ConnectionListener() {
            @Override
            public void connect(Channel channel) throws Exception {
                logger.info("channel {} is connect", channel.id());
            }

            @Override
            public void onClose(Channel channel, int closeCode, String reason) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onFail(int status, String reason) {
                logger.info("status:{} , reason:{}", status, reason);
            }

            @Override
            public void onMessage(Channel channel, String text) throws Exception {
                logger.info("channel {} , receive message:{}", channel.id(), text);
            }

            @Override
            public void onMessage(Channel channel, ByteBuf byteBuf) throws Exception {
                logger.info("");
            }
        });
        webSocketServer.start();

```

##start client
```java

/**
         * 启动ws客户端
         */
        WebSocketClient client = new WebSocketClient("ws://192.168.181.178:8190/ws");


        for (int i = 0; i < 10; i++) {
            client.connection(new ConnectionListener() {
                @Override
                public void onClose(Channel channel, int closeCode, String reason) {
                    logger.warn("channelId:{} , closeCode:{} , reason:{}", channel.id(), closeCode, reason);
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.warn("onError:{}", throwable.getMessage());
                }

                @Override
                public void onFail(int status, String reason) {

                }

                @Override
                public void onMessage(Channel channel, String text) throws Exception {
                    logger.info("client:{} onMessage:{}", channel.id(), text);
                }

                @Override
                public void onMessage(Channel channel, ByteBuf byteBuf) throws Exception {
                    logger.info("client:{} onMessage:{}", channel.id(), byteBuf.toString(Charset.defaultCharset()));
                }

                @Override
                public void connect(Channel channel) throws Exception {
                    logger.info("client:{} connect", channel.id());
                }
            });
            Thread.sleep(10);
        }
```