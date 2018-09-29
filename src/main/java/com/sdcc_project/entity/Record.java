package com.sdcc_project.entity;

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

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public String getRecordData() {
        return recordData;
    }

    public void setRecordData(String recordData) {
        this.recordData = recordData;
    }
}
