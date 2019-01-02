package com.qimeng.jace.dapingji.entity;

import java.util.List;

public class Image {


    private List<Pic> pic;

    public List<Pic> getPic() {
        return pic;
    }

    public void setPic(List<Pic> pic) {
        this.pic = pic;
    }

    @Override
    public String toString() {
        return pic.toString();
    }

    public static class Pic {
        /**
         * pic : cc
         */

        private int res;
        private String pic;


        public Pic(int res, String pic) {
            this.res = res;
            this.pic = pic;
        }

        public int getRes() {
            return res;
        }

        public void setRes(int res) {
            this.res = res;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {

            this.pic = pic;
        }


        @Override
        public String toString() {
            return "res="+res+"/pic="+pic;
        }
    }
}
