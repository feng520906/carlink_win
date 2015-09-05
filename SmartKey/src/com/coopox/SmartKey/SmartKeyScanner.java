package com.coopox.SmartKey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/9
 */
public class SmartKeyScanner implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "SK_Scanner";
    private BluetoothManager mBluetoothManager;

    public interface DeviceListener {
        void onDeviceDiscovery(DeviceInfo deviceInfo);
    }

    class DeviceInfo {
        public DeviceInfo(BluetoothDevice device, int rssi, int source) {
            mDevice = device;
            mRssi = rssi;
            mSource = source;
        }

        BluetoothDevice mDevice;
        int mRssi;
        int mSource;
    }

    private final Context mContext;
    private BluetoothAdapter mBluetoothAdapter = null;
    private volatile boolean mIsScanning;
//    private List<DeviceInfo> mDevices;
    private DeviceListener mListener;

    public SmartKeyScanner(Context context) {
        mContext = context;
//        mDevices = new Vector<DeviceInfo>();
    }

    public boolean setup(DeviceListener listener) {
        mListener = listener;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) return false;

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmartKeyService.GATT_DEVICE_FOUND);
        mContext.registerReceiver(mGattDeviceReceiver, intentFilter);

        startScan();
        return true;
    }

    public void teardown() {
        mContext.unregisterReceiver(mGattDeviceReceiver);
        stopScan();
//        mDevices.clear();
    }

    public void startScan() {
        if (null != mBluetoothAdapter && mBluetoothAdapter.startLeScan(this)) {
            mIsScanning = true;
        }
    }

    public void stopScan() {
        mIsScanning = false;
        if (null != mBluetoothAdapter) mBluetoothAdapter.stopLeScan(this);
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public BluetoothManager getmBluetoothManager() {
        return mBluetoothManager;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//        mDevices.add(new DeviceInfo(device, rssi, SmartKeyService.DEVICE_SOURCE_SCAN));
        Log.d(TAG, String.format("Device(%s), rssi = %d, source = %d", device.getName(), rssi, SmartKeyService.DEVICE_SOURCE_SCAN));
        if (null != mListener) {
            DeviceInfo info = new DeviceInfo(device, rssi, SmartKeyService.DEVICE_SOURCE_SCAN);
            mListener.onDeviceDiscovery(info);
        }
    }

    private final BroadcastReceiver mGattDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(SmartKeyService.EXTRA_DEVICE);
            final int rssi = intent.getIntExtra(SmartKeyService.EXTRA_RSSI, 0);
            final int source = intent.getIntExtra(SmartKeyService.EXTRA_SOURCE, 0);

            if (SmartKeyService.GATT_DEVICE_FOUND.equals(intent.getAction())) {
//                mDevices.add(new DeviceInfo(device, rssi, source));
                Log.d(TAG, String.format("Device(%s), rssi = %d, source = %d", device.getName(), rssi, source));
                if (null != mListener) {
                    mListener.onDeviceDiscovery(new DeviceInfo(device, rssi, source));
                }
            }
        }
    };
}
