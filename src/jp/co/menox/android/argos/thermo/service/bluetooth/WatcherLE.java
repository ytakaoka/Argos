package jp.co.menox.android.argos.thermo.service.bluetooth;

import java.util.Calendar;
import java.util.UUID;

import jp.co.menox.android.argos.thermo.response.Value;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

/*package*/class WatcherLE extends Watcher {

    public static interface Events {
        public static final String KEY = "EVENT";

        public static final int EVENT_DISCONNECTED = -1;
        public static final int EVENT_INFO = 1;
        public static final int EVENT_VALUE = 2;
    }

    private BluetoothGatt bgatt;

    private Context context;

    private UUID CHARACTERISTIC_TEMPERATURE_MEASUREMENT = UUID
            .fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    private UUID CHARACTERISTIC_INTERMEDIATE_TEMPERATURE = UUID
            .fromString("00002A1E-0000-1000-8000-00805f9b34fb");
    private UUID CLIENT_CHARACTERISCIC_CONFIGURATION = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    private UUID USER_DESCRPITION = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");

    public WatcherLE(BluetoothDevice device, Context context, Handler handler) {
        super(device, handler);
        this.setName("Thermo Sensor Watcher(BLE) #" + this.getId());
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

        bgatt = getDevice().connectGatt(context, true, new ThermoAttCallback());
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

    private class ThermoAttCallback extends BluetoothGattCallback {

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
        public void onServicesDiscovered(BluetoothGatt gatt,
                int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return; // bye!
            }

            for (BluetoothGattService s : gatt.getServices()) {

                BluetoothGattCharacteristic intermediateTemp =
                        s.getCharacteristic(CHARACTERISTIC_INTERMEDIATE_TEMPERATURE);

                BluetoothGattDescriptor userDesc = intermediateTemp
                        .getDescriptor(USER_DESCRPITION);
                if (userDesc == null) {
                    return;
                }

                JSONObject desc;
                try {
                    desc = new JSONObject(String.valueOf(userDesc
                            .getValue()));
                } catch (JSONException e) {
                    return; // this is not expected one.
                }

                WatcherLE.this.emitInfo(desc, 0);
                BluetoothGattDescriptor d = intermediateTemp
                        .getDescriptor(CLIENT_CHARACTERISCIC_CONFIGURATION);

                @SuppressWarnings("unused")
                boolean _ = d != null
                        && d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        && gatt.writeDescriptor(d)
                        && gatt.setCharacteristicNotification(
                                intermediateTemp, true);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic chars) {
            Value value = new Value();
            int unitOffset = 0 != (chars.getValue()[0] & 0x80) ? 0 : 1;
            String[] unit = new String[]{"℃", "°Ｆ"};
            byte[] raw = chars.getValue();
            if (raw.length < 16) {
                return;
            }
            
            value.setUnit(unit[unitOffset]);
            value.setValue(chars.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1 + unitOffset));
            Calendar c = Calendar.getInstance();
            c.set(raw[9] * 8 + raw[10],
                    raw[11],
                    raw[12],
                    raw[13],
                    raw[14],
                    raw[15]);
            value.setTime(c.getTimeInMillis());
            value.setTransaction(1);
            emit(value);
        }
    }
}
