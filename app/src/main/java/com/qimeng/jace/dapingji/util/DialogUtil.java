package com.qimeng.jace.dapingji.util;

import android.app.AlertDialog;
import android.content.Context;

public class DialogUtil {

    private static int currentItem = 0;



    public static void showSingleChoiceDialog(Context context, String[] items) {
        currentItem = 0;
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(context);
        singleChoiceDialog.setTitle("请选择wifi");
        // 第二个参数是默认选项，此处设置为0
        singleChoiceDialog.setSingleChoiceItems(items, currentItem,
                (dialog, which) -> {
                    currentItem = which;
                });
        singleChoiceDialog.setPositiveButton("确定",
                (dialog, which) -> {

                });
        singleChoiceDialog.show();
    }


    public interface ItemListener {

        void onItem(int itemId);

    }
}
