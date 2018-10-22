package com.sdcc_project.entity;

/**
 * Classe usata per l'invio di dati di un file con metodi REST
 */
@SuppressWarnings("unused")
public class Record {
    private String recordID;
    private String recordData;

    public Record() {
    }

    public Record(String recordID, String recordData) {
        this.recordID = recordID;
        this.recordData = recordData;
    }

    public String getRecordID() {
        return recordID;
    }


    public String getRecordData() {
        return recordData;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public void setRecordData(String recordData) {
        this.recordData = recordData;
    }
}
