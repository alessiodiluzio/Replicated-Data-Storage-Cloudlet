package com.sdcc_project.rest;
import com.sdcc_project.controller.CloudLetController;
import com.sdcc_project.entity.Record;
import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.DataNodeException;
import com.sdcc_project.exception.FileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.rmi.NotBoundException;

@SuppressWarnings("unused")
@RestController
@RequestMapping(path = "operations")
public class CloudLetRestService {

   private final CloudLetController cloudLetController;

    @Autowired
    public CloudLetRestService(CloudLetController cloudLetController) {
        this.cloudLetController = cloudLetController;
    }


    @RequestMapping(path = "", method = RequestMethod.PUT)
    public ResponseEntity<Record> putRecord(@RequestBody Record record) {
        try {
            cloudLetController.write(record);
        } catch (CloudLetException e) {
            e.printStackTrace();
            Record nullRecord = new Record();
            return new ResponseEntity<>(nullRecord,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    @RequestMapping(path="{recordID}",method = RequestMethod.GET)
    public ResponseEntity<Record> getRecord(@PathVariable String recordID){
        String result;
        Record record= new Record();
        try {
            result = cloudLetController.readFromCache(recordID);
        } catch (CloudLetException | NotBoundException | IOException | DataNodeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(record,HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(record,HttpStatus.NOT_FOUND);
        }
        record = new Record(recordID,result);
        return new ResponseEntity<>(record,HttpStatus.OK);
    }



}
