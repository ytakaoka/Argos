package jp.co.menox.android.argos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.co.menox.android.argos.thermo.response.Value;
import jp.co.menox.android.argos.thermo.service.bluetooth.IThermoSensorService;
import jp.co.menox.android.argos.thermo.service.bluetooth.ThermoSensorCallback;
import jp.co.menox.android.argos.thermo.service.bluetooth.ThermoSensorService;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_BLUETOOTH_ENABLE = 42;

    private ListView listview;
    private ArrayAdapter<ValueWithMacAddr> adapter;

    private IThermoSensorService service;
    private ServiceConnection sconn;

    private BluetoothAdapter mBluetoothAdapter;

    private List<ValueWithMacAddr> listItems = new ArrayList<ValueWithMacAddr>(
            1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (this.mBluetoothAdapter != null)
            for (BluetoothDevice device : this.mBluetoothAdapter
                    .getBondedDevices()) {
                listItems.add(new ValueWithMacAddr(device.getAddress()));
            }

        listview = (ListView) this.findViewById(R.id.devices);
        listview.setAdapter((adapter = new DevicesAdapter(this, listItems)));
        listview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                DevicesAdapter.DeviceHolder h = (DevicesAdapter.DeviceHolder) arg1
                        .getTag();

                try {
                    MainActivity.this.service.startWatch(h.getMacAddr());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter == null) {
            return;
        }

        if (this.mBluetoothAdapter == null) {
            Toast.makeText(this, "sorry! this device has no bluetooth!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQ_BLUETOOTH_ENABLE);
        } else {
            Boolean test = bindService(new Intent(getApplicationContext(),
                    ThermoSensorService.class),
                    (sconn = new ThermoSensorServiceConnection()), BIND_AUTO_CREATE);
            Log.d("BIND", test.toString());
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        unbindService(sconn);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_BLUETOOTH_ENABLE && resultCode == RESULT_OK) {
            bindService(new Intent(getApplicationContext(),
                    ThermoSensorService.class),
                    new ThermoSensorServiceConnection(), BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class ThermoSensorServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IThermoSensorService tService = IThermoSensorService.Stub
                    .asInterface(service);
            try {
                tService.addThermoSensorCallback(new ThermoSensorCallback() {

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }

                    @Override
                    public void onValueUpdatedCallback(String macaddr,
                            Value value)
                            throws RemoteException {

                        Log.d("Thermo Update",
                                (String.format(
                                        "{\"transaction\":\"%d\", \"id\":\"%s\", \"name\":\"%s\", \"value\":\"%d\", \"unit\":\"%d\", \"time\":\"%s\"}",
                                        value.getTransaction(), value.getId(),
                                        value.getName(),
                                        value.getValue(), value.getUnit(),
                                        value.getTimeAsString()
                                        )));

                        for (ValueWithMacAddr v : listItems) {
                            if (!macaddr.equals(v.getMacAddr())) {
                                continue;
                            }
                            v.value = value;
                            adapter.notifyDataSetChanged();
                            return;
                        }
                    }

                    @Override
                    public void onDeviceDisconnectedCallback(String macaddr)
                            throws RemoteException {
                        Iterator<ValueWithMacAddr> iter = listItems.iterator();
                        while (iter.hasNext()) {
                            ValueWithMacAddr v = iter.next();
                            if (!macaddr.equals(v.getMacAddr())) {
                                continue;
                            }
                            listItems.remove(v);
                            adapter.notifyDataSetChanged();
                            return;
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            MainActivity.this.service = tService;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MainActivity.this.service = null;
        }
    }

    protected class DevicesAdapter extends ArrayAdapter<ValueWithMacAddr> {
        public class DeviceHolder {
            private View v;
            private String macAddr;

            public DeviceHolder(View v) {
                this.v = v;
            }

            public void setMacAddr(String macAddr) {
                this.macAddr = macAddr;
                ((TextView) v.findViewById(R.id.macaddr)).setText(macAddr);
            }

            public String getMacAddr() {
                return this.macAddr;
            }

            public void setThermo(String thermo) {
                ((TextView) v.findViewById(R.id.thermo)).setText(thermo);;
            }

            public void setWhen(String when) {
                ((TextView) v.findViewById(R.id.at)).setText(when);
            }
        }

        public DevicesAdapter(Context context, List<ValueWithMacAddr> items) {
            super(context, R.layout.device, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            DeviceHolder holder;

            if (convertView == null) {
                convertView = MainActivity.this.getLayoutInflater().inflate(
                        R.layout.device, parent, false);
                convertView.setTag((holder = new DeviceHolder(convertView)));
            } else {
                holder = (DeviceHolder) convertView.getTag();
            }

            ValueWithMacAddr value = this.getItem(position);
            holder.setMacAddr(value.getMacAddr());

            if (value.value != null) {
                holder.setThermo(value.value.getValue() + value.value.getUnit());
                holder.setWhen(value.value.getTimeAsString());
            }

            return convertView;
        }
    }

}