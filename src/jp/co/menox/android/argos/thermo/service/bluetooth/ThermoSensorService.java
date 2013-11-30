package jp.co.menox.android.argos.thermo.service.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.menox.android.argos.thermo.response.Error;
import jp.co.menox.android.argos.thermo.response.Info;
import jp.co.menox.android.argos.thermo.response.Value;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
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
        mBinder.onDestroy();
        super.onDestroy();
    }

    private final IThermoSensorServiceImpl mBinder = new IThermoSensorServiceImpl(this);

    public static class IThermoSensorServiceImpl extends
            IThermoSensorService.Stub {
        public IThermoSensorServiceImpl(Context context) {
            this.context = context;
        }
        
        private Context context;
        private Map<String, Info> infos = new HashMap<String, Info>();
        private Map<String, Value> values = new HashMap<String, Value>();
        private Map<String, Watcher> watchers = new HashMap<String, Watcher>();
        
        private List<TimelimittedLeScanCallback> leSeekers = new ArrayList<TimelimittedLeScanCallback>();
        private List<ThermoSensorCallback> thermoCallbacks = new ArrayList<ThermoSensorCallback>();

        protected void onInfo(Info info, String macaddr) {
            infos.put(macaddr, info);
        }

        protected void onValue(Value value, String macaddr) {
            values.put(macaddr, value);

            for (ThermoSensorCallback callback : thermoCallbacks) {
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
            watchers.remove(macaddr).cancel();;

            for (ThermoSensorCallback callback : thermoCallbacks) {
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

        /** not implied yet */
        @Override
        public void reset(String macaddr) throws RemoteException {}

        @Override
        public Value getRecentValue(String macAddr) throws RemoteException {
            return values.get(macAddr);
        }

        /** not implied yet */
        @Override
        public Error getError(String macAddr) throws RemoteException {
            return null;
        }

        @Override
        public void startWatch(String macaddr) throws RemoteException {
            if (adapter == null || !adapter.isEnabled()) {
                return;
            }

            macaddr = macaddr.toUpperCase(Locale.ENGLISH);
            if (watchers.keySet().contains(macaddr)
                    || !BluetoothAdapter.checkBluetoothAddress(macaddr)) {
                return;
            }

            BluetoothDevice dev = adapter.getRemoteDevice(macaddr);
            if (dev.getBondState() != BluetoothDevice.BOND_BONDED) {
                return;
            }
            Watcher watcher = new Watcher(dev, new WatcherEventProxy(this));
            watchers.put(macaddr, watcher);
            watcher.start();
        }

        @Override
        public void startWatchLe(long scanDulation) throws RemoteException {
            if (adapter == null || !adapter.isEnabled()) {
                return;
            }
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            
            TimelimittedLeScanCallback tllsc = new TimelimittedLeScanCallback(scanDulation);
            leSeekers.add(tllsc);
            adapter.startLeScan(tllsc);
        }

        @Override
        public void endWatch(String macaddr) throws RemoteException {
            Watcher watcher = watchers.get(macaddr);
            if (watcher != null) {
                watchers.remove(macaddr).cancel();
            }
        }

        @Override
        public void addThermoSensorCallback(ThermoSensorCallback callback)
                throws RemoteException {
            this.thermoCallbacks.add(callback);
        }

        @Override
        public void clearThermoSensorCallback(ThermoSensorCallback callback)
                throws RemoteException {
            this.thermoCallbacks.remove(callback);
        }
        
        /*package*/ void onDestroy() {
            thermoCallbacks.clear();
            infos.clear();
            values.clear();
            
            for(Watcher watcher : watchers.values()){
                watcher.cancel();
            }
            watchers.clear();
            
            for(TimelimittedLeScanCallback seekser : leSeekers){
                seekser.quit();
            }
            leSeekers.clear();
        }

        private static class WatcherEventProxy extends
                Watcher.WatcherEventHandler {
            private ThermoSensorService.IThermoSensorServiceImpl enclosing;

            public WatcherEventProxy(
                    ThermoSensorService.IThermoSensorServiceImpl enclosing) {
                this.enclosing = enclosing;
            }

            @Override
            public void onDisconnected(String macaddr) {
                enclosing.onDisconnected(macaddr);
            }

            @Override
            public void onInfo(Info info, String macaddr) {
                enclosing.onInfo(info, macaddr);
            }

            @Override
            public void onValue(Value value, String macaddr) {
                enclosing.onValue(value, macaddr);
            }
        }

        /**
         * 
         * 時間制限付きのLeScanCallback.
         * 指定した時間が過ぎると、自動的にスキャンを中止する。
         * 
         * @author Youta
         * 
         */
        private class TimelimittedLeScanCallback implements LeScanCallback {
            private Timer timelimit;
            private Boolean seeking;

            /**
             * @param scanDulation
             *            スキャンを継続する時間
             */
            public TimelimittedLeScanCallback(long scanDulation) {
                timelimit = new Timer();
                timelimit.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        adapter.stopLeScan(TimelimittedLeScanCallback.this);
                        seeking = false;
                    }
                }, scanDulation);
                seeking = true;
            }

            @Override
            public void onLeScan(BluetoothDevice device, int rssi,
                    byte[] scanRecord) {
                Watcher watcher = new WatcherLE(
                        device,
                        context,
                        new WatcherEventProxy(
                                ThermoSensorService.IThermoSensorServiceImpl.this));
                watchers.put(device.getAddress(), watcher);
                watcher.start();
            }

            public void quit() {
                if (!seeking) { return; }
                adapter.stopLeScan(this);
                timelimit.cancel();
                timelimit.purge();
                seeking = false;
            }
        }
    }
}
