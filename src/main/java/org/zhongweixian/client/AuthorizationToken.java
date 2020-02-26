package org.zhongweixian.client;

/**
 * Created by caoliang on 2020-02-25
 */
public class AuthorizationToken {

    private String ping = "{\"cmd\":\"ping\",\"cts\":" + System.currentTimeMillis() + "}";

    private String payload;


    public String getPing() {
        return ping;
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
}
