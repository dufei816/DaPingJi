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
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.iprt.android_print_sdk.Table;
import com.iprt.android_print_sdk.usb.USBPrinter;
import com.qimeng.jace.dapingji.entity.Buy;
import com.qimeng.jace.dapingji.entity.Commodity;
import com.qimeng.jace.dapingji.entity.Commodity.CommodityEntity;
import com.qimeng.jace.dapingji.entity.PrintEntity;
import com.qimeng.jace.dapingji.entity.User;
import com.qimeng.jace.dapingji.util.QRCodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressLint({"HandlerLeak", "CheckResult"})
public class MainActivity extends AppCompatActivity implements Code.CodeListener {

    private static final String TAG = "Print";
    private static final String ACTION_USB_PRINT_PERMISSION = "com.android.usb.USB_PERMISSION";
    public static final String ACTION_DEVICE_PERMISSION = "com.demo.USB_PERMISSION";

    @BindView(R.id.content)
    FrameLayout content;


    private USBPrinter usbPrinter;
    private boolean isConnected;

    private UsbManager manager;
    private CommodityFragment currenCommodityFragment;
    private Thread printThread;
    private boolean printStart = true;
    private LinkedBlockingQueue<PrintEntity> printEntities = new LinkedBlockingQueue<>(30);


    @Override
    protected void onDestroy() {
        super.onDestroy();
        printStart = false;
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
        currenCommodityFragment = CommodityFragment.newInstance(user);
        currenCommodityFragment.setListener(listener);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                .replace(R.id.content, currenCommodityFragment).commit();
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
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(buy -> {
                                if (buy.isSuccess()) {
                                    printBuy(buy, entity, user);
                                    currenCommodityFragment.changeVIew(buy.getJf());
                                } else {
                                    showMessagelDialog("提示", "兑换失败！", ((dialog1, which1) -> dialog.dismiss()));
                                }
                            }, error -> {
                                Log.e("Tag", error.getMessage());
                            });
                });
        normalDialog.setNegativeButton("关闭",
                (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = normalDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    private void showMessagelDialog(String title, String content, DialogInterface.OnClickListener dialog1) {
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle(title);
        normalDialog.setMessage(content);
        normalDialog.setPositiveButton("确定", dialog1);
        AlertDialog dialog = normalDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        content.setOnLongClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            return false;
        });

        printThread = new Thread(() -> {
            while (printStart) {
                try {
                    if (usbPrinter.getPrinterStates() == 0) {
                        PrintEntity entity = printEntities.take();
                        printContent(entity.getBuy(), entity.getEntity(), entity.getUser());
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        init();
    }


    private void printBuy(Buy buy, CommodityEntity entity, User user) {
        /**
         * 0 正常
         * 1 缺纸
         * 2 机开盖
         * 3 通讯异常
         */
        if (isConnected) {
            int states = usbPrinter.getPrinterStates();
            switch (states) {
                case 1:
                    showMessagelDialog("提示", "打印机缺纸请工作人员换纸！", (dialog, which) -> {
                        dialog.dismiss();
                    });
                    break;
                case 2:
                    showMessagelDialog("提示", "打印机盖未盖好，请仔细检查打印机！", ((dialog, which) -> {
                        dialog.dismiss();
                    }));
                    break;
                case 3:
                    initPrint();
                    break;
            }
        }
        Observable.just(new PrintEntity(entity, user, buy)).subscribeOn(Schedulers.newThread()).subscribe(print -> printEntities.put(print));
    }

    private void printContent(Buy buy, CommodityEntity entity, User user) {
        usbPrinter.init();
        usbPrinter.setCharacterMultiple(2, 2);
        usbPrinter.printText("    勤盟互动\n");
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.setCharacterMultiple(1, 1);
        usbPrinter.printText("礼品兑换凭证\n");
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.setCharacterMultiple(0, 0);
        usbPrinter.printText("兑换人：" + user.getXm());
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.printText("\n");
        usbPrinter.printText("兑换品：" + entity.getMc());
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.printText("\n");
        usbPrinter.printText("积分消耗：" + entity.getJf() + "积分");
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.printText("凭证号:" + buy.getDdh());
        usbPrinter.printText("\n");
        usbPrinter.printImage(QRCodeUtil.createQRCodeBitmap(buy.getDdh(), 240, 240), 150);
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.printText("\n");
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.cutPaper();
        return;
    }

    private void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_PERMISSION);
        filter.addAction(ACTION_USB_PRINT_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        initPrint();
        initCode();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                .add(R.id.content, ImageFragment.newInstance()).commit();
    }

    private void initPrint() {
        HashMap<String, UsbDevice> list = manager.getDeviceList();
        Observable.fromArray(list)
                .subscribeOn(Schedulers.newThread())
                .flatMap(data -> Observable.fromIterable(new ArrayList<>(data.values())))
                .filter(data -> data.getProductId() == 22304 && data.getVendorId() == 1155)
                .filter(data -> {
                    if (!manager.hasPermission(data)) {
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(ACTION_USB_PRINT_PERMISSION), 0);
                        manager.requestPermission(data, mPermissionIntent);
                        return false;
                    }
                    return true;
                })
                .subscribe(data -> {
                    usbPrinter = new USBPrinter(this);
                    isConnected = usbPrinter.openConnection(data);
                    printThread.start();
                });
    }

    private void initCode() {
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


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(MainActivity.this, action, Toast.LENGTH_SHORT).show();
            if (ACTION_USB_PRINT_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        isConnected = usbPrinter.openConnection(mUsbDevice);
                        printThread.start();
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
            }
        }
    };
}
