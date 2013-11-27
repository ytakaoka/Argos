package jp.co.menox.android.argos.thermo.service.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/*package*/class WatcherLE extends Watcher {

    public static final String DATA = "DATA";
    public static final String MACADDR = "MACADDR";

    public static interface Events {
        public static final String KEY = "EVENT";

        public static final int EVENT_DISCONNECTED = -1;
        public static final int EVENT_INFO = 1;
        public static final int EVENT_VALUE = 2;
    }

    private BluetoothGatt bgatt;

    private Context context;

    public WatcherLE(BluetoothDevice device, Context context, Handler handler) {
        super(device, handler);
        this.setPriority(MIN_PRIORITY);
        this.setName("Thermo Sensor Watcher #" + this.getId());
        this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
        {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                adapter.cancelDiscovery();
            } else {
                return;
            }
        }
        
        bgatt = getDevice().connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt,
                    int status, int newState) {
                switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    emitDisconnected();
                    cancel();
                    break;
                default:
                    break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    return; // bye!
                }

                for (BluetoothGattService s : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : s
                            .getCharacteristics()) {
                        characteristic.getUuid();
                        Log.d("BLE CHAR UUID", characteristic.getUuid().toString());
                        
                        gatt.setCharacteristicNotification(characteristic, true);
                    }
                }
            }
            
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                    BluetoothGattCharacteristic chars) {
                chars.getUuid();
            }
        });
    }

    /**
     * 温度センサの監視をやめる
     */
    @Override
    public void cancel() {
        if (bgatt != null) {
            bgatt.close();
        }
        bgatt = null;
    }

}
