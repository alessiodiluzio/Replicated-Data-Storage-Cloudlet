package com.sdcc_project;

import com.sdcc_project.config.Config;
import com.sdcc_project.config.GlobalInformation;
import com.sdcc_project.controller.CloudLetController;
import com.sdcc_project.dao.CloudLetDAO;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.rmi.registry.LocateRegistry.createRegistry;

@SpringBootApplication
public class CloudletApplication  {


    private static final int N_OF_UPDATES_SYSTEM = 20;


    private static boolean condition = true;
    private static CloudLetDAO cloudLetDAO;
    private static CloudLetController cloudLetController;


    public static void main(String[] args) {
        SpringApplication.run(CloudletApplication.class, args);
        try {
            cloudLetDAO = CloudLetDAO.getInstance();
            cloudLetController = CloudLetController.getInstance(cloudLetDAO);
        } catch (CloudLetException e) {
            e.printStackTrace();
            System.err.println("Errore nell'avvio della cache");
            System.exit(0);
        }
        final int REGISTRYPORT = Config.masterRegistryPort;

        // Controllo argomento host
        try {

            if (args.length != 1) {
                System.out.println("Usage: CloudLet <hostname> ");
                System.exit(1);
            }

            GlobalInformation globalInformation =  GlobalInformation.getInstance();

            String registryHost = args[0];

            globalInformation.setMasterAddress(Integer.toString(REGISTRYPORT));
            globalInformation.setHost(registryHost);
            //globalInformation.setCloudLetAddress(Integer.toString(CLOUDLET_PORT));

            //Registry cloudLetRegistry = createRegistry(CLOUDLET_PORT);
            //String completeName = "//" + registryHost + ":" + CLOUDLET_PORT + "/" + Config.cloudLetServiceName;
            //CloudletApplication cloudLet = new CloudletApplication();
            //cloudLetRegistry.rebind(completeName, cloudLet);

            //System.out.println("CloudLet bind " + CLOUDLET_PORT);

            asynchWrite.start();
            asynchRead.start();
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
                } catch (MasterException | CloudLetException | InterruptedException | FileNotFoundException | IOException | DataNodeException | NotBoundException e) {
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
                    System.err.print(e.getMessage());
                }
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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

}
