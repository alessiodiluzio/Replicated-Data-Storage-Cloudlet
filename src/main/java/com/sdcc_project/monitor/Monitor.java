package com.sdcc_project.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Monitor {

    private static Monitor instance;
    private double cpuUsage;
    private double memoryUsage;
    private boolean running = true;

    public static Monitor getInstance(){
        if(instance==null)
            instance = new Monitor();
        return instance;
    }

    private Monitor(){
        monitorThread.start();
    };

    private double getUsage(Components component){
        String command = "";
        if(component.equals(Components.CPU))
            command = "bash /home/ubuntu/get_cpu_usage.sh 0.2";
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

    private Thread monitorThread = new Thread("MonitorThread"){
        @Override
        public void run() {
            while (running){
                cpuUsage = getUsage(Components.CPU);
                memoryUsage = getUsage(Components.RAM);
                try {
                    sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void setRunning(boolean running) {
        this.running = running;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }
}
