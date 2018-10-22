package com.sdcc_project.system_properties;

import com.sdcc_project.monitor.State;
import com.sdcc_project.util.Util;

/**
 * Contiene informazioni globali per lo scambio tra i vari componenti.
 */
public class GlobalInformation {

    private  String masterAddress;
    private State state = State.NORMAL;
    private static GlobalInformation instance;
    private  String publicIPAddress;
    private int replicationFactory;


    private GlobalInformation(){
        publicIPAddress = Util.getPublicIPAddress();
    }

    public static GlobalInformation getInstance(){
        if(instance==null)
            instance = new GlobalInformation();
        return instance;
    }

    public int getReplicationFactory() {
        return replicationFactory;
    }

    public void setReplicationFactory(int replicationFactory) {
        this.replicationFactory = replicationFactory;
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




    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
