package com.sdcc_project.controller;

import com.sdcc_project.config.Config;
import com.sdcc_project.system_properties.GlobalInformation;
import com.sdcc_project.dao.CloudLetDAO;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.entity.Record;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import com.sdcc_project.service_interface.MasterInterface;
import com.sdcc_project.service_interface.StorageInterface;
import com.sdcc_project.monitor.State;
import com.sdcc_project.util.GeoLocation;
import com.sdcc_project.util.Util;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

@Service
public class CloudLetController {

    private CloudLetDAO cloudLetDAO;
    private GlobalInformation globalInformation;
    private static CloudLetController instance ;
    private static File logFile = new File("Logging_Cloudlet.txt");

    @Autowired
    private CloudLetController(CloudLetDAO cloudLetDAO) {
        this.cloudLetDAO = cloudLetDAO;
        this.globalInformation = GlobalInformation.getInstance();
    }


    public static CloudLetController getInstance(CloudLetDAO cloudLetDAO){
        if(instance == null)
            instance = new CloudLetController(cloudLetDAO);
        return instance;
    }

    public void write(Record record) throws CloudLetException {
        cloudLetDAO.insertFile(record.getRecordID(),record.getRecordData());
    }

    public String readFromCache(String filename) throws CloudLetException, DataNodeException, FileNotFoundException, IOException {
        try{
            return cloudLetDAO.readFile(filename);
        } catch (FileNotFoundException e) {
            System.out.println("File non in cache");
            Util.writeOutput(e.getMessage(),logFile);
            return read(filename);
        }
    }

    /**
     * Operazione di lettura di un file.
     * @param fileName nome del file da leggere.
     */
    public String read(String fileName) throws CloudLetException, FileNotFoundException, IOException, DataNodeException {
        FileLocation fileLocation = getFileLocation(fileName, "R");
        if (fileLocation != null) {
            System.out.println("File location non vuota");
            if (fileLocation.isResult()) {
                System.out.println("File Trovato");
                try {
                    String dataNodeAddress = fileLocation.getFilePositions().get(0);
                    StorageInterface dataNode = (StorageInterface) registryLookup(dataNodeAddress, Config.dataNodeServiceName);

                    //Il DataNode restituisce un array di byte
                    byte[] fileBytes = dataNode.read(fileName);
                    //L'array di byte è ricostruito in un file
                    FileOutputStream fos = new FileOutputStream(fileName);
                    fos.write(fileBytes);
                    fos.close();
                    BufferedReader br = new BufferedReader(new FileReader(fileName));

                    //Stampo a schermo il contenuto del file
                    String str;
                    StringBuilder output = new StringBuilder();
                    while ((str = br.readLine()) != null) {
                        output.append(str).append("\n");
                    }
                    System.out.println(output.toString());
                    br.close();
                    //Cancello il file temporaneo
                    Path path = Paths.get(fileName);
                    Files.delete(path);
                    cloudLetDAO.insertFileToReadCache(fileName, output.toString(), fileLocation.getFileVersion());
                    System.out.println("Testo " + output.toString());
                    return output.toString();
                } catch (NotBoundException e) {
                    e.printStackTrace();
                    System.out.println("ERROR IMPOSSIBLE TO CONTACT MASTER");
                    Util.writeOutput(e.getMessage(),logFile);
                    throw new CloudLetException("ERROR 500 INTERNAL SERVER ERROR");
                }
            }
            else {
                System.out.println("File location Result False");
                throw new FileNotFoundException("File not found");

            }
        }else {
            System.out.println("File location nulla");
            throw new FileNotFoundException("File not found");
        }
    }

    /**
     * Fase di richiesta della posizione di un file, il master risponde con l'indirizzo del DataNode responsabile.
     *
     * @param fileName  file da cercare.
     * @param operation operazione da effettuare sul file (in caso di scrittura di un nuovo file il Master
     *                  risponde con un DataNode scelto da lui).
     * @return Posizione del File.
     */
    public FileLocation getFileLocation(String fileName, String operation){



        try {
            MasterInterface master = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            //System.out.println("Get File Location - Result: " + fileLocation.isResult() + " - Port: "+ fileLocation.getFilePositions());
            return master.getMostUpdatedFileLocation(fileName,operation);
        }
        catch (IOException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
        } catch (FileNotFoundException e) {
            Util.writeOutput(e.getMessage(),logFile);
            System.out.println("ERROR 404 FILE NOT FOUND ERROR");
        } catch (NotBoundException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
            System.out.println("ERROR IMPOSSIBLE TO CONTACT MASTER");
        }
        return  null;
    }


    /**
     * Scrittura di un dato su un file gestito da un DataNode
     *
     * @param fileName nome del file su cui scrivere
     * @param data dati da scrivere
     */
    public void writeToMaster(String fileName, String data){
        FileLocation fileLocation = getFileLocation(fileName, "W");
        if (fileLocation != null) {
            if (fileLocation.isResult()) {
                ArrayList<String> dataNodeAddresses = fileLocation.getFilePositions();
                String primaryReplica = dataNodeAddresses.get(0);
                System.out.println("Write "+fileName+" "+data + " Port "+ primaryReplica);
                try {
                    StorageInterface dataNode = (StorageInterface) registryLookup(primaryReplica, Config.dataNodeServiceName);
                    boolean response =  dataNode.write(fileName,data,fileLocation.getFileVersion(),Config.REPLICATION_FACTORY);
                    System.out.println("Response "+ response);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Util.writeOutput(e.getMessage(),logFile);
                } catch (DataNodeException e) {
                    e.printStackTrace();
                    Util.writeOutput(e.getMessage(),logFile);
                    System.out.println("ERROR 500 INTERNAL SERVER ERROR");
                } catch (NotBoundException e) {
                    Util.writeOutput(e.getMessage(),logFile);
                    e.printStackTrace();
                    System.out.println("ERROR IMPOSSIBLE TO CONTACT MASTER");
                }

            }
        } else System.out.print("File not found!");

    }


    private Remote registryLookup(String registryHost, String serviceName) throws RemoteException, NotBoundException {

        String completeName ="//" + registryHost + ":" + Config.port + "/" + serviceName;
        System.out.println(completeName);
        Registry registry = LocateRegistry.getRegistry(registryHost,Config.port);
        return registry.lookup(completeName);

    }

    public double getLatency(String ipAddress) {
        String linuxCommandResult;
        try {
            Process p = Runtime.getRuntime().exec("ping -c 2 "+ipAddress);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String lastLine="";
            int count=0;
            while (count<=2){
                linuxCommandResult = stdInput.readLine();
                if(linuxCommandResult == null){
                    count++;
                }
                else lastLine=linuxCommandResult;
            }
            System.out.println(lastLine);
            String[] resultArray = lastLine.split("/");
            if(resultArray.length<5)
                return GeoLocation.getDistance(ipAddress,Util.getPublicIPAddress());
            return Double.parseDouble(resultArray[4]);
        } catch (IOException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
            return -1;
        }
    }

    public String getMinorLatencyCloudlet(String sourceIP){
        try {
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            return masterInterface.getMinorLatencyCloudlet(sourceIP);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
        }
        return null;

    }

    public boolean registerToMaster(){
        boolean value = false;
        try {
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            value = masterInterface.addCloudlet(Util.getLocalIPAddress());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        return value;


    }


    public void sendLifeSignal(State state) {
        try{
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            masterInterface.cloudletLifeSignal(globalInformation.getPublicIPAddress(),state);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
        }
    }

    public boolean delete(String recordID) {
        try {
            cloudLetDAO.deleteFileFromCache(recordID,1);
            cloudLetDAO.deleteFileFromCache(recordID,0);
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            return masterInterface.delete(recordID);
        } catch (CloudLetException | RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        return false;


    }
}

