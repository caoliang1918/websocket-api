package com.yuntongxun.api.entity;

import java.io.Serializable;


public class Message implements Serializable {

    private String cmd;
    private String code;
    private Object obj;


    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        return "Message{" +
                "cmd='" + cmd + '\'' +
                ", code='" + code + '\'' +
                ", obj=" + obj +
                '}';
    }
}
