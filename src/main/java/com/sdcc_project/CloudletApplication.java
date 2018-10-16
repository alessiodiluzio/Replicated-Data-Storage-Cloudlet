package com.sdcc_project;
import com.sdcc_project.config.Config;
import com.sdcc_project.system_properties.GlobalInformation;
import com.sdcc_project.controller.CloudLetController;
import com.sdcc_project.dao.CloudLetDAO;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import com.sdcc_project.monitor.Monitor;
import com.sdcc_project.service_interface.CloudletInterface;
import com.sdcc_project.util.Util;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@SpringBootApplication
public class CloudletApplication extends UnicastRemoteObject implements CloudletInterface {


    private static final int N_OF_UPDATES_SYSTEM = 20;

    private static File file = new File("log.txt");
    private static boolean condition = true;
    private static CloudLetDAO cloudLetDAO;
    private static CloudLetController cloudLetController;
    private static Monitor monitor;
    private static GlobalInformation globalInformation;


    protected CloudletApplication() throws RemoteException {
        super();
    }


    public static void main(String[] args) {
        SpringApplication.run(CloudletApplication.class, args);
        Util.writeOutput("Avviata applicazione",file);
        monitor = Monitor.getInstance();
        try {
            cloudLetDAO = CloudLetDAO.getInstance();
            cloudLetController = CloudLetController.getInstance(cloudLetDAO);
            Util.writeOutput("Creati dao e controller ",file);
        } catch (CloudLetException e) {
            e.printStackTrace();
            Util.writeOutput("Errore nell'avvio della cache",file);
            System.exit(0);
        }
        // Controllo argomento host
        try {

            if (args.length != 1) {
                Util.writeOutput("Usage: CloudLet <hostname> ",file);
                System.exit(1);
            }

            globalInformation =  GlobalInformation.getInstance();

            String registryHost = args[0];

            globalInformation.setMasterAddress(registryHost);
            globalInformation.setHost(registryHost);

            //Registro oggetto remoto
            Registry registry = LocateRegistry.createRegistry(Config.port);
            String completeName = "//" + Util.getLocalIPAddress() + ":" + Config.port + "/" + Config.cloudLetServiceName;
            CloudletApplication cloudletApplication  = new CloudletApplication();
            registry.rebind(completeName,cloudletApplication);

            asynchWrite.start();
            asynchRead.start();
            lifeThread.start();
            Runtime.getRuntime().addShutdownHook(
                    new Thread("app-shutdown-hook") {
                        @Override
                        public void run() {
                            condition = false;
                            try {
                                asynchRead.join();
                                asynchWrite.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } catch (Exception e) {
            System.err.println("CloudLet exception: ");
            e.printStackTrace();
        }

    }

    private static Thread asynchRead = new Thread("AsynchRead"){
        @Override
        public void run() {
            while (condition){
                try{
                    HashMap<String,Integer> subscriptions = cloudLetDAO.getSubscritption();
                    for(Map.Entry<String,Integer> subscription : subscriptions.entrySet()){
                        FileLocation location = cloudLetController.getFileLocation(subscription.getKey(),"R");
                        if(location == null || !location.isResult()) continue;
                        if(location.getFileVersion()>subscription.getValue()){
                            System.out.println("PULL DELL AGGIORNAMENTO DI "+subscription.getKey());
                            cloudLetController.read(subscription.getKey());
                        }
                    }
                    sleep(5000);
                } catch (MasterException | CloudLetException | InterruptedException | FileNotFoundException | IOException | DataNodeException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static Thread asynchWrite = new Thread("AsynchWrite"){
        @Override
        public void run() {
            System.out.flush();
            while (condition){
                try {
                    ArrayList<String> fileToWrite = cloudLetDAO.getTableRows(N_OF_UPDATES_SYSTEM);
                    for(String file : fileToWrite){
                        ArrayList<String> data = cloudLetDAO.getFileData(file);
                        cloudLetDAO.deleteFileFromCache(file,0);
                        String writeData = concatenateArrayOfString(data);
                        System.out.println("PUSH DELL AGGIORNAMENTO DI "+file + " "+writeData);
                        cloudLetController.writeToMaster(file,writeData);
                    }

                } catch (CloudLetException e) {
                    e.printStackTrace();
                    Util.writeOutput(e.getMessage(),file);
                    System.err.print(e.getMessage());
                }
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    Util.writeOutput(e.getMessage(),file);
                    e.printStackTrace();
                }
            }
        }
    };

    private static Thread lifeThread = new Thread("LifeThread"){
        @Override
        public void run() {
            while(condition){
                if(monitor.isOverCpuUsage() || monitor.isOverRamUsage()){
                    globalInformation.setState(com.sdcc_project.monitor.State.BUSY);
                    System.out.println("MemoryUsage "+monitor.getMemoryUsage()+" CpuUsage "+monitor.getCpuUsage()+" state "+
                    globalInformation.getState().toString());
                }
                else globalInformation.setState(com.sdcc_project.monitor.State.NORMAL);
                cloudLetController.sendLifeSignal(globalInformation.getState());
                try {
                    sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Util.writeOutput(e.getMessage(),file);
                }
            }
        }
    };

    private static String concatenateArrayOfString(ArrayList<String> arrayList){
        StringBuilder result = new StringBuilder();
        for(String str : arrayList){
            result.append(str).append("\n");
        }
        return result.toString();

    }

    @Override
    public Double getLatency(String ipAddress) throws RemoteException {
        return cloudLetController.getLatency(ipAddress);
    }

    @Override
    public boolean newMasterAddress(String newMasterAddress) throws RemoteException {
        globalInformation.setMasterAddress(newMasterAddress);
        return true;
    }
}
