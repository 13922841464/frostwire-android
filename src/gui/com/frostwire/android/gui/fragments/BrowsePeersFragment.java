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

package com.frostwire.android.gui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.adapters.PeerListAdapter;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractListFragment;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.gui.upnp.UPnPManager;
import com.frostwire.gui.upnp.UPnPService;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class BrowsePeersFragment extends AbstractListFragment implements Refreshable, MainFragment {

    private PeerListAdapter adapter;

    private TextView header;

    private ServiceConnection serviceConnection;

    public BrowsePeersFragment() {
        super(R.layout.fragment_browse_peers);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        serviceConnection = UPnPManager.instance().getServiceConnection();
        getActivity().getApplicationContext().bindService(new Intent(getActivity(), UPnPService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        setupAdapter();

        if (getActivity() instanceof AbstractActivity) {
            ((AbstractActivity) getActivity()).addRefreshable(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(serviceConnection);
    }

    @Override
    public void refresh() {
        List<Peer> peers = PeerManager.instance().getPeers();
        adapter.updateList(peers);
    }

    @Override
    public void dismissDialogs() {
        super.dismissDialogs();

        if (adapter != null) {
            adapter.dismissDialogs();
        }
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.wifi_sharing);

        return header;
    }

    private void setupAdapter() {
        adapter = new PeerListAdapter(this.getActivity(), new ArrayList<Peer>());
        setListAdapter(adapter);
        refresh();
    }
}
