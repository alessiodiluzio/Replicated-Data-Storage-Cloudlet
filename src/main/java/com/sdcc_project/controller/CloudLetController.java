package com.sdcc_project.controller;

import com.sdcc_project.config.Config;
import com.sdcc_project.system_properties.GlobalInformation;
import com.sdcc_project.dao.CloudLetDAO;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.entity.Record;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.service_interface.MasterInterface;
import com.sdcc_project.service_interface.StorageInterface;
import com.sdcc_project.monitor.State;
import com.sdcc_project.util.GeoLocation;
import com.sdcc_project.util.Util;
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

    /**
     * Inserisce un file nella cache della cloudlet
     *
     * @param record Record da inserire (Nome File e nuovo testo)
     * @throws CloudLetException ...
     */
    public void write(Record record) throws CloudLetException {
        cloudLetDAO.insertFile(record.getRecordID(),record.getRecordData());
    }

    /**
     * Lettura di un file (dalla cache o dal nucleo del sistema)
     *
     * @param filename nome del file da leggere
     * @return il contenuto del file (se esistente)
     * @throws CloudLetException ...
     * @throws DataNodeException ...
     * @throws FileNotFoundException ...
     * @throws IOException ...
     */
    public String readFromCache(String filename) throws CloudLetException, DataNodeException, FileNotFoundException, IOException {
        try{
            return cloudLetDAO.readFile(filename);
        } catch (FileNotFoundException e) {
            //Il file non è nella cache lo cerco nel nucleo del sistema.
            System.out.println("File non in cache");
            Util.writeOutput(e.getMessage(),logFile);
            return read(filename);
        }
    }

    /**
     * Operazione di lettura di un file richiesto al nucleo del sistema.
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
                    br.close();
                    //Cancello il file temporaneo
                    Path path = Paths.get(fileName);
                    Files.delete(path);
                    cloudLetDAO.insertFileToReadCache(fileName, output.toString(), fileLocation.getFileVersion());
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
     * Propagazione dell'operazione di scrittura di un file dalla cache verso il nucleo del sistema
     *
     * @param fileName nome del file su cui scrivere
     * @param data dati da scrivere
     */
    public void writeToMaster(String fileName, String data) throws RemoteException, NotBoundException, DataNodeException {
        FileLocation fileLocation = getFileLocation(fileName, "W");
        if (fileLocation != null) {
            if (fileLocation.isResult()) {
                ArrayList<String> dataNodeAddresses = fileLocation.getFilePositions();
                String primaryReplica = dataNodeAddresses.get(0);
                StorageInterface dataNode = (StorageInterface) registryLookup(primaryReplica, Config.dataNodeServiceName);
                dataNode.write(fileName,data,fileLocation.getFileVersion(),globalInformation.getReplicationFactory());



            }
        } else System.out.print("File not found!");

    }


    /**
     * Lookup di un interfaccia remota via RMI
     *
     * @param registryHost host del servizio
     * @param serviceName nome del servizio
     * @return l'interfaccia ottenuta
     * @throws RemoteException ...
     * @throws NotBoundException ...
     */
    private Remote registryLookup(String registryHost, String serviceName) throws RemoteException, NotBoundException {

        String completeName ="//" + registryHost + ":" + Config.port + "/" + serviceName;
        Registry registry = LocateRegistry.getRegistry(registryHost,Config.port);
        return registry.lookup(completeName);

    }

    /**
     * Operazione di Ping (o calcolo di geolocalizzazione) verso un host remoto per ottenere la latenza (usato per la ricerca della miglior cloudlet
     * per un dispositvo ai bordi della rete)
     *
     * @param ipAddress Host da cui calcolare la latenza
     * @return la latenza ottenuta
     */
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
            String[] resultArray = lastLine.split("/");
            //Se c'è stato un errore allora l'host non è raggiungibile (nascosta da NAT...) calcolo la distanza con
            // la geolocalizzazione
            if(resultArray.length<5)
                return GeoLocation.getDistance(ipAddress,Util.getPublicIPAddress());
            return Double.parseDouble(resultArray[4]);
        } catch (IOException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
            return -1;
        }
    }

    /**
     * Chiede al nucleo del sistema quale sia la cloudlet più "vicina" a un host
     *
     * @param sourceIP host da cui calcolare la latenza
     * @return indirizzo della cloudlet più vicina
     */
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

    /**
     *
     * Registrazione presso un master
     */
    public void registerToMaster(){

        try {
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            masterInterface.addCloudlet(Util.getPublicIPAddress());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }



    }


    /**
     * Invio del sengnale di "vita" al master di competenza
     *
     * @param state Stato in cui si trova la cloudlet
     */
    public void sendLifeSignal(State state) {
        try{
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            masterInterface.cloudletLifeSignal(globalInformation.getPublicIPAddress(),state);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            Util.writeOutput(e.getMessage(),logFile);
        }
    }

    /**
     * Cancella un file dalla cache
     * @param recordID nome del file da cancellare
     * @return risultato operazione
     */
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

    /**
     *
     * Comunica al nucleo del sistema che è pronta a spegnersi
     */
    public void sendShutdownSignal() {
        try {
            MasterInterface masterInterface = (MasterInterface) registryLookup(globalInformation.getMasterAddress(),Config.masterServiceName);
            masterInterface.shutdownCloudletSignal(globalInformation.getPublicIPAddress());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

    }
}

