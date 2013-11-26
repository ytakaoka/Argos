package jp.co.menox.android.argos.thermo.service.bluetooth;

import jp.co.menox.android.argos.thermo.response.Value;

interface ThermoSensorCallback {
    void onValueUpdatedCallback(String macaddr, in Value value);
    void onDeviceDisconnectedCallback(String macaddr);
}