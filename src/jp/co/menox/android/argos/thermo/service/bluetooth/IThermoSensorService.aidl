package jp.co.menox.android.argos.thermo.service.bluetooth;

import jp.co.menox.android.argos.thermo.response.Info;
import jp.co.menox.android.argos.thermo.response.Value;
import jp.co.menox.android.argos.thermo.response.Error;
import jp.co.menox.android.argos.thermo.service.bluetooth.ThermoSensorCallback;

interface IThermoSensorService {
    
    /**
     * 端末の MACアドレスを指定して監視を始める。
     * 指定された MACアドレスの端末が見つからないとか、Bluetooth が利用できないという場合には、何もしない。
     */
    oneway void startWatch(String macaddr);
    
    /**
     * 端末の MACアドレスを指定して監視をやめる。
     * 指定された MACアドレスの端末が見つからないとか、Bluetooth が利用できないという場合には、何もしない。
     */
    oneway void endWatch(String macaddr);
    
    /**
     * センサの詳細情報を取得する
    */
    Info getInfo(String deviceMacAddr);
    
    /**
     * センサ端末を指定してリセットシグナルを送る
     */
    oneway void reset(String macaddr);
    
    /**
     * センサ端末を指定して、最近の測定値を返す
    */
    Value getRecentValue(String macAddr);
    
    /**
     * センサ端末を指定して、最近のエラーを返す
     */
    Error getError(String macAddr);
    
    /**
     * センサの状態変化イベントを取得するためのコールバックの指定
     */
    void addThermoSensorCallback(ThermoSensorCallback callback);
    
    /**
     * センサの状態変化イベントを取得するためのコールバックを解除する
     */
    oneway void clearThermoSensorCallback(ThermoSensorCallback callback);
}