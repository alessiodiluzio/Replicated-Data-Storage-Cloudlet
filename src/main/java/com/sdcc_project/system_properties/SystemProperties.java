package com.sdcc_project.system_properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * SystemProperties estratte dal file di configurazione
 */
public class SystemProperties {

    private static SystemProperties istanza;
    private double cpuMaxUsage = 70.0;
    private double ramMaxUsage = 70.0;
    private double cpuMinUsage = 30.0;
    private double ramMinUsage = 45;



    private SystemProperties(){
        loadProperties();
    };

    public static SystemProperties getInstance(){
        if(istanza==null)
            istanza = new SystemProperties();
        return istanza;
    }

    public static SystemProperties getIstanza() {
        return istanza;
    }

    public double getCpuMaxUsage() {
        return cpuMaxUsage;
    }

    public double getRamMaxUsage() {
        return ramMaxUsage;
    }

    public double getCpuMinUsage() {
        return cpuMinUsage;
    }

    public double getRamMinUsage() {
        return ramMinUsage;
    }

    private void loadProperties(){

        Properties prop = new Properties();
        try(InputStream input = new FileInputStream("config.properties")) {

            // load a properties file
            prop.load(input);
            this.cpuMaxUsage = Double.parseDouble(prop.getProperty("cpuMaxUsage"));
            this.ramMaxUsage = Double.parseDouble(prop.getProperty("ramMaxUsage"));
            this.cpuMinUsage = Double.parseDouble(prop.getProperty("cpuMinUsage"));
            this.ramMinUsage = Double.parseDouble(prop.getProperty("ramMinUsage"));


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
