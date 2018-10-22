package com.sdcc_project.dao;

import com.sdcc_project.exception.CloudLetException;
import com.sdcc_project.exception.FileNotFoundException;
import com.sdcc_project.exception.MasterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class CloudLetDAO {

    private static CloudLetDAO instance;
    private Connection connection = null;


    @Autowired
    private CloudLetDAO() throws CloudLetException {
        System.out.println("Creo cloudlet dao");
        loadDB();
        instance = this;
    }

    public static CloudLetDAO getInstance() throws CloudLetException {
        if(instance==null)
            instance = new CloudLetDAO();
        return instance;
    }


    /**
     * Prende il numero di versio piu alto per un file nella cache di scrittura
     *
     * @param filename nome del file di cui si desidera la versione
     * @return la versione richiesta
     * @throws FileNotFoundException ...
     * @throws CloudLetException ...
     */
    private int getFileLastVersion(String filename) throws FileNotFoundException, CloudLetException {
        String query = "SELECT MAX(version) FROM CloudLetWriteTable WHERE filename=?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1,filename);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return resultSet.getInt(1);
            }
            throw new FileNotFoundException("File not in cache");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB Error");
        }
    }


    /**
     * Preleva n righe dalla cache di scrittura per propagarne l'aggiornamento verso il nucleo del sistema
     *
     * @param nrows numero di righe da prelevare
     * @return una lista di file da inviare al nucleo del sistema
     * @throws CloudLetException ...
     */
    public ArrayList<String> getTableRows(int nrows) throws CloudLetException {
        ArrayList<String> fileNames = new ArrayList<>();
        String query = "SELECT DISTINCT filename FROM CloudLetWriteTable OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1,nrows);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                fileNames.add(resultSet.getString(1));
            }
            return fileNames;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB Exception");
        }
    }

    /**
     * Preleva il contenuto di un file dalla cache di scrittura
     *
     * @param fileName nome del file da prelevare
     * @return contenuto del file .
     * @throws CloudLetException ...
     */
    public ArrayList<String> getFileData(String fileName) throws CloudLetException {
        ArrayList<String> fileData = new ArrayList<>();
        String query = "SELECT data FROM CloudLetWriteTable WHERE filename = ? ORDER BY version ";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1,fileName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                Blob blobData = resultSet.getBlob(1);
                String result = new String(blobData.getBytes(1L, (int) blobData.length()));
                fileData.add(result);
            }
            return fileData;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB exception");
        }
    }

    /**
     * Crea le tabelle del DB locale alla cloudlet
     *
     * @throws CloudLetException ...
     */
    private  void createTable() throws CloudLetException {
        try (Statement createStatement = connection.createStatement()) {
            String createWTBL = "create TABLE CloudLetWriteTable(filename varchar(50),data blob,version int)";
            String createRTBL = "create TABLE CloudLetReadTable(filename varchar(50),data blob,version int)";
            createStatement.execute(createWTBL);
            createStatement.execute(createRTBL);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("Impossible to create Table");
        }

    }

    /**
     * Preleva l'elenco delle sottoscrizione della cloudlet.
     * Una cloudlet si sottoscrive ad un file non appena un dispositivo ne richiede la lettura ,
     * in seguito la cloudlet ne preleva automaticamente gli aggiornamenti e li mantiene in cache di lettura.
     *
     * @return sottoscrizioni della cloudlet
     * @throws MasterException ...
     */
    public HashMap<String,Integer> getSubscritption() throws MasterException {
        String query ="SELECT filename,version FROM CloudLetReadTable";
        HashMap<String,Integer> result = new HashMap<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                result.put(resultSet.getString(1),resultSet.getInt(2));
            }
            resultSet.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MasterException("DB Error");
        }
    }

    /**
     * Lettura di un file dalla cache di lettura
     *
     * @param filename nome del file da leggere
     * @return contenuto del file
     * @throws FileNotFoundException ...
     * @throws CloudLetException ...
     */
    public String readFile(String filename) throws FileNotFoundException, CloudLetException {
        String query = "SELECT data FROM CloudLetReadTable WHERE filename=?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1,filename);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                Blob blobData = resultSet.getBlob(1);
                return new String(blobData.getBytes(1L, (int) blobData.length()));
            }
            throw new FileNotFoundException("File not in cache");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB ERROR");
        }
    }

    /**
     * Inserisce un file nella cache di scrittura
     *
     * @param filename nome del file da inserire
     * @param data contenuto del file
     * @throws CloudLetException ...
     */
    public synchronized void insertFile(String filename,String data) throws CloudLetException {
        String query = "INSERT into CloudLetWriteTable values(?,?,?)";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            int version = 0;
            try {
                version = getFileLastVersion(filename);
            } catch (FileNotFoundException  e) {
                System.out.println("File non in cache");
            } catch (CloudLetException e) {
                e.printStackTrace();
            }

            byte[] byteData = data.getBytes();//Better to specify encoding
            Blob blobData = connection.createBlob();
            blobData.setBytes(1, byteData);

            preparedStatement.setString(1,filename);
            preparedStatement.setBlob(2,blobData);
            preparedStatement.setInt(3,version+1);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB Error");
        }

    }

    /**
     * Cancella un file da una cache
     *
     * @param filename nome del file da cancellare
     * @param cacheType (1 per cache di lettura ,qualsiasi altro valore cache di scrittura)
     * @throws CloudLetException ...
     */
    public void deleteFileFromCache(String filename,int cacheType) throws CloudLetException {
        String query ;
        if(cacheType==1){
            query ="DELETE FROM CloudLetReadTable WHERE filename=?";
        }
        else  query ="DELETE FROM CloudLetWriteTable WHERE filename=?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1,filename);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB ERROR");
        }
    }

    /**
     * Inserimento di un file in cache di lettura
     *
     * @param filename nome del file
     * @param data contenuto
     * @param version versione
     * @throws CloudLetException ...
     */
    public void insertFileToReadCache(String filename,String data,int version) throws CloudLetException {
        deleteFileFromCache(filename,1);
        String query = "INSERT INTO CloudLetReadTable values (?,?,?)";
        try(PreparedStatement preparedStatement =connection.prepareStatement(query)){
            preparedStatement.setString(1,filename);
            byte[] byteData = data.getBytes();//Better to specify encoding
            Blob blobData = connection.createBlob();
            blobData.setBytes(1, byteData);
            preparedStatement.setBlob(2,blobData);
            preparedStatement.setInt(3,version);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudLetException("DB Error");
        }

    }

    /**
     * Operazioni di avvio del database locale alla cache
     *
     */
    private void loadDB() throws CloudLetException {
        try{
            createDB();
            System.out.println(connection.isClosed());
        } catch (Exception e) {
            e.printStackTrace();
            throw new CloudLetException("Impossible to load/create DB");
        }
    }

    /**
     * Creazione del db locale alla cache
     */
    private void createDB()  {
        String dbUri = "jdbc:derby:memory:"+"cloudLet" +"DB"+";create=true;user="
                +"cloudlet"+";password="+"cloudlet";
        DataSource dataSource = DataSource.getInstance();
        try {
            this.connection = dataSource.getConnection(dbUri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            createTable();
        } catch (CloudLetException e) {
            e.printStackTrace();
        }

    }

}
