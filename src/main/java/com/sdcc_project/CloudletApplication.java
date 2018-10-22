package com.sdcc_project;
import com.sdcc_project.config.Config;
import com.sdcc_project.monitor.State;
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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@SpringBootApplication
public class CloudletApplication extends UnicastRemoteObject implements CloudletInterface {


    private static final int N_OF_UPDATES_SYSTEM = 20;

    private static File file = new File("log.txt");
    private static boolean condition = true;
    private static CloudLetDAO cloudLetDAO;
    private static CloudLetController cloudLetController;
    private static Monitor monitor;
    private static GlobalInformation globalInformation;
    private static Registry registry;
    private static String completeName;
    private static CloudletApplication publishedObject;


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

            if (args.length != 2) {
                Util.writeOutput("Usage: CloudLet <master-hostname> <replication-factory>",file);
                System.exit(1);
            }

            globalInformation =  GlobalInformation.getInstance();

            String registryHost = args[0];

            globalInformation.setMasterAddress(registryHost);
            globalInformation.setReplicationFactory(Integer.parseInt(args[1]));
            //Registro oggetto remoto
            registry = LocateRegistry.createRegistry(Config.port);
            completeName = "//" + Util.getPublicIPAddress() + ":" + Config.port + "/" + Config.cloudLetServiceName;
            publishedObject  = new CloudletApplication();
            registry.rebind(completeName,publishedObject);
            System.setProperty("java.rmi.server.hostname", Objects.requireNonNull(Util.getPublicIPAddress()));
            asynchWrite.start();
            asynchRead.start();
            lifeThread.start();
        } catch (Exception e) {
            System.err.println("CloudLet exception: ");
            e.printStackTrace();
        }

    }

    /**
     * Thread per l'aggiornamento della cache di lettura.
     * Si cercano gli aggiornamenti per tutti file sottoscritti ,ovvero per quelli richiesti almeno una volta alla cloudlet
     */
    private static Thread asynchRead = new Thread("AsynchRead"){
        @Override
        public void run() {
            while (condition){
                try{
                    HashMap<String,Integer> subscriptions = cloudLetDAO.getSubscritption();
                    for(Map.Entry<String,Integer> subscription : subscriptions.entrySet()){
                        FileLocation location = cloudLetController.getFileLocation(subscription.getKey(),"R");
                        if(location == null || !location.isResult()) {
                            cloudLetController.delete(subscription.getKey());
                        }
                        else if(location.getFileVersion()>subscription.getValue()){
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

    /**
     * Thread che propaga gli aggiornamenti dei file dalla cache di scrittura al nucleo del sistema
     *
     */
    private static Thread asynchWrite = new Thread("AsynchWrite"){
        @Override
        public void run() {
            System.out.flush();
            while (condition){
                try {
                    ArrayList<String> fileToWrite = cloudLetDAO.getTableRows(N_OF_UPDATES_SYSTEM);
                    //Se il master ha richiesto lo spegnimento della cloudlet e la cache di scrittura è vuota la cloudlet
                    // di essere pronta per la cancellazione
                    if(fileToWrite.isEmpty() && globalInformation.getState().equals(com.sdcc_project.monitor.State.DELETING)){
                        Util.writeOutput("SEQUENZA DI SPEGNIMENTO",file);
                        cloudLetController.sendShutdownSignal();
                        terminate();
                        System.exit(1);
                    }
                    for(String file : fileToWrite){
                        ArrayList<String> data = cloudLetDAO.getFileData(file);
                        String writeData = concatenateArrayOfString(data);
                        cloudLetController.writeToMaster(file,writeData);
                        cloudLetDAO.deleteFileFromCache(file,0);
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

    /**
     * Thread che invia segnali di vita al nucleo del sistema
     * - Busy Richiede di creare una nuova cloudlet con cui condivedere il carico
     * - Free risorse sottoutilizzate la cloudlet potrebbe essere cancellata
     * - Normal
     */
    private static Thread lifeThread = new Thread("LifeThread"){
        @Override
        public void run() {
            while(condition){
                if(monitor.isOverCpuUsage() || monitor.isOverRamUsage()){
                    globalInformation.setState(com.sdcc_project.monitor.State.BUSY);
                }
                else if(monitor.isUnderUsage()){
                    globalInformation.setState(com.sdcc_project.monitor.State.FREE);
                }
                else globalInformation.setState(com.sdcc_project.monitor.State.NORMAL);
                cloudLetController.sendLifeSignal(globalInformation.getState());
                try {
                    sleep(7000);
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
    public Double getLatency(String ipAddress)  {
        return cloudLetController.getLatency(ipAddress);
    }

    /**
     * Servizio RMI con cui viene comunicato alla cloudlet di "appartenere" a un nuovo master
     * @param newMasterAddress Nuovo master di competenza
     */
    @Override
    public void newMasterAddress(String newMasterAddress)  {
        globalInformation.setMasterAddress(newMasterAddress);
        Util.writeOutput("nuovo indirizzo master "+newMasterAddress,file);
    }

    /**
     * Viene richiesto alla cloudlet di ultimare le operazioni prima dello spegnimento
     */
    @Override
    public void shutdownSignal()  {
        Util.writeOutput("DELETE SIGNAL ",file);
        globalInformation.setState(State.DELETING);
    }


    /**
     * Termina il processo cloudlet
     */
    public static void terminate() {
        try {
            registry.unbind(completeName);
            UnicastRemoteObject.unexportObject(publishedObject, true);
        }
        catch (RemoteException | NotBoundException e) {
            Util.writeOutput(e.getMessage(),file);
        }
        Util.writeOutput("Termination...",file);
        System.exit(0);
    }
}
