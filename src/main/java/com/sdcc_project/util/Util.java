package com.sdcc_project.util;

import java.io.*;
import java.net.URL;
import java.util.Random;

/**
 * Contiene funzioni di utilità
 */
public class Util {


    /**
     * Scrittura di un messaggio su file di log
     * @param message ...
     * @param file ...
     */
    public static void writeOutput(String message, File file){

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file,true))) {
            message+="\n";
            bw.append(message);
            bw.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Estrazione di un intero compreso tra 0 e max
     * @param max ...
     * @return intero estratto
     */
    public static int randInt(int max) {
        Random rand = new Random();
        return rand.nextInt((max) + 1);
    }

    /**
     * Effettua una chiamata REST per venire a conoscienza del proprio IP Publico
     * @return indirizzo ip ottenuto
     */
    public static String getPublicIPAddress() {
        URL whatismyip ;
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
