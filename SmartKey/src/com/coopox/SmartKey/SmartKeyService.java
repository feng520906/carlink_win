package com.coopox.SmartKey;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.coopox.common.Constants;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.EventReporter;
import com.coopox.common.utils.LocalLog;
import com.coopox.common.utils.ThreadManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/9
 */
public class SmartKeyService extends Service implements SmartKeyScanner.DeviceListener {
    private static final String TAG = "SmartKeyService";
    /** Intent extras */
    public static final String EXTRA_DEVICE = "DEVICE";
    public static final String EXTRA_RSSI = "RSSI";
    public static final String EXTRA_SOURCE = "SOURCE";
    public static final String EXTRA_ADDR = "ADDRESS";
    public static final String EXTRA_CONNECTED = "CONNECTED";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_UUID = "UUID";
    public static final String EXTRA_VALUE = "VALUE";
    public static final int DEVICE_SOURCE_SCAN = 1;
    // 智键的设备名称，目前是通过名称来判断是否为智键的
    private static final String SMART_KEY_NAME = "S1";
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final int DEVICE_SOURCE_CONNECTED = 2;

    public static final String GATT_DEVICE_FOUND = "com.coopox.VoiceNow.smartkey.device_found";
    public final static String ACTION_GATT_CONNECTED =
            "com.coopox.VoiceNow.smartkey.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.coopox.VoiceNow.smartkey.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.coopox.VoiceNow.smartkey.ACTION_GATT_SERVICES_DISCOVERED";
    //    public final static String EXTRA_DATA =
//            "com.coopox.VoiceNow.smartkey.EXTRA_DATA";

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public final static UUID UUID_KEY_EVENT =
            UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);

    /** Descriptor used to enable/disable notifications/indications */
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    SmartKeyScanner mSmartKeyScanner;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private boolean mIsKeyConnected;
    private BluetoothGattCharacteristic  mNotifyCharacteristic;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private String mDevice;
    private Toast mToast;
    private long mLastConnectTime;

    @Override
    public void onCreate() {
        super.onCreate();

        if (isVoiceNowTooOld()) {
            // 如果 VoiceNow 是老版本（内置了智键管理能力）则本服务就不再启动，避免两者冲突。
            stopSelf();
            Log.d(TAG, "VoiceNow is too old to launch SmartKey Component!");
        }

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        showTip("智键连接服务启动");
        mSmartKeyScanner = new SmartKeyScanner(this);
        if (!mSmartKeyScanner.setup(this)) {
            mSmartKeyScanner = null;
        } else {

            // Start foreground service to avoid unexpected kill
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("智键控制服务运行中……")
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .build();
            startForeground(1002, notification);

            mBluetoothManager = mSmartKeyScanner.getmBluetoothManager();
            mBluetoothAdapter = mSmartKeyScanner.getmBluetoothAdapter();

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        showTip("智键连接服务关闭");

        if (mNotifyCharacteristic != null) {
            enableNotification(false, mNotifyCharacteristic);
        }

        if (mIsKeyConnected) {
            disconnect();
            close();
        }

        if (null != mSmartKeyScanner) {
            mSmartKeyScanner.teardown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            boolean willStop = intent.getBooleanExtra("stop", false);
            if (willStop) {
                Log.d(TAG, "Stop SmartKey daemon by user.");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    // 检查 VoiceNow 组件是否为老版本，因为在 VersionCode=12 及之前的版本里 VoiceNow
    // 内置了 SmartKey 的功能，检查一下避免二者冲突。
    public boolean isVoiceNowTooOld() {
        final String pkgName = "com.coopox.VoiceNow";
        final PackageManager pm = getPackageManager();

        int versionCode = 0;
        try {
            PackageInfo pkgInfo =
                    pm.getPackageInfo(pkgName, PackageManager.GET_META_DATA);
            if (null != pkgInfo) {
                versionCode = pkgInfo.versionCode;
                if (versionCode > 0 && versionCode <= 12) {
                    EventReporter.INSTANCE.report("VoiceNowTooOld", String.valueOf(versionCode));
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    @Override
    public void onDeviceDiscovery(SmartKeyScanner.DeviceInfo deviceInfo) {
        if (!mIsKeyConnected) {
            if (null != deviceInfo) {
                String name = deviceInfo.mDevice.getName();
                if (null != name
                        && (name.equals(SMART_KEY_NAME)
                            // 暂时仍然保留老智键的设备名，等新智键真正投入使用之后再去掉
                            || name.equals("G5 KEYS"))) {
                    if (connect(deviceInfo.mDevice.getAddress())) {
                        Log.d(TAG, "Smart Key is connected.");
                        mIsKeyConnected = true;

                        showTip("智键已连接");
                    }
                }
            }
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
/*    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }*/

    public boolean connect(String address) {
        if (mBluetoothAdapter == null) return false;

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) return false;

        if (mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) ==
                BluetoothProfile.STATE_CONNECTED && null != mBluetoothGatt) {
            mBluetoothGatt.discoverServices();
            return false;
        }

        if (address.equals(mDevice) && mBluetoothGatt != null) {
            mBluetoothGatt.connect();
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }

        mDevice = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mIsKeyConnected = false;

        showTip(String.format("智键已主动断开"));
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_KEY_EVENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) return false;
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) return false;

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (clientConfig == null) return false;

        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        boolean ret = mBluetoothGatt.writeDescriptor(clientConfig);
        Log.d(TAG, "set notification " + ret);
        showTip("智键已就绪");
        mLastConnectTime = SystemClock.uptimeMillis();
        return ret;
    }

    public boolean enableIndication(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) return false;
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) return false;

        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (clientConfig == null) return false;

        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            Intent intent = new Intent(ACTION_GATT_CONNECTED);
            intent.putExtra(EXTRA_ADDR, device.getAddress());
            intent.putExtra(EXTRA_CONNECTED, newState == BluetoothProfile.STATE_CONNECTED);
            intent.putExtra(EXTRA_STATUS, status);
            sendBroadcast(intent);

            if (newState == BluetoothProfile.STATE_CONNECTED && mBluetoothGatt != null) {
                sendDeviceFoundIntent(device, 255, DEVICE_SOURCE_CONNECTED);
                mBluetoothGatt.discoverServices();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED && mBluetoothGatt != null) {
                // 尝试重连
                mBluetoothGatt.connect();

                long currentTime = SystemClock.uptimeMillis();
                long diffTime = currentTime - mLastConnectTime;
                long lifeTime = (long) (diffTime / 1000f);
                showTip(String.format("智键重连中, 上次连接持续 %d 秒", lifeTime));

                // DEBUG:
                String log = String.format("智键连接持续 %d 秒。", lifeTime);
                LocalLog.write(log);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                enumGattServices(getSupportedGattServices());

                // enable notification
                if (!Checker.isEmpty(mGattCharacteristics) &&
                        !Checker.isEmpty(mGattCharacteristics.get(0))) {
                    final BluetoothGattCharacteristic characteristic =
                            mGattCharacteristics.get(0).get(0);
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            enableNotification(false, mNotifyCharacteristic);
                            mNotifyCharacteristic = null;
                        }
                        readCharacteristic(characteristic);
                    }
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        mNotifyCharacteristic = characteristic;
//                        setCharacteristicNotification(characteristic, true);
                        enableNotification(true, characteristic);
                    }

                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        enableIndication(true, characteristic);
                    }
                }

                BluetoothDevice device = gatt.getDevice();
                Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
                intent.putExtra(EXTRA_ADDR, device.getAddress());
                intent.putExtra(EXTRA_STATUS, status);
                sendBroadcast(intent);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] values = characteristic.getValue();
                if (!Checker.isEmpty(values)) {
                    Intent intent = new Intent(Constants.ACTION_SMART_KEY_EVENT);
                    intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
                    intent.putExtra(EXTRA_STATUS, status);
                    intent.putExtra(EXTRA_VALUE, values);
                    sendBroadcast(intent);

                    showTip(String.format("检测到智键事件(%d)", (int)values[0]));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            broadcastUpdate(ACTION_SMART_KEY_EVENT, characteristic);
            onCharacteristicRead(gatt, characteristic, 0);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void enumGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic";
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
//                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            if (!Checker.isEmpty(uuid) && uuid.equals(SampleGattAttributes.SMART_KEY_SERVICE)) {
                currentServiceData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);
                }
                mGattCharacteristics.add(charas);
            }
//            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

/*    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_KEY_EVENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }*/

    private void sendDeviceFoundIntent(BluetoothDevice device, int rssi, int source) {
        Intent intent = new Intent(GATT_DEVICE_FOUND);
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_RSSI, rssi);
        intent.putExtra(EXTRA_SOURCE, source);
        sendBroadcast(intent);
    }

    private void showTip(final String str)
    {
        Log.d(TAG, str);
        ThreadManager.INSTANCE.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (null == mToast) {
                    mToast = Toast.makeText(SmartKeyService.this, "", Toast.LENGTH_SHORT);
                }
                mToast.setText(str);
                mToast.show();
            }
        });
    }
}
