package com.example.android.wifidirect;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by vincentsantos on 11/10/14.
 */
public class Data {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    private String name;

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

    private String age;
    private String address;
    private String message;

    public Data(){

    }

    public Data(String name, String age, String address, String message){
        super();
        this.name = name;
        this.age = age;
        this.address = address;
        this.message = message;
    }
    
    public JSONObject getData(){
        JSONObject json_data = new JSONObject();
        try {
            json_data.put("name", this.name);
            json_data.put("age", this.age);
            json_data.put("address", this.address);
            json_data.put("message", this.message);
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
}
