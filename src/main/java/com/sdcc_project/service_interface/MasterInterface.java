package com.sdcc_project.service_interface;

import com.sdcc_project.entity.DataNodeStatistic;
import com.sdcc_project.entity.FileLocation;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import com.sdcc_project.monitor.State;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface MasterInterface extends Remote {

    FileLocation checkFile(String fileName, String operation) throws RemoteException, FileNotFoundException, MasterException;
    void writeAck(String filename, String port, int version, String oldPort) throws RemoteException;
    void setStatistic(DataNodeStatistic dataNodeStatistic) throws RemoteException;
    void lifeSignal(String port) throws RemoteException;
    ArrayList<String> getDataNodeAddresses() throws RemoteException;
    void dataNodeToManage(ArrayList<String> addresses) throws RemoteException;
    String getMinorLatencyCloudlet(String sourceIP) throws RemoteException;
    ArrayList<String> getMinorLatencyLocalCloudlet(String sourceIP) throws RemoteException;
    boolean addCloudlet(String ipAddress) throws RemoteException;
    void cloudletLifeSignal(String address, State state) throws RemoteException;
}
