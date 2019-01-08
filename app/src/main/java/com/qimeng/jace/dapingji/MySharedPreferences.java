package com.qimeng.jace.dapingji;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;

public class MySharedPreferences {

    private static SharedPreferences preferences;
    private static MySharedPreferences mySharedPreferences;
    private static Gson gson;

    public synchronized static MySharedPreferences getInstance() {
        if (preferences == null) {
            mySharedPreferences = new MySharedPreferences();
        }
        return mySharedPreferences;
    }

    private MySharedPreferences() {
        preferences = App.context.getSharedPreferences("MyData", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static void putWiFiName(String ssd) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("WIFI", ssd);
        editor.commit();
    }

    public static String getWiFi() {
        String wifi = preferences.getString("WIFI", "");
        return wifi;
    }


    public static void putCode(String code) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Code", code);
        editor.commit();
    }

    public static String getCode() {
        String code = preferences.getString("Code", "");
        return code;
    }
}
