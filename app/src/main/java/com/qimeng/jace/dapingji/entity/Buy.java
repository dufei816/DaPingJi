package com.qimeng.jace.dapingji.entity;

public class Buy {


    /**
     * success : true
     * ddh : 20190102150443431
     */

    private boolean success;
    private String ddh;
    /**
     * jf : 3
     */

    private int jf;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDdh() {
        return ddh;
    }

    public void setDdh(String ddh) {
        this.ddh = ddh;
    }

    public int getJf() {
        return jf;
    }

    public void setJf(int jf) {
        this.jf = jf;
    }
}
