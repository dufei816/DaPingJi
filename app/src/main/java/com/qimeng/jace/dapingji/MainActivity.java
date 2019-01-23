package com.qimeng.jace.dapingji;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.iprt.android_print_sdk.usb.USBPrinter;
import com.qimeng.jace.dapingji.entity.Buy;
import com.qimeng.jace.dapingji.entity.Commodity.CommodityEntity;
import com.qimeng.jace.dapingji.entity.PrintEntity;
import com.qimeng.jace.dapingji.entity.User;
import com.qimeng.jace.dapingji.util.MainHandlerConstant;
import com.qimeng.jace.dapingji.util.QRCodeUtil;
import com.qimeng.jace.dapingji.util.TTSUtil;
import com.qimeng.jace.dapingji.util.WiFiUtil;

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
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;
import static android.net.wifi.WifiManager.SUPPLICANT_STATE_CHANGED_ACTION;

@SuppressLint({"HandlerLeak", "CheckResult", "WifiManagerLeak"})
public class MainActivity extends AppCompatActivity implements Code.CodeListener {

    @BindView(R.id.content)
    FrameLayout content;


    private static final int CODE_FOR_WRITE_PERMISSION = 100;

    private static final String TAG = "MainActivity";

    private static final String ACTION_USB_PRINT_PERMISSION = "com.android.usb.USB_PERMISSION";
    public static final String ACTION_DEVICE_PERMISSION = "com.demo.USB_PERMISSION";

    private USBPrinter usbPrinter;
    private boolean isConnected;
    private TTSUtil ttsUtil;

    private UsbManager usbManager;
    private CommodityFragment currenCommodityFragment;
    private Thread printThread;
    private boolean printStart = true;
    private LinkedBlockingQueue<PrintEntity> printEntities = new LinkedBlockingQueue<>(30);

    private ProgressDialog waitingDialog;
    private Disposable initObj;
    private Disposable wifiCheck;


