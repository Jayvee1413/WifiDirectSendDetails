package com.example.android.wifidirect;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by vincentsantos on 11/10/14.
 */
public class FileData {



    private int id;
    private String name;
    private String status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public FileData(){

    }

    public FileData(String name){
        super();
        this.name = name;
        this.status = "QUEUED";
    }
    
    public JSONObject getFileData(){
        JSONObject json_data = new JSONObject();
        try {
            json_data.put("name", this.name);
            json_data.put("status", this.status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json_data;
    }



    public String toString(){
        JSONObject json_data = getFileData();
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
