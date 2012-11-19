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

package com.frostwire.gui.upnp.android;

import java.net.InetAddress;

import org.teleal.cling.UpnpService;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;

import android.content.ServiceConnection;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.gui.upnp.PingInfo;
import com.frostwire.gui.upnp.UPnPFWDevice;
import com.frostwire.gui.upnp.UPnPFWDeviceInfo;
import com.frostwire.gui.upnp.UPnPManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class AndroidUPnPManager extends UPnPManager {

    private UPnPServiceConnection serviceConnection;

    public AndroidUPnPManager() {
        serviceConnection = new UPnPServiceConnection(registryListener);
    }

    public ServiceConnection getServiceConnection() {
        return serviceConnection;
    }

    @Override
    public UpnpService getService() {
        return serviceConnection.getService();
    }

    @Override
    public LocalDevice getLocalDevice() {
        return UPnPServiceConnection.getLocalDevice();
    }

    @Override
    public UPnPFWDevice getUPnPLocalDevice() {
        AndroidUPnPFWDeviceDesc desc = new AndroidUPnPFWDeviceDesc();

        LocalService<?>[] services = new LocalService<?>[] { getInfoService() };

        return new UPnPFWDevice(desc, services);
    }

    @Override
    public PingInfo getLocalPingInfo() {
        PingInfo p = new PingInfo();

        p.uuid = ConfigurationManager.instance().getUUIDString();
        p.listeningPort = NetworkManager.instance().getListeningPort();
        p.numSharedFiles = Librarian.instance().getNumFiles();
        p.nickname = ConfigurationManager.instance().getNickname();
        p.deviceMajorType = Librarian.instance().getScreenSizeInInches() < 6.9 ? Constants.DEVICE_MAJOR_TYPE_PHONE : Constants.DEVICE_MAJOR_TYPE_TABLET;
        p.clientVersion = Constants.FROSTWIRE_VERSION_STRING;

        return p;
    }

    @Override
    public void refreshPing() {
        UpnpService service = getService();
        LocalDevice device = UPnPServiceConnection.getLocalDevice();

        if (service != null && device != null) {
            invokeSetPingInfo(service, device);
        }
    }

    @Override
    protected void handlePeerDevice(String udn, PingInfo p, InetAddress address, boolean added) {
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP) && added) {
            return;
        }

        PeerManager.instance().onMessageReceived(udn, address, added, p);
    }

    @SuppressWarnings("unchecked")
    private LocalService<UPnPFWDeviceInfo> getInfoService() {
        LocalService<UPnPFWDeviceInfo> service = new AnnotationLocalServiceBinder().read(UPnPFWDeviceInfo.class);

        service.setManager(new DefaultServiceManager<UPnPFWDeviceInfo>(service, UPnPFWDeviceInfo.class));

        return service;
    }
}
