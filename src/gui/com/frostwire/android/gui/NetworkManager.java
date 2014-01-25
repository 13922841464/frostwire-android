/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.CoreRuntimeException;
import com.frostwire.android.util.ByteUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class NetworkManager {

    private static final String TAG = "FW.NetworkManager";

    private static Field inetAddressHostNameField;

    // The following constants are from the android sdk source code
    // It does not matter if some constants are from a newer android version,
    // since only the latest phone can go to the specified state.

    /**
     * Current network is UMTS - aka GSM's 3G (AT&T)
     */
    private static final int NETWORK_TYPE_UMTS = 3;

    /**
     * Current network is EVDO revision B - aka CDMA's 3G (Verizon)
     */
    private static final int NETWORK_TYPE_EVDO_B = 12;

    /**
     * Verizon's 4G LTE (as seen on the Thunderbolt)
     */
    private static final int NETWORK_TYPE_4G_LTE = 13;

    /**
     * Verizon's 4G eHRPD 
     * http://developer.motorola.com/docstools/library/detecting-and-using-lte-networks/
     */
    private static final int NETWORK_TYPE_4G_EHRPD = 14;

    /**
     * The Default WiMAX data connection.  When active, all data traffic
     * will use this connection by default.  Should not coexist with other
     * default connections.
     */
    private static final int TYPE_WIMAX = 6;

    private final Application context;

    private int listeningPort;

    static {
        try {
            inetAddressHostNameField = InetAddress.class.getDeclaredField("hostName");
            inetAddressHostNameField.setAccessible(true);
        } catch (Throwable e) {
            Log.e(TAG, "Error getting inetAddressHostNameField", e);
        }
    }

    private static NetworkManager instance;

    public synchronized static void create(Application context) {
        if (instance != null) {
            return;
        }
        instance = new NetworkManager(context);
    }

    public static NetworkManager instance() {
        if (instance == null) {
            throw new CoreRuntimeException("NetworkManager not created");
        }
        return instance;
    }

    private NetworkManager(Application context) {
        this.context = context;

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_RANDOM_LISTENING_PORT)) {
            listeningPort = ByteUtils.randomInt(40000, 49999);
        } else {
            listeningPort = Constants.GENERIC_LISTENING_PORT;
        }
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public boolean isDataUp() {
        // boolean logic trick, since sometimes android reports WIFI and MOBILE up at the same time
        return (isDataWIFIUp() != isDataMobileUp()) || isDataWiMAXUp();
    }

    public boolean isDataMobileUp() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public boolean isData3GUp() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && (networkInfo.getSubtype() == NETWORK_TYPE_UMTS || networkInfo.getSubtype() == NETWORK_TYPE_EVDO_B) && networkInfo.isConnected();
    }

    public boolean isDataWIFIUp() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public boolean isDataWiMAXUp() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(TYPE_WIMAX);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public boolean isData4GUp() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && (networkInfo.getSubtype() == NETWORK_TYPE_4G_LTE || networkInfo.getSubtype() == NETWORK_TYPE_4G_EHRPD) && networkInfo.isConnected();
    }

    /**
     * This method returns the current active network interface that it's not the loop back.
     * Until now, there is no evidence that at any given time, could be more than one (no loop back)
     * interfaces up.
     * 
     * @return
     */
    public NetworkInterface getNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress()) {
                        return networkInterface;
                    }
                }
            }

            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    public void printNetworkInfo() {
        String str = "";

        try {
            if (!isDataUp()) {
                str = context.getString(R.string.not_connected);
            } else if (isDataMobileUp()) {
                str = context.getString(R.string.mobile_data);
            } else if (isDataWIFIUp()) {
                String ipPortInfo = "";
                NetworkInterface ni = getNetworkInterface();

                if (ni != null) {
                    ipPortInfo = getNetworkInterface().getInetAddresses().nextElement().getHostAddress() + ":" + getListeningPort();
                }

                WifiManager wifi = getWifiManager();
                WifiInfo wifiInfo = wifi != null ? wifi.getConnectionInfo() : null;
                String ssid = wifiInfo != null ? wifiInfo.getSSID() : null;
                String wifiNameInfo = "";

                if (ssid != null) {
                    wifiNameInfo += "WiFi: " + ssid;
                }

                str = wifiNameInfo + ", Addr: " + ipPortInfo;
            } else {
                str = "";
            }
        } catch (Throwable e) {
            // ignore, not a problem, only for debugging
        }

        Log.i(TAG, str);
    }

    public WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Application.WIFI_SERVICE);
    }

    public InetAddress getMulticastInetAddress() throws IOException {
        WifiManager wifi = getWifiManager();
        int intaddr = wifi.getConnectionInfo().getIpAddress();
        byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
        return InetAddress.getByAddress(byteaddr);
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) context.getSystemService(Application.CONNECTIVITY_SERVICE);
    }
}