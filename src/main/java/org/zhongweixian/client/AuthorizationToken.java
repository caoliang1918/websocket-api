package org.zhongweixian.client;

/**
 * Created by caoliang on 2020-02-25
 */
public class AuthorizationToken {

    private String ping;

    private String payload;

    /**
     * 默认心跳10秒
     */
    private Integer heart = 10;

    /**
     * 是否是定时心跳，默认是没有业务消息则发心跳
     */
    private Boolean timeHeart = false;

    /**
     * 服务端返回消息超时
     */
    private Integer pongTimeout = 100;

    /**
     * 定义线程名称
     */
    private String threadName;

    /**
     * 线程数量
     */
    private Integer threadNums;


    public String getPing() {
        return ping == null ? "{\"cmd\":\"ping\",\"cts\":" + System.currentTimeMillis() + "}" : ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getHeart() {
        return heart;
    }

    public void setHeart(Integer heart) {
        this.heart = heart;
    }

    public Boolean getTimeHeart() {
        return timeHeart;
    }

    public void setTimeHeart(Boolean timeHeart) {
        this.timeHeart = timeHeart;
    }

    public Integer getPongTimeout() {
        return pongTimeout;
    }

    public void setPongTimeout(Integer pongTimeout) {
        this.pongTimeout = pongTimeout;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public Integer getThreadNums() {
        return threadNums;
    }

    public void setThreadNums(Integer threadNums) {
        this.threadNums = threadNums;
    }
}
