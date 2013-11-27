package jp.co.menox.android.argos.thermo.service.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Locale;
import java.util.UUID;

import jp.co.menox.android.argos.thermo.response.Info;
import jp.co.menox.android.argos.thermo.response.Value;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/*package*/ class Watcher extends Thread {
    
    public static final String DATA = "DATA";
    public static final String MACADDR = "MACADDR";
    public static interface Events {
        public static final String KEY = "EVENT";
        
        public static final int EVENT_DISCONNECTED = -1;
        public static final int EVENT_INFO = 1;
        public static final int EVENT_VALUE = 2;
    }
    
    private final static UUID SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;
    private Handler handler;

    private BluetoothSocket sock;
    private InputStream inst;
    private OutputStream ost;

    public Watcher(BluetoothDevice device, Handler handler) {
        super();
        this.setPriority(MIN_PRIORITY);
        this.setName("Thermo Sensor Watcher #" + this.getId());
        this.device = device;
        this.handler = handler;
    }
    
    protected BluetoothDevice getDevice() {
        return device;
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

        try {
            
            sock = getDevice().createRfcommSocketToServiceRecord(SPP_UUID);
            sock.connect();
            inst = sock.getInputStream();
            ost = sock.getOutputStream();
            ost.write(infoQuery(1));
            ost.write(startQuery(2));
        } catch (IOException e) {
            e.printStackTrace();
            this.emitDisconnected();
            return;
        }

        while (true) {
            try {
                JSONObject json = miningJson(inst);

                if (!json.has("transaction")) {
                    continue;
                }

                int transaction = json.getInt("transaction");
                if (transaction == 1) {
                    emitInfo(json, transaction);
                } else if (json.has("value") || json.has("time") ) {
                    emitValue(json, transaction);
                }

            } catch (IOException e) {
                emitDisconnected();
                break;
            } catch (JSONException e) {
                continue;
            }
        }

        cancel();
    }
    
    /**
     * 温度センサの監視をやめる
     */
    public void cancel() {
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * this is a dull json tokenizer.
     * 
     * it assumes that stream to read has no chars of '{' and '}' without both
     * ends of json object.
     * So, if it is resursive or string item contains '{' or '}', it may not
     * work well.
     * 
     * if it fail to create json object, it returns null.
     * 
     * It blocks until gets '}' or reaches end of stream.
     * 
     * @param inst
     * @return
     * @throws IOException
     */
    private JSONObject miningJson(InputStream inst) throws IOException {
        int oneByte;
        StringBuilder buffer = new StringBuilder();

        while (-1 < (oneByte = inst.read())) {
            char[] chars = Character.toChars(oneByte);
            if (chars[0] == '{') {
                buffer.append(chars);
                break;
            }
        }

        while (-1 < (oneByte = inst.read())) {
            char[] chars = Character.toChars(oneByte);
            buffer.append(chars);
            if (chars[0] == '}') {
                break;
            }
        }

        try {
            return new JSONObject(buffer.toString());
        } catch (JSONException e) {
            return null;
        }
    }

    private byte[] infoQuery(int transaction) {
        return String.format(Locale.JAPAN, "{\"info\":\"%d\"}", transaction)
                .getBytes();
    }
    
    private byte[] startQuery(int transaction) {
        return String.format(Locale.JAPAN, "{\"start\":\"%d\"}", transaction)
                .getBytes();
    }

    /**
     * this method is failure-oblivious.
     * handler may get Info has null property.
     *  
     * @param valueJson
     * @param transaction
     */
    protected void emitInfo(JSONObject infoJson, int transaction) {
        Info info = new Info();

        info.setTransaction(transaction);
        try {
            info.setId(infoJson.getString("id"));
        } catch (JSONException e) {}
        try {
            info.setLocation(infoJson.getString("location"));
        } catch (JSONException e) {}
        try {
            info.setName(infoJson.getString("name"));
        } catch (JSONException e) {}
        try {
            info.setUnit(infoJson.getString("unit"));
        } catch (JSONException e) {}
        
        emit(info);
    }

    /**
     * this method is failure-oblivious.
     * handler may get Value has null property.
     *  
     * @param valueJson
     * @param transaction
     */
    protected void emitValue(JSONObject valueJson, int transaction) {
        Value value = new Value();

        value.setTransaction(transaction);
        
        try {
            value.setId(valueJson.getString("id"));
        } catch (JSONException e) {}
        try {
            value.setValue(valueJson.getDouble("value"));
        } catch (JSONException e) {}
        try {
            value.setTimeAsString(valueJson.getString("time"));
        } catch (JSONException e) {} catch (ParseException e) {}
        try {
            value.setName(valueJson.getString("name"));
        } catch (JSONException e) {}
        try {
            value.setUnit(valueJson.getString("unit"));
        } catch (JSONException e) {}
        
        emit(value);
    }

    /**
     * emit VALUE event.
     * 
     * @param value payload of event
     */
    protected void emit(Value value) {
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(Events.KEY, Events.EVENT_VALUE);
        b.putParcelable(DATA, value);
        b.putString(MACADDR, getDevice().getAddress());
        msg.setData(b);
        handler.sendMessage(msg);
    }
    
    /**
     * emit INFO event.
     * 
     * @param info payload of event
     */
    protected void emit(Info info) {
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(Events.KEY, Events.EVENT_INFO);
        b.putParcelable(DATA, info);
        b.putString(MACADDR, getDevice().getAddress());
        msg.setData(b);
        handler.sendMessage(msg);
    }
    
    protected void emitDisconnected() {
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(Events.KEY, Events.EVENT_DISCONNECTED);
        b.putString(MACADDR, getDevice().getAddress());
        msg.setData(b);
        handler.sendMessage(msg);
    }
}

