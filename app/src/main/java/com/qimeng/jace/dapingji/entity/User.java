package com.qimeng.jace.dapingji.entity;

import java.io.Serializable;

public class User implements Serializable {


    /**
     * id : 1
     * jf : 2
     * xm : 张培轩
     * msg : 1
     * success : true
     */

    private int id;
    private int jf;
    private String xm;
    private int msg;
    private boolean success;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJf() {
        return jf;
    }

    public void setJf(int jf) {
        this.jf = jf;
    }

    public String getXm() {
        return xm;
    }

    public void setXm(String xm) {
        this.xm = xm;
    }

    public int getMsg() {
        return msg;
    }

    public void setMsg(int msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
