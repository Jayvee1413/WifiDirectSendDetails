package com.example.android.wifidirect;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by vincentsantos on 11/10/14.
 */
public class PacketData {



    private int id;
    private String file_name;
    private int packet_no;
    private String packet;
    private String status;
    private int total_packet_cnt;

    public int getTotal_packet_cnt() {
        return total_packet_cnt;
    }

    public void setTotal_packet_cnt(int total_packet_cnt) {
        this.total_packet_cnt = total_packet_cnt;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public int getPacket_no() {
        return packet_no;
    }

    public void setPacket_no(int packet_no) {
        this.packet_no = packet_no;
    }

    public String getPacket() {
        return packet;
    }

    public void setPacket(String packet) {
        this.packet = packet;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    public PacketData(){

    }

    public PacketData(String file_name, int packet_no, int total_packet_cnt, String packet, String status){
        super();
        this.file_name = file_name;
        this.packet_no = packet_no;
        this.total_packet_cnt = total_packet_cnt;
        this.packet = packet;
        if(status == null)
            this.status = "QUEUED";
        else
            this.status = status;
    }
    
    public JSONObject getFileData(){
        JSONObject json_data = new JSONObject();
        try {
            json_data.put("file_name", this.file_name);
            json_data.put("packet_no", this.packet_no);
            json_data.put("total_packet_cnt", this.total_packet_cnt);
            json_data.put("packet", this.packet);
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