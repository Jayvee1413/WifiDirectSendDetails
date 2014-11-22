package com.example.android.wifidirect;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by vincentsantos on 11/22/14.
 */
public interface DeviceActionListener {

    void showDetails(WifiP2pDevice device);

    void cancelDisconnect();

    void connect(WifiP2pConfig config);

    void disconnect();
}
