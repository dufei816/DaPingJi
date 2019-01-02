package com.qimeng.jace.dapingji.entity;

import java.util.List;

public class Commodity {

    private List<CommodityEntity> lp;

    public List<CommodityEntity> getLp() {
        return lp;
    }

    public void setLp(List<CommodityEntity> lp) {
        this.lp = lp;
    }

    @Override
    public String toString() {
        return lp.toString();
    }

    public static class CommodityEntity {
        /**
         * id : 2
         * mc : bb1
         * jf : 3
         * pic : bb
         */

        private int id;
        private String mc;
        private int jf;
        private String pic;

        public CommodityEntity(String mc, int jf, String pic) {
            this.mc = mc;
            this.jf = jf;
            this.pic = pic;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getMc() {
            return mc;
        }

        public void setMc(String mc) {
            this.mc = mc;
        }

        public int getJf() {
            return jf;
        }

        public void setJf(int jf) {
            this.jf = jf;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {
            this.pic = pic;
        }

        @Override
        public String toString() {
            return "id=" + id + " mc=" + id + " jf=" + jf + " pic=" + pic ;
        }
    }
}
