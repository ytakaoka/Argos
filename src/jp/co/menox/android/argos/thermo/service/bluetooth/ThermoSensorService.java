package jp.co.menox.android.argos.thermo.service.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.menox.android.argos.thermo.response.Error;
import jp.co.menox.android.argos.thermo.response.Info;
import jp.co.menox.android.argos.thermo.response.Value;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

public class ThermoSensorService extends Service {
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private final IThermoSensorService.Stub mBinder = new IThermoSensorServiceImpl(
            this);

    public static class IThermoSensorServiceImpl extends
            IThermoSensorService.Stub {
        public IThermoSensorServiceImpl(Context context) {
            this.context = context;
        }

        private Context context;
        private Map<String, Info> infos = new HashMap<String, Info>();
        private Map<String, Value> values = new HashMap<String, Value>();

        private Map<String, Watcher> watchers = new HashMap<String, Watcher>();

        protected void onInfo(Info info, String macaddr) {
            infos.put(macaddr, info);
        }

        protected void onValue(Value value, String macaddr) {
            values.put(macaddr, value);

            for (ThermoSensorCallback callback : callbacks) {
                try {
                    callback.onValueUpdatedCallback(macaddr, value);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onDisconnected(String macaddr) {
            infos.remove(macaddr);
            values.remove(macaddr);
            watchers.remove(macaddr);

            for (ThermoSensorCallback callback : callbacks) {
                try {
                    callback.onDeviceDisconnectedCallback(macaddr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        @Override
        public Info getInfo(String deviceMacAddr) throws RemoteException {
            return infos.get(deviceMacAddr);
        }

        /**
         * not implied yet
         */
        @Override
        public void reset(String macaddr) throws RemoteException {}

        @Override
        public Value getRecentValue(String macAddr) throws RemoteException {
            return values.get(macAddr);
        }

        /**
         * not implied yet
         */
        @Override
        public Error getError(String macAddr) throws RemoteException {
            return null;
        }

        @Override
        public void startWatch(String macaddr) throws RemoteException {
            if (adapter == null) {
                return;
            }
            if (watchers.keySet().contains(macaddr)) {
                return; // already started
            }
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if (!device.getAddress().equals(macaddr)) {
                    continue;
                }
                Watcher watcher = null;
                
                switch (device.getType()){
                    case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    case BluetoothDevice.DEVICE_TYPE_DUAL:
                        watcher = new Watcher(device, new WatcherEventHandler(this));
                        break;
                    case BluetoothDevice.DEVICE_TYPE_LE:
                        watcher = new WatcherLE(device, context,
                                new WatcherEventHandler(this));
                        break;
                     default:
                         continue;
                }
                
                watchers.put(macaddr, watcher);
                watcher.start();
            }
        }

        @Override
        public void endWatch(String macaddr) throws RemoteException {
            Watcher watcher = watchers.get(macaddr);
            if (watcher == null) {
                return; // not started yet
            }
            watcher.cancel();
        }

        List<ThermoSensorCallback> callbacks = new ArrayList<ThermoSensorCallback>();

        @Override
        public void addThermoSensorCallback(ThermoSensorCallback callback)
                throws RemoteException {
            this.callbacks.add(callback);
        }

        @Override
        public void clearThermoSensorCallback(ThermoSensorCallback callback)
                throws RemoteException {
            this.callbacks.remove(callback);
        }
    };

    private static class WatcherEventHandler extends Handler {
        private IThermoSensorServiceImpl service;

        public WatcherEventHandler(IThermoSensorServiceImpl service) {
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();

            Integer event = b.getInt(Watcher.Events.KEY);

            DISPATCH: {
                if (event.equals(Watcher.Events.EVENT_INFO)) {
                    service.onInfo((Info) b.getParcelable(Watcher.DATA),
                            b.getString(Watcher.MACADDR));
                    break DISPATCH;
                }
                if (event.equals(Watcher.Events.EVENT_VALUE)) {
                    service.onValue((Value) b.getParcelable(Watcher.DATA),
                            b.getString(Watcher.MACADDR));
                    break DISPATCH;
                }
                if (event.equals(Watcher.Events.EVENT_DISCONNECTED)) {
                    service.onDisconnected(b.getString(Watcher.MACADDR));
                    break DISPATCH;
                }
            }
            msg.recycle();
        }
    }
}
