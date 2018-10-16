package com.sdcc_project.system_properties;

import com.sdcc_project.monitor.State;
import com.sdcc_project.util.Util;

public class GlobalInformation {

    private  String masterAddress;
    private  String host;
    private State state = State.NORMAL;
    private static GlobalInformation instance;
    private  String publicIPAddress;


    private GlobalInformation(){
        publicIPAddress = Util.getPublicIPAddress();
    }

    public static GlobalInformation getInstance(){
        if(instance==null)
            instance = new GlobalInformation();
        return instance;
    }

    public String getPublicIPAddress() {
        return publicIPAddress;
    }

    public  String getMasterAddress() {
        return masterAddress;
    }

    public  void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public String getHost() {
        return host;
    }

    public  void setHost(String host) {
        this.host = host;
    }

    public void setInstance(GlobalInformation instance) {
        GlobalInformation.instance = instance;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
