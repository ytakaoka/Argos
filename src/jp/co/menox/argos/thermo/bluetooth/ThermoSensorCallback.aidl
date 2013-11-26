package jp.co.menox.argos.thermo.bluetooth;

import jp.co.menox.argos.thermo.bluetooth.response.Value;

interface ThermoSensorCallback {
    void onValueUpdatedCallback(String macaddr, in Value value);
    void onDeviceDisconnectedCallback(String macaddr);
}