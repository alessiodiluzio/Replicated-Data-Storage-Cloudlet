package com.sdcc_project.controller;

import com.sdcc_project.config.Config;
import com.sdcc_project.config.GlobalInformation;
import com.sdcc_project.dao.CloudLetDAO;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.entity.Record;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import com.sdcc_project.service_interface.MasterInterface;
import com.sdcc_project.service_interface.StorageInterface;
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
            if (fileLocation.isResult()) {
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
                    throw new CloudLetException("ERROR 500 INTERNAL SERVER ERROR");
                }
            }
            else throw new FileNotFoundException("File not found");
        }else throw new FileNotFoundException("File not found");
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
            return master.checkFile(fileName,operation);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (MasterException e) {

            System.out.println("ERROR 500 INTERNAL SERVER ERROR");
        } catch (FileNotFoundException e) {
            System.out.println("ERROR 404 FILE NOT FOUND ERROR");
        } catch (NotBoundException e) {
            e.printStackTrace();
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
                    String response = dataNode.write(data,fileName,dataNodeAddresses,fileLocation.getFileVersion(),null);
                    System.out.println("Response "+ response);
                }
                catch (IOException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    System.out.println("ERROR 404 FILE NOT FOUND");
                } catch (DataNodeException e) {
                    e.printStackTrace();
                    System.out.println("ERROR 500 INTERNAL SERVER ERROR");
                } catch (NotBoundException e) {
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



}

