package com.sdcc_project.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;

public class GeoLocation {

    public static float getDistance(String ip1,String ip2){
        if(ip1 == null || ip2 ==null)
            return -1;
        ArrayList<Float> ip1Coordinates = getLatLong(ip1);
        ArrayList<Float> ip2Coordinates = getLatLong(ip2);
        return  distFrom(ip1Coordinates.get(0),ip1Coordinates.get(1),ip2Coordinates.get(0),ip2Coordinates.get(1));
    }

    private static ArrayList<Float> getLatLong(String ip){
        String url = "http://api.ipstack.com/"+ip+"?access_key=7856739a9726953f2cd9d48014c2ddd3";
        ArrayList<Float> result=new ArrayList<Float>();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        // add request header
        //request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = null;
        try {
            response = client.execute(request);
            response.getEntity().getContent();
            JSONObject jsonObject = new JSONObject(new JSONTokener(response.getEntity().getContent()));
            result.add(jsonObject.getFloat("latitude"));
            result.add(jsonObject.getFloat("longitude"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);

        return dist;
    }
}
