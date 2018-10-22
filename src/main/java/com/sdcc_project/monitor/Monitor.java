package com.sdcc_project.monitor;

import com.sdcc_project.system_properties.SystemProperties;
import com.sdcc_project.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Classe di monitoraggio delle risorse del sistema.
 *
 */
public class Monitor {

    private static Monitor instance;
    private double cpuUsage;
    private double memoryUsage;
    private boolean running = true;
    private boolean overCpuUsage = false;
    private boolean overRamUsage = false;
    private boolean underUsage = false;
    private SystemProperties systemProperties;

    private File file = new File("Monitor.txt");

    public static Monitor getInstance(){
        if(instance==null)
            instance = new Monitor();
        return instance;
    }

    /**
     * Thread che monitora l'uso di CPU e RAM e in caso segnala il sovra/sottoutilizzo delle risorse.
     */
    private Monitor(){
        systemProperties = SystemProperties.getInstance();
        Thread monitorThread = new Thread("MonitorThread") {
            @Override
            public void run() {
                int cpuOverUsageTime = 0;
                int ramOverUsageTime = 0;
                int cpuUnderUsageTime = 0;
                int ramUnderUsageTime = 0;
                while (running) {
                    cpuUsage = getUsage(Components.CPU);
                    memoryUsage = getUsage(Components.RAM);

                    if (cpuUsage >= systemProperties.getCpuMaxUsage())
                        cpuOverUsageTime++;
                    else {
                        cpuOverUsageTime = 0;
                        overCpuUsage = false;
                    }
                    if (memoryUsage >= systemProperties.getRamMaxUsage())
                        ramOverUsageTime++;
                    else {
                        ramOverUsageTime = 0;
                        overRamUsage = false;
                    }
                    if (cpuUsage <= systemProperties.getCpuMinUsage()) {
                        cpuUnderUsageTime++;
                    } else {
                        cpuUnderUsageTime = 0;
                        underUsage = false;
                    }
                    if (memoryUsage <= systemProperties.getRamMinUsage()) {
                        ramUnderUsageTime++;
                    } else {
                        ramUnderUsageTime = 0;
                        underUsage = false;
                    }
                    if (cpuOverUsageTime >= 8)
                        overCpuUsage = true;
                    if (ramOverUsageTime >= 8)
                        overRamUsage = true;
                    if (cpuUnderUsageTime >= 20 && ramUnderUsageTime >= 20) {
                        underUsage = true;
                    }
                    System.out.println("Uso Locale : CPU " + cpuUsage + " RAM " + " cpuUnder "+cpuUnderUsageTime+" ramUnder "+ramUnderUsageTime+ " under "+underUsage);
                    Util.writeOutput("Uso Locale : CPU " + cpuUsage + " RAM " + " cpuUnder "+cpuUnderUsageTime+" ramUnder "+ramUnderUsageTime+ " under "+underUsage, file);
                    try {
                        sleep(15000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        monitorThread.start();
    }

    /**
     * Esegue uno script BASH che calcola percentuale utilizzo di una risorsa HW
     * @param component CPU o RAM
     * @return percentuale di utilizzo
     */
    private double getUsage(Components component){
        String command ;
        if(component.equals(Components.CPU))
            command = "bash /home/ubuntu/get_cpu_usage.sh 5";
        else command = "bash /home/ubuntu/get_memory_usage.sh";
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader preader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String cpuUsageStr = preader.readLine();
            return Double.parseDouble(cpuUsageStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean isUnderUsage() {
        return underUsage;
    }


    public boolean isOverCpuUsage() {
        return overCpuUsage;
    }

    public boolean isOverRamUsage() {
        return overRamUsage;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

}
