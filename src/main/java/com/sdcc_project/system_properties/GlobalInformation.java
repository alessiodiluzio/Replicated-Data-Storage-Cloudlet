package com.sdcc_project.system_properties;

public class GlobalInformation {

    private  String masterAddress;
    private  String host;
    private  State state = State.NORMAL;
    private static GlobalInformation instance;


    private GlobalInformation(){

    }

    public static GlobalInformation getInstance(){
        if(instance==null)
            instance = new GlobalInformation();
        return instance;
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
