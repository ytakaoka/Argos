package jp.co.menox.argos;

import jp.co.menox.argos.thermo.bluetooth.response.Value;

public class ValueWithMacAddr {
    public Value value;
    private String macAddr;
    
    public ValueWithMacAddr(String macaddr){
        this.macAddr = macaddr;
    }
    
    public String getMacAddr(){
        return macAddr;
    }
    
}
