package com.example.android.wifidirect;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by vincentsantos on 11/10/14.
 */
public class Data {



    private int id;
    private String number;
    private String name;
    private String age;
    private String address;
    private String message;
    private double latitude;
    private double longitude;
    private String image;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getImage(){ return image;}

    public void setImage(String image){ this.image = image;}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status;
    public Data(){

    }

    public Data(String number, String name, String age, String address, String message, Double latitude, Double longitude, String image){
        super();
        this.number = number;
        this.name = name;
        this.age = age;
        this.address = address;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.image = image;
        this.status = "QUEUED";
    }
    
    public JSONObject getData(){
        JSONObject json_data = new JSONObject();
        try {
            json_data.put("number", this.number);
            json_data.put("name", this.name);
            json_data.put("age", this.age);
            json_data.put("address", this.address);
            json_data.put("message", this.message);
            json_data.put("latitude", this.latitude);
            json_data.put("longitude", this.longitude);
            json_data.put("status", this.status);
            json_data.put("image", this.image);
            json_data.put("status", this.status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json_data;
    }



    public String toString(){
        JSONObject json_data = getData();
        Iterator<?> keys = json_data.keys();
        String output = "";

        while(keys.hasNext()){
            String key = (String)keys.next();
            try {
                String value = json_data.getString(key);
                output += key+"="+value;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return output;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
