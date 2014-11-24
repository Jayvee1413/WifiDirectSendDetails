package com.example.android.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;

/**
 * Created by vincentsantos on 11/24/14.
 */
public class WifiConnService{
    private ArrayList<WifiP2pDevice> peers;

    public WifiConnService(ArrayList<WifiP2pDevice> peers){
        this.peers = peers;
    }



}