    private int currentWiFiItem = 0;
    private WiFiUtil wiFiUtil;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        printStart = false;
        unregisterReceiver(mUsbReceiver);
    }

    private Handler initHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainHandlerConstant.INIT_SUCCESS:
                    initCode();
                    break;
                case MainHandlerConstant.INIT_FAIL:

                    break;
            }
        }
    };


    private void showButton() {
        Intent intent = new Intent("android.intent.action.nvigationbar_show");
        sendBroadcast(intent);
    }

    public void hideButton() {
        Intent intent = new Intent("android.intent.action.nvigationbar_hide");
        sendBroadcast(intent);
    }


    @Override
    public void onError() {
        ttsUtil.speak("扫码器连接失败");
    }

    @Override
    public void onSuccess() {
        ttsUtil.speak("扫码器连接完成");
        initPrint();
        hideButton();
    }

    @Override
    public void onCode(String code) {
        if (code.indexOf("HEXIAO") != -1) {
            String dingdan = code.substring(6);
            runOnUiThread(() -> {
                showMessagelDialog("提示", "是否核销该订单？", ((dialogInterface, i) -> dialogInterface.dismiss()), (dialogInterface, i) -> {
                    hexiao(dingdan);
                    dialogInterface.dismiss();
                });
            });
        }
        if (code.indexOf("SetBH") != -1) {
            code = code.substring(5);
            MySharedPreferences.putCode(code);
            runOnUiThread(() -> Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show());
            return;
        }
        if (code.equals("uihaoguhasnoiuhnoreiuhdfg")) {
            showButton();
            return;
        }
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
                }, error -> Log.e(TAG, error.getLocalizedMessage()));
    }

    private void hexiao(String code) {
        HttpUtil.getInstance().getHttp().hxdd(code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (data.isSuccess()) {
                        showMessagelDialog("提示", "核销成功", null, (dialog, i) -> {
                            dialog.dismiss();
                        });
                    } else {
                        showMessagelDialog("提示", "核销失败", null, (dialog, i) -> {
                            dialog.dismiss();
                        });
                    }
                }, error -> {
                    showMessagelDialog("提示", "网络请求失败", null, (dialog, i) -> {
                        dialog.dismiss();
                    });
                });
    }

    private void startFragment(User user) {
        if (currenCommodityFragment == null) {
            currenCommodityFragment = CommodityFragment.newInstance(user);
            currenCommodityFragment.setListener(listener);
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                    .replace(R.id.content, currenCommodityFragment).commit();
        }
    }

    private CommodityFragment.FragmentListener listener = new CommodityFragment.FragmentListener() {
        @Override
        public void onQuit() {
            currenCommodityFragment = null;
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
                            .getLpdh(MySharedPreferences.getCode(),
                                    entity.getId() + "",
                                    entity.getJf() + "",
                                    user.getId() + "")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(buy -> {
                                if (buy.isSuccess()) {
                                    ttsUtil.speak("兑换成功,如果不继续兑换请退出，防止积分丢失！");
                                    printBuy(buy, entity, user);
                                    if (currenCommodityFragment != null) {
                                        currenCommodityFragment.changeVIew(buy.getJf());
                                    }
                                } else {
                                    showMessagelDialog("提示", "兑换失败！", null, ((dialog1, which1) -> dialog.dismiss()));
                                }
                            }, error -> Log.e(TAG, error.getMessage()));
                });
        normalDialog.setNegativeButton("关闭",
                (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = normalDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    private void showMessagelDialog(String title, String content, DialogInterface.OnClickListener dialog1, DialogInterface.OnClickListener dialog2) {
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle(title);
        normalDialog.setMessage(content);
        if (dialog1 != null) {
            normalDialog.setNegativeButton("取消", dialog1);
        }
        normalDialog.setPositiveButton("确定", dialog2);
        AlertDialog dialog = normalDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_FOR_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ttsUtil = TTSUtil.getInstance(initHandler);
            }
        }
    }

    private boolean checkNetworkInfo() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo networkinfo = manager.getActiveNetworkInfo();
        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }
        return true;
    }

    /**
     * 检测wifi是否打开
     */
    private void checkWiFi() {
        if (checkNetworkInfo()) {
            checkConnect();
        } else {
            if (wiFiUtil.checkState() == WifiManager.WIFI_STATE_DISABLED) {
                wiFiUtil.openWifi();
            }
            wifiCheck = Observable.interval(5, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(l -> {
                        if (wiFiUtil.checkState() == WifiManager.WIFI_STATE_ENABLED) {
                            if (checkNetworkInfo()) {
                                checkConnect();
                            } else {
                                openWiFiList();
                            }
                            wifiCheck.dispose();
                        }
                    });
        }
    }

    private void openWiFiList() {
        wiFiUtil.creatWifiLock("MyJace");
        wiFiUtil.startScan();
        List<ScanResult> list = wiFiUtil.getWifiList();
        String[] strs = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            strs[i] = list.get(i).SSID;
        }
        showSingleChoiceDialog(this, strs);
    }

    /**
     * 连接wifi
     *
     * @param item
     * @param password
     */
    private void connectWiFi(ScanResult item, String password) {
        if (item.capabilities.contains("WPA2") || item.capabilities.contains("WPA-PSK")) {
            wiFiUtil.addWiFiNetwork(item.SSID, password, WiFiUtil.Data.WIFI_CIPHER_WPA2);
        } else if (item.capabilities.contains("WPA")) {
            wiFiUtil.addWiFiNetwork(item.SSID, password, WiFiUtil.Data.WIFI_CIPHER_WPA);
        } else if (item.capabilities.contains("WEP")) {
            wiFiUtil.addWiFiNetwork(item.SSID, password, WiFiUtil.Data.WIFI_CIPHER_WEP);
        } else {
            wiFiUtil.addWiFiNetwork(item.SSID, "", WiFiUtil.Data.WIFI_CIPHER_NOPASS);
        }
    }

    /**
     * wifi列表
     *
     * @param context
     * @param items
     */
    private void showSingleChoiceDialog(Context context, String[] items) {
        currentWiFiItem = 0;
        AlertDialog.Builder singleChoiceDialog =
                new AlertDialog.Builder(context);
        singleChoiceDialog.setTitle("请选择wifi");
        // 第二个参数是默认选项，此处设置为0
        singleChoiceDialog.setSingleChoiceItems(items, currentWiFiItem,
                (dialog, which) -> {
                    currentWiFiItem = which;
                });
        singleChoiceDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    dialog.dismiss();
                    showInputDialog();
                });
        singleChoiceDialog.show();
    }


    private void showInputDialog() {
        EditText editText = new EditText(MainActivity.this);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("请输入wifi密码").setView(editText);
        editText.setHint("请输入");
        inputDialog.setPositiveButton("确定", (dialog, which) -> {
            String pwd = editText.getText().toString();
            ScanResult item = wiFiUtil.getWifiList().get(currentWiFiItem);
            MySharedPreferences.putWiFiName(item.SSID);
            connectWiFi(item, pwd);
            dialog.dismiss();
        });
        inputDialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_PERMISSION);
        filter.addAction(ACTION_USB_PRINT_PERMISSION);
        filter.addAction(SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mUsbReceiver, filter);

        showButton();

        checkConnect();

