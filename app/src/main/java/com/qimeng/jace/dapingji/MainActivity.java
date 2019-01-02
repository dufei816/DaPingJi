package com.qimeng.jace.dapingji;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.iprt.android_print_sdk.Table;
import com.iprt.android_print_sdk.usb.USBPrinter;
import com.qimeng.jace.dapingji.entity.Commodity.CommodityEntity;
import com.qimeng.jace.dapingji.entity.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressLint({"HandlerLeak", "CheckResult"})
public class MainActivity extends AppCompatActivity implements Code.CodeListener {

    private static final String TAG = "Print";
    private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
    public static final String ACTION_DEVICE_PERMISSION = "com.demo.USB_PERMISSION";


    @BindView(R.id.content)
    FrameLayout content;


    private USBPrinter usbPrinter;
    private boolean isConnected;

    private UsbManager manager;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onCode(String code) {
        if (code.indexOf("SetBH") != -1) {
            code = code.substring(5);
            MySharedPreferences.putCode(code);
            return;
        }
//        startFragment(new User());
        Observable.just(code)
                .subscribeOn(Schedulers.newThread())
                .filter(str -> str.indexOf("code=") != -1)
                .map(str -> str.substring(str.indexOf("code="), str.indexOf("&card")).split("=")[1])
                .observeOn(Schedulers.io())
                .flatMap(str -> HttpUtil.getInstance().getHttp().getUserDp(str))
                .observeOn(AndroidSchedulers.mainThread())
                .filter(user -> {
                    if (!user.isSuccess()) {
                        Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                })
                .filter(user -> {
                    if (user.getMsg() != 1) {
                        Toast.makeText(this, "请家长扫码绑定用户！", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                })
                .subscribe(user -> {
                    startFragment(user);
                }, error);
    }

    private Consumer<Throwable> error = throwable -> {

    };

    private void startFragment(User user) {
        CommodityFragment fragment = CommodityFragment.newInstance(user);
        fragment.setListener(listener);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                .replace(R.id.content, fragment).commit();
    }

    private CommodityFragment.FragmentListener listener = new CommodityFragment.FragmentListener() {
        @Override
        public void onQuit() {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                    .replace(R.id.content, ImageFragment.newInstance()).commit();
        }

        @Override
        public void onPlay(CommodityEntity entity, User user) {
            showNormalDialog(entity, user);
        }
    };


    private void showNormalDialog(CommodityEntity entity, User user) {
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("选择" + entity.getMc());
        normalDialog.setMessage("是否消耗" + entity.getJf() + "积分兑换？");
        normalDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    HttpUtil.getInstance().getHttp()
                            .getLpdh("123456",
                                    entity.getId() + "",
                                    entity.getJf() + "",
                                    user.getId() + "")
                            .subscribeOn(Schedulers.io())
                            .subscribe(user1 -> {
                                Log.e("Tag", user1.toString());
                            }, error -> {
                                Log.e("Tag", error.getMessage());
                            });
                });
        normalDialog.setNegativeButton("关闭",
                (dialog, which) -> {
                    dialog.dismiss();
                });
        // 显示
        normalDialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPrint();
    }


    private void initPrint() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbPrinter = new USBPrinter(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_DEVICE_PERMISSION);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        usbPrinter.getDeviceList(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case USBPrinter.Handler_Get_Device_List_Found:
                        UsbDevice device = (UsbDevice) msg.obj;
                        String itemName = device.getDeviceName() + "\nVendor Id: " + device.getVendorId() + " Product Id: " + device.getProductId();
                        UsbManager mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
                        if (mUsbManager.hasPermission(device)) {
                            isConnected = usbPrinter.openConnection(device);
                        } else {
                            // 没有权限询问用户是否授予权限
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            mUsbManager.requestPermission(device, pendingIntent); // 该代码执行后，系统弹出一个对话框
                        }
                        break;
                }
            }
        });

        HashMap<String, UsbDevice> list = manager.getDeviceList();
        Observable.fromArray(list)
                .subscribeOn(Schedulers.newThread())
                .flatMap(data -> Observable.fromIterable(new ArrayList<>(data.values())))
                .filter(data -> data.getProductId() == 9930 && data.getVendorId() == 11734)
                .filter(data -> {
                    if (!manager.hasPermission(data)) {
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(ACTION_DEVICE_PERMISSION), 0);
                        manager.requestPermission(data, mPermissionIntent);
                        return false;
                    }
                    return true;
                })
                .subscribe(data -> {
                    Code code = new Code(data, manager);
                    code.setListener(this);
                });
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                .add(R.id.content, ImageFragment.newInstance()).commit();
    }


    public static boolean isConnetction() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process p = runtime.exec("ping -c 3 www.baidu.com");
        int ret = p.waitFor();
        if (ret == 0) {
            return true;
        }
        return false;
    }

    private void printTest() {
        if (isConnected) {
            usbPrinter.init();
            usbPrinter.setCharacterMultiple(0, 0);
            Table table = new Table("Test", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(1, 1);
            table = new Table("1,第一行;	    2.00;    5.00;   10.0q0", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(2, 2);
            table = new Table("2,第二行;   2.00;   5.00;    10.00", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(3, 3);
            table = new Table("3,第三行;   1.00;   68.00;   68.00", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(4, 4);
            table = new Table("4,第四行;   1.00;   4.00;    4.00", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(5, 5);
            table = new Table("5,第五行; 1.00;   5.00;    5.00", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            usbPrinter.setCharacterMultiple(6, 6);
            table = new Table("6,第六行;	    1.00;   2.00;    2.00", ";", new int[]{18, 8, 8, 8});
            usbPrinter.printTable(table);
            //换行
            usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
            //切刀
            usbPrinter.cutPaper();
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(MainActivity.this, action, Toast.LENGTH_SHORT).show();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        isConnected = usbPrinter.openConnection(mUsbDevice);
                    } else {
                        Log.d(TAG, "permission denied for device " + mUsbDevice);
                    }
                }
            } else if (ACTION_DEVICE_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {//已授权
                        Code code = new Code(device, manager);
                        code.setListener(MainActivity.this);
                    }
                } else {
                    Log.d(TAG, "permission denied for device " + device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isConnected && device.equals(usbPrinter.getCurrentDevice())) {
                    usbPrinter.closeConnection();
                    isConnected = false;
                }
            }
        }
    };
}
