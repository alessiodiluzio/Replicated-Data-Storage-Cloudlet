package com.sdcc_project.entity;

import java.io.Serializable;
import java.util.ArrayList;


public class FileLocation implements Serializable {

    private ArrayList<String> filePositions ;
    private boolean result;
    private int fileVersion;

    public ArrayList<String> getFilePositions() {
        return filePositions;
    }

    public void setFilePositions(ArrayList<String> chunkPositions) {
        this.filePositions = chunkPositions;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }
}
