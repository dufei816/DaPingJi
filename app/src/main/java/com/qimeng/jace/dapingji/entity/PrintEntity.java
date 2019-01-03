package com.qimeng.jace.dapingji.entity;

public class PrintEntity {

    private Commodity.CommodityEntity entity;
    private User user;
    private Buy buy;

    public PrintEntity(Commodity.CommodityEntity entity, User user, Buy buy) {
        this.entity = entity;
        this.user = user;
        this.buy = buy;
    }

    public Buy getBuy() {
        return buy;
    }

    public void setBuy(Buy buy) {
        this.buy = buy;
    }

    public Commodity.CommodityEntity getEntity() {
        return entity;
    }

    public void setEntity(Commodity.CommodityEntity entity) {
        this.entity = entity;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
