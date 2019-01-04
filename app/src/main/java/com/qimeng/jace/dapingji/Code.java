package com.qimeng.jace.dapingji;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.qimeng.jace.dapingji.util.Buff;

import java.util.Arrays;

public class Code {

    private static final String TAG = "CodeClass";

    private boolean isRunn = true;

    private UsbDevice device;
    private UsbManager manager;
    private UsbEndpoint inUsbEndpoint;
    private UsbEndpoint outUsbEndpoint;
    private UsbInterface mUsbInterface;
    private UsbDeviceConnection mUsbDeviceConnection;
    private CodeListener listener;

    private Buff buff = new Buff();

    public Code(UsbDevice device, UsbManager manager) {
        this.device = device;
        this.manager = manager;
        init();
    }

    public void setListener(CodeListener listener) {
        this.listener = listener;
    }

    private void init() {
        int interfaceCount = device.getInterfaceCount();
        for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
            UsbInterface usbInterface = device.getInterface(interfaceIndex);
            if ((UsbConstants.USB_CLASS_CDC_DATA != usbInterface.getInterfaceClass())
                    && (UsbConstants.USB_CLASS_COMM != usbInterface.getInterfaceClass())
                    && UsbConstants.USB_CLASS_HID != usbInterface.getInterfaceClass()) {
                continue;
            }
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                UsbEndpoint ep = usbInterface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        inUsbEndpoint = ep;
                    } else {
                        outUsbEndpoint = ep;
                    }
                }
            }
            if (null == inUsbEndpoint) {
                inUsbEndpoint = null;
                outUsbEndpoint = null;
                mUsbInterface = null;
            } else {
                //连接成功
                mUsbInterface = usbInterface;
                mUsbDeviceConnection = manager.openDevice(device);
                Log.e(TAG, "连接成功");
                initConnection();
                break;
            }
        }
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, "连接失败");
        }
    }

    private void initConnection() {
        if (!mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {
            return;
        }
        int ret = mUsbDeviceConnection.controlTransfer(0x21, 0x22, 0x00, 0, null, 0, 0);
        setCdcBaudrate(115200);
    }

    private void setCdcBaudrate(int baudrate) {
        byte[] baudByte = new byte[4];
        baudByte[0] = (byte) (baudrate & 0x000000FF);
        baudByte[1] = (byte) ((baudrate & 0x0000FF00) >> 8);
        baudByte[2] = (byte) ((baudrate & 0x00FF0000) >> 16);
        baudByte[3] = (byte) ((baudrate & 0xFF000000) >> 24);
        int ret = mUsbDeviceConnection.controlTransfer(0x21, 0x20, 0, 0,
                new byte[]{baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0x00, 0x00, 0x08}, 7, 0);
        startMode();
    }

    private void startMode() {
        new Thread(() -> {
            while (isRunn) {
                byte[] bytes = new byte[inUsbEndpoint.getMaxPacketSize()];
                //读取数据, 这里注意最后一个参数为0时 为阻塞读取，知道读到数据为止
                int ret = mUsbDeviceConnection.bulkTransfer(inUsbEndpoint, bytes, bytes.length, 0);
                if (ret > 0) {
                    buff.append(Arrays.copyOf(bytes, ret));
                    byte[] code = buff.getCode();
                    if (code != null) {
                        final String str = new String(code);
                        if (listener != null) {
                            listener.onCode(str);
                        }
                    }
                } else {
                    Log.d("xxx", "Read empty data");
                }
            }
        }).start();
    }


    public interface CodeListener {

        void onCode(String code);

    }
}
