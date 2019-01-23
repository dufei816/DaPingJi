package com.qimeng.jace.dapingji;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;

public class App extends Application {

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        MySharedPreferences.getInstance();
//        new Thread(() -> {
//            Glide.get(this).clearDiskCache();
//        }).start();
//        Glide.get(this).clearMemory();
    }
}
