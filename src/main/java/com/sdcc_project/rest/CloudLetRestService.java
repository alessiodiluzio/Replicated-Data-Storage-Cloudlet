package com.sdcc_project.rest;
import com.sdcc_project.controller.CloudLetController;
import com.sdcc_project.entity.Record;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.monitor.Monitor;
import com.sdcc_project.system_properties.GlobalInformation;
import com.sdcc_project.monitor.State;
import com.sdcc_project.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Servizi rest della cloudlet
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping(path = "operations")
public class CloudLetRestService {

    private final CloudLetController cloudLetController;
    private GlobalInformation globalInformation ;
    private Monitor monitor;

    @Autowired
    public CloudLetRestService(CloudLetController cloudLetController) {

        this.cloudLetController = cloudLetController;
        this.globalInformation = GlobalInformation.getInstance();
        this.monitor = Monitor.getInstance();
    }

    /**
     * Inserimento di un file (creazione o aggiornamento)
     * L'inserimento avviene nella cache della cloudlet e in seguito è propagato al nucleo del sistema
     * @param record (Nome File + Contenuto)
     * @return il record inserito
     */
    @RequestMapping(path = "", method = RequestMethod.PUT)
    public ResponseEntity<Record> putRecord(@RequestBody Record record) {
        Record nullRecord = new Record();
        if(notReply())
            return new ResponseEntity<>(nullRecord,HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            cloudLetController.write(record);
        } catch (CloudLetException e) {
            e.printStackTrace();

            return new ResponseEntity<>(nullRecord,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    /**
     * Servizio REST di Get di un file
     * @param recordID nome del file richiesto
     * @return il contenuto del file
     */
    @RequestMapping(path="{recordID}",method = RequestMethod.GET)
    public ResponseEntity<String> getRecord(@PathVariable String recordID){
        String result;
        Record record= new Record();
        if(notReply())
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            result = cloudLetController.readFromCache(recordID);
        } catch (CloudLetException | IOException | DataNodeException e) {
            e.printStackTrace();
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>("",HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result,HttpStatus.OK);
    }

    @RequestMapping(path="ciao",method = RequestMethod.GET)
    public ResponseEntity<String> saluta(){
        if(notReply())
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>("CIAO!",HttpStatus.OK);
    }

    /**
     * Servizio REST con cui un dispositivo ai bordi della rete richiede l'indirizzo della cloudlet
     * piu vicina a se.
     * @param sourceIP indirizzo IP del dispositivo richiedente
     * @return L'indirizzo della cloudlet piu vicina
     */
    @RequestMapping(path="/best_cloudlet/{sourceIP}",method = RequestMethod.GET)
    public ResponseEntity<String> getMinorLatencyCloudlet(@PathVariable String sourceIP){
        if(notReply())
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        String result = cloudLetController.getMinorLatencyCloudlet(sourceIP);
        if(!result.equals(""))
            return new ResponseEntity<>(result,HttpStatus.OK);
        else return new ResponseEntity<>("No CloudLet Available",HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path="/register",method= RequestMethod.PUT)
    public ResponseEntity<Boolean> register(){
        if(notReply())
            return new ResponseEntity<>(false,HttpStatus.INTERNAL_SERVER_ERROR);
        cloudLetController.registerToMaster();
        return new ResponseEntity<>(true,HttpStatus.OK);
    }

    /**
     * Delete di un file
     * @param recordID Nome del file da cancellare
     * @return risultato dell'esecuzione
     */
    @RequestMapping(path="/{recordID}",method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> delete(@PathVariable String recordID){
        if(notReply())
            return new ResponseEntity<>(false,HttpStatus.INTERNAL_SERVER_ERROR);
        if(cloudLetController.delete(recordID)){
            return new ResponseEntity<>(true,HttpStatus.OK);
        }
        return new ResponseEntity<>(false,HttpStatus.NOT_FOUND);

    }

    /**
     * Metodo per decidere se rispondere o meno a una richiesta REST
     * - Se la cloudlet è marcata per la cancellazione non risponde piu ai dispositivi di modo che trovino
     * un altra cloudlet a cui agganciarsi.
     * - Se la cloudlet è BUSY allora risponde affermativamente con una probabilità proporzionale all'utilizzo della CPU
     *
     * @return
     */
    private boolean notReply(){
        if(globalInformation.getState().equals(State.NORMAL))
            return false;
        else if(globalInformation.getState().equals(State.DELETING)){
            return true;
        }
        int max = (int) monitor.getCpuUsage();
        int extracted = Util.randInt(100);
        return extracted <= max;
    }





}
