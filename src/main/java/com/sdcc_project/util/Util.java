package com.sdcc_project.util;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Random;

public class Util {
    public static String getLocalIPAddress(){
        InetAddress ip;
        try {

            ip = InetAddress.getLocalHost();
            return ip.getHostAddress();

        } catch (UnknownHostException e) {

            e.printStackTrace();

        }
        return null;
    }

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

    public static int randInt(int max) {
        Random rand = new Random();
        return rand.nextInt((max) + 1);
    }

    public static String getPublicIPAddress() {
        URL whatismyip = null;
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            return in.readLine();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