//        wiFiUtil = WiFiUtil.getInstance(this);
//        checkWiFi();
//        openWiFiList();
    }

    public boolean isConnetction() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process p = runtime.exec("ping -c 3 www.baidu.com");
        int ret = p.waitFor();
        if (ret == 0) {
            return true;
        }
        return false;
    }


    private void showWaitingDialog() {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            return;
        }
        /* 等待Dialog具有屏蔽其他控件的交互能力
         * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
         * 下载等事件完成后，主动调用函数关闭该Dialog
         */
        waitingDialog = new ProgressDialog(MainActivity.this);
        waitingDialog.setTitle("正在初始化");
        waitingDialog.setMessage("请稍后···");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);
        waitingDialog.show();
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
                    showMessagelDialog("提示", "打印机缺纸请工作人员换纸！", null, (dialog, which) -> {
                        dialog.dismiss();
                    });
                    break;
                case 2:
                    showMessagelDialog("提示", "打印机盖未盖好，请仔细检查打印机！", null, ((dialog, which) -> {
                        dialog.dismiss();
                    }));
                    break;
                case 3:
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
        usbPrinter.printImage(QRCodeUtil.createQRCodeBitmap("HEXIAO" + buy.getDdh(), 240), 150);
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.printText("\n");
        usbPrinter.setPrinter(USBPrinter.COMM_PRINT_AND_NEWLINE);
        usbPrinter.cutPaper();
        return;
    }

    private void init() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        int hasWriteStoragePermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
            ttsUtil = TTSUtil.getInstance(initHandler);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    CODE_FOR_WRITE_PERMISSION);
        }

        printThread = new Thread(() -> {
            while (printStart) {
                try {
                    if (usbPrinter.getPrinterStates() == 0) {
                        PrintEntity entity = printEntities.take();
                        if (usbPrinter.getPrinterStates() == 0) {
                            printContent(entity.getBuy(), entity.getEntity(), entity.getUser());
                        } else {
                            printEntities.put(entity);
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out)
                .add(R.id.content, ImageFragment.newInstance()).commit();
    }


    private void initPrint() {
        if (usbPrinter == null) {
            usbPrinter = new USBPrinter(this);
        }
        HashMap<String, UsbDevice> list = usbManager.getDeviceList();
        Observable.fromArray(list)
                .subscribeOn(Schedulers.newThread())
                .flatMap(data -> Observable.fromIterable(new ArrayList<>(data.values())))
                .filter(data -> data.getProductId() == 22304 && data.getVendorId() == 1155)
                .filter(data -> {
                    if (!usbManager.hasPermission(data)) {
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(ACTION_USB_PRINT_PERMISSION), 0);
                        usbManager.requestPermission(data, mPermissionIntent);
                        return false;
                    }
                    return true;
                })
                .subscribe(data -> {
                    isConnected = usbPrinter.openConnection(data);
                    if (isConnected) {
                        printThread.start();
                        ttsUtil.speak("打印机连接完成");
                    } else {
                        ttsUtil.speak("打印机连接失败");
                    }
                });
    }

    private void initCode() {
        HashMap<String, UsbDevice> list = usbManager.getDeviceList();
        Observable.fromArray(list)
                .subscribeOn(Schedulers.newThread())
                .flatMap(data -> Observable.fromIterable(new ArrayList<>(data.values())))
                .filter(data -> data.getProductId() == 9930 && data.getVendorId() == 11734)
                .filter(data -> {
                    Log.e(TAG, "initCode");
                    if (!usbManager.hasPermission(data)) {
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                                0, new Intent(ACTION_DEVICE_PERMISSION), 0);
                        usbManager.requestPermission(data, mPermissionIntent);
                        return false;
                    }
                    return true;
                })
                .subscribe(data -> {
                    Log.e(TAG, "initCode1");
                    Code code = new Code(data, usbManager);
                    code.setListener(this);
                });
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PRINT_PERMISSION.equals(action)) {
                UsbDevice mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    isConnected = usbPrinter.openConnection(mUsbDevice);
                    if (isConnected) {
                        printThread.start();
                        ttsUtil.speak("打印机连接完成");
                    } else {
                        ttsUtil.speak("打印机连接失败");
                    }
                } else {
                    Log.d(TAG, "permission denied for device " + mUsbDevice);
                }
            } else if (ACTION_DEVICE_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Code code = new Code(device, usbManager);
                    code.setListener(MainActivity.this);
                } else {
                    Log.d(TAG, "permission denied for device " + device);
                }
            } else if (SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                    openWiFiList();
                }
            } else if (NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    checkConnect();
                }
            }
        }
    };

    private void checkConnect() {
        if (initObj != null) return;
        showWaitingDialog();
        initObj = Observable.interval(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    if (isConnetction()) {
                        waitingDialog.dismiss();
                        init();
                        initObj.dispose();
                    }
                }, error -> error.printStackTrace());
    }
}
