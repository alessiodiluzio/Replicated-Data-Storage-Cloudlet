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


    @RequestMapping(path = "", method = RequestMethod.PUT)
    public ResponseEntity<Record> putRecord(@RequestBody Record record) {
        Record nullRecord = new Record();
        if(!reply())
            return new ResponseEntity<>(nullRecord,HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            cloudLetController.write(record);
        } catch (CloudLetException e) {
            e.printStackTrace();

            return new ResponseEntity<>(nullRecord,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    @RequestMapping(path="{recordID}",method = RequestMethod.GET)
    public ResponseEntity<Record> getRecord(@PathVariable String recordID){
        String result;
        Record record= new Record();
        if(!reply())
            return new ResponseEntity<>(record,HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            result = cloudLetController.readFromCache(recordID);
        } catch (CloudLetException | IOException | DataNodeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(record,HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(record,HttpStatus.NOT_FOUND);
        }
        record = new Record(recordID,result);
        return new ResponseEntity<>(record,HttpStatus.OK);
    }

    @RequestMapping(path="ciao",method = RequestMethod.GET)
    public ResponseEntity<String> saluta(){
        if(!reply())
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>("CIAO!",HttpStatus.OK);
    }

    @RequestMapping(path="/best_cloudlet/{sourceIP}",method = RequestMethod.GET)
    public ResponseEntity<String> getMinorLatencyCloudlet(@PathVariable String sourceIP){
        if(!reply())
            return new ResponseEntity<>("",HttpStatus.INTERNAL_SERVER_ERROR);
        String result = cloudLetController.getMinorLatencyCloudlet(sourceIP);
        if(!result.equals(""))
            return new ResponseEntity<>(result,HttpStatus.OK);
        else return new ResponseEntity<>("No CloudLet Available",HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path="/register",method= RequestMethod.PUT)
    public ResponseEntity<Boolean> register(){
        boolean value = cloudLetController.registerToMaster();
        return new ResponseEntity<>(value,HttpStatus.OK);
    }

    private boolean reply(){
        if(globalInformation.getState().equals(State.NORMAL))
            return true;
        int max = (int) monitor.getCpuUsage();
        int extracted = Util.randInt(100);
        if(extracted>max)
            return true;
        return false;
    }





}
