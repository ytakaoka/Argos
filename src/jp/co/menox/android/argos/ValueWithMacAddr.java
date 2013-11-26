package jp.co.menox.android.argos;

import jp.co.menox.android.argos.thermo.response.Value;

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
