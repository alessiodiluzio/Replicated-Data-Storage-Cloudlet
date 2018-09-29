package com.sdcc_project.config;

public class GlobalInformation {
    private  String cloudLetAddress;
    private  String masterAddress;
    private  String host;
    private static GlobalInformation instance;

    private GlobalInformation(){

    }

    public static GlobalInformation getInstance(){
        if(instance==null)
            instance = new GlobalInformation();
        return instance;
    }

    public  String getCloudLetAddress() {
        return cloudLetAddress;
    }

    public void setCloudLetAddress(String cloudLetAddress) {
        this.cloudLetAddress = cloudLetAddress;
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
}
