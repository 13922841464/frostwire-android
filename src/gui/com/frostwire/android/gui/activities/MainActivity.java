/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.activities;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.appia.sdk.Appia;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.SoftwareUpdater;
import com.frostwire.android.gui.SoftwareUpdater.ConfigurationUpdateListener;
import com.frostwire.android.gui.activities.internal.MainController;
import com.frostwire.android.gui.activities.internal.XmlMenuAdapter;
import com.frostwire.android.gui.activities.internal.XmlMenuItem;
import com.frostwire.android.gui.activities.internal.XmlMenuLoader;
import com.frostwire.android.gui.fragments.AboutFragment;
import com.frostwire.android.gui.fragments.BrowsePeerFragment;
import com.frostwire.android.gui.fragments.BrowsePeersDisabledFragment;
import com.frostwire.android.gui.fragments.BrowsePeersFragment;
import com.frostwire.android.gui.fragments.MainFragment;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.services.DesktopUploadManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.OfferUtils;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.DesktopUploadRequestDialog;
import com.frostwire.android.gui.views.DesktopUploadRequestDialogResult;
import com.frostwire.android.gui.views.PlayerMenuItemView;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.TOS;
import com.frostwire.android.gui.views.TOS.OnTOSAcceptListener;
import com.frostwire.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class MainActivity extends AbstractActivity implements ConfigurationUpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private static final String FRAGMENT_STACK_TAG = "fragment_stack";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    private static final String DUR_TOKEN_KEY = "dur_token";
    private static final String APPIA_STARTED_KEY = "appia_started";

    private static boolean firstTime = true;

    private MainController controller;

    private DrawerLayout drawerLayout;
    private View leftDrawer;
    private ListView listMenu;
    private ImageButton buttonMainMenu;

    private SearchFragment search;
    private BrowsePeerFragment library;
    private TransfersFragment transfers;
    private BrowsePeersFragment peers;
    private BrowsePeersDisabledFragment peersDisabled;
    private AboutFragment about;

    private PlayerMenuItemView playerItem;

    // not sure about this variable, quick solution for now
    private String durToken;

    private boolean offercastStarted = false;
    private boolean appiaStarted = false;

    public MainActivity() {
        super(R.layout.activity_main, false, 2);
        this.controller = new MainController(this);
    }

    public void showMyFiles() {
        controller.showMyFiles();
    }

    public void switchFragment(int itemId) {
        controller.switchFragment(itemId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!(getCurrentFragment() instanceof SearchFragment)) {
                controller.switchFragment(R.id.menu_main_search);
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleDrawer();
        } else {
            return super.onKeyDown(keyCode, event);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            handleLastBackPressed();
        }

        syncSlideMenu();
        updateHeader(getCurrentFragment());
    }

    @Override
    public void onConfigurationUpdate() {
        setupMenuItems();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drawerLayout = findView(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                refreshPlayerItem();
                syncSlideMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (slideOffset > 0) {
                    buttonMainMenu.setImageResource(R.drawable.main_menu_button_icon_selected);
                } else {
                    buttonMainMenu.setImageResource(R.drawable.main_menu_button_icon);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                buttonMainMenu.setImageResource(R.drawable.main_menu_button_icon_selected);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                buttonMainMenu.setImageResource(R.drawable.main_menu_button_icon);
            }
        });

        leftDrawer = findView(R.id.activity_main_left_drawer);
        listMenu = findView(R.id.left_drawer);

        playerItem = findView(R.id.slidemenu_player_menuitem);
        playerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.launchPlayerActivity();
            }
        });

        buttonMainMenu = findView(R.id.activity_main_button_menu);
        buttonMainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        setupFragments();

        setupInitialFragment(savedInstanceState);

        setupMenuItems();

        if (savedInstanceState != null) {
            durToken = savedInstanceState.getString(DUR_TOKEN_KEY);
            appiaStarted = savedInstanceState.getBoolean(APPIA_STARTED_KEY);
        }

        addRefreshable((Refreshable) findView(R.id.activity_main_player_notifier));

        onNewIntent(getIntent());

        SoftwareUpdater.instance().addConfigurationUpdateListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        onResumeFragments();

        if (action != null && action.equals(Constants.ACTION_SHOW_TRANSFERS)) {
            controller.showTransfers();
        } else if (action != null && action.equals(Constants.ACTION_OPEN_TORRENT_URL)) {
            //Open a Torrent from a URL or from a local file :), say from Astro File Manager.
            /**
             * TODO: Ask @aldenml the best way to plug in NewTransferDialog.
             * I've refactored this dialog so that it is forced (no matter if the setting
             * to not show it again has been used) and when that happens the checkbox is hidden.
             * 
             * However that dialog requires some data about the download, data which is not
             * obtained until we have instantiated the Torrent object.
             * 
             * I'm thinking that we can either:
             * a) Pass a parameter to the transfer manager, but this would probably
             * not be cool since the transfer manager (I think) should work independently from
             * the UI thread.
             * 
             * b) Pass a "listener" to the transfer manager, once the transfer manager has the torrent
             * it can notify us and wait for the user to decide wether or not to continue with the transfer
             * 
             * c) Forget about showing that dialog, and just start the download, the user can cancel it.
             */

            //Show me the transfer tab
            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Constants.ACTION_SHOW_TRANSFERS);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

            //go!
            TransferManager.instance().downloadTorrent(intent.getDataString());
        } else if ((action != null && action.equals(Constants.ACTION_DESKTOP_UPLOAD_REQUEST)) || durToken != null) {
            handleDesktopUploadRequest(intent);
        }
        // When another application wants to "Share" a file and has chosen FrostWire to do so.
        // We make the file "Shared" so it's visible for other FrostWire devices on the local network.
        else if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {
            controller.handleSendAction(intent);
        }

        if (intent.hasExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION)) {
            controller.showTransfers();
            TransferManager.instance().clearDownloadsToReview();
            try {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED);
                Bundle extras = intent.getExtras();
                if (extras.containsKey(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH)) {
                    File file = new File(extras.getString(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH));
                    if (file.isFile()) {
                        UIUtils.openFile(this, file.getAbsoluteFile());
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Error handling download complete notification", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshPeersFragment();

        initializeAppia();
        initializeOffercast();
        

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE)) {
                mainResume();
            } else {
                controller.startWizardActivity();
            }
        } else {
            trackDialog(TOS.showEula(this, new OnTOSAcceptListener() {
                public void onAccept() {
                    controller.startWizardActivity();
                }
            }));
        }

        checkLastSeenVersion();
    }

    private void initializeAppia() {
        if (!appiaStarted && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA)) {
            try {
                Appia appia = Appia.getAppia();
                appia.setSiteId(3867);
                appiaStarted = true;
            } catch (Throwable t) {
                appiaStarted = false;
            }
        }
    }

    private void initializeOffercast() {
        if (!offercastStarted && 
            ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_OFFERCAST)){
            try {
                OfferUtils.startOffercast(getApplicationContext());
                offercastStarted = true;
            } catch (Exception e) {
                offercastStarted = false;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveLastFragment(outState);

        outState.putString(DUR_TOKEN_KEY, durToken);
        outState.putBoolean(APPIA_STARTED_KEY, appiaStarted);
    }

    @Override
    protected void onPause() {
        super.onPause();

        search.dismissDialogs();
        library.dismissDialogs();
        peers.dismissDialogs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //avoid memory leaks when the device is tilted and the menu gets recreated.
        SoftwareUpdater.instance().removeConfigurationUpdateListener(this);

        if (playerItem != null) {
            playerItem.unbindDrawables();
        }
    }

    private void saveLastFragment(Bundle outState) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT_KEY, fragment);
        }
    }

    private void mainResume() {
        syncSlideMenu();

        if (firstTime) {
            firstTime = false;
            Engine.instance().startServices(); // it's necessary for the first time after wizard
        }

        SoftwareUpdater.instance().checkForUpdate(this);
    }

    private void checkLastSeenVersion() {
        final String lastSeenVersion = ConfigurationManager.instance().getString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION);
        if (StringUtils.isNullOrEmpty(lastSeenVersion)) {
            //fresh install
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_FIRST_TIME);
        } else if (!Constants.FROSTWIRE_VERSION_STRING.equals(lastSeenVersion)) {
            //just updated.
            ConfigurationManager.instance().setString(Constants.PREF_KEY_CORE_LAST_SEEN_VERSION, Constants.FROSTWIRE_VERSION_STRING);
            UXStats.instance().log(UXAction.CONFIGURATION_WIZARD_AFTER_UPDATE);
        }
    }

    private void handleDesktopUploadRequest(Intent intent) {
        String action = intent.getAction();

        if (durToken == null && action.equals(Constants.ACTION_DESKTOP_UPLOAD_REQUEST)) {
            durToken = intent.getStringExtra(Constants.EXTRA_DESKTOP_UPLOAD_REQUEST_TOKEN);
        }

        final DesktopUploadManager dum = Engine.instance().getDesktopUploadManager();

        if (dum == null) {
            return;
        }

        DesktopUploadRequest dur = dum.getRequest(durToken);

        if (durToken != null && dur != null && dur.status == DesktopUploadRequestStatus.PENDING) {
            DesktopUploadRequestDialog dlg = new DesktopUploadRequestDialog(this, dur, new DesktopUploadRequestDialog.OnDesktopUploadListener() {
                @Override
                public void onResult(DesktopUploadRequestDialog dialog, DesktopUploadRequestDialogResult result) {
                    switch (result) {
                    case ACCEPT:
                        dum.authorizeRequest(durToken);
                        if (ConfigurationManager.instance().showTransfersOnDownloadStart()) {
                            Intent i = new Intent(Constants.ACTION_SHOW_TRANSFERS);
                            MainActivity.this.startActivity(i.setClass(MainActivity.this, MainActivity.class));
                        }
                        break;
                    case REJECT:
                        dum.rejectRequest(durToken);
                        break;
                    case BLOCK:
                        dum.blockComputer(durToken);
                        break;
                    }
                    durToken = null;
                }
            });

            trackDialog(dlg).show();
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(leftDrawer)) {
            drawerLayout.closeDrawer(leftDrawer);
        } else {
            drawerLayout.openDrawer(leftDrawer);
        }

        updateHeader(getCurrentFragment());
    }

    private Fragment getWifiSharingFragment() {
        return Engine.instance().isStarted() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP) ? peers : peersDisabled;
    }

    private void handleLastBackPressed() {
        trackDialog(UIUtils.showYesNoDialog(this, R.string.are_you_sure_you_wanna_leave, R.string.minimize_frostwire, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }));
    }

    private void syncSlideMenu() {
        Fragment fragment = getCurrentFragment();

        if (fragment instanceof SearchFragment) {
            setSelectedItem(R.id.menu_main_search);
        } else if (fragment instanceof BrowsePeerFragment) {
            setSelectedItem(R.id.menu_main_library);
        } else if (fragment instanceof TransfersFragment) {
            setSelectedItem(R.id.menu_main_transfers);
        } else if (fragment instanceof BrowsePeersFragment || fragment instanceof BrowsePeersDisabledFragment) {
            setSelectedItem(R.id.menu_main_peers);
        } else if (fragment instanceof AboutFragment) {
            setSelectedItem(R.id.menu_main_about);
        }

        updateHeader(getCurrentFragment());
    }

    private void setSelectedItem(int id) {
        try {
            XmlMenuAdapter adapter = (XmlMenuAdapter) listMenu.getAdapter();
            adapter.setSelectedItem(id);
        } catch (Throwable e) { // protecting from weird android UI engine issues
            LOG.warn("Error setting slide menu item selected", e);
        }
    }

    private void refreshPlayerItem() {
        if (playerItem != null) {
            playerItem.refresh();
        }
    }

    private void setupMenuItems() {
        XmlMenuItem[] items = new XmlMenuLoader().load(this);
        XmlMenuAdapter adapter = new XmlMenuAdapter(controller, items);
        listMenu.setAdapter(adapter);
    }

    private void setupFragments() {
        search = new SearchFragment();
        library = new BrowsePeerFragment();
        transfers = new TransfersFragment();
        peers = new BrowsePeersFragment();
        peersDisabled = new BrowsePeersDisabledFragment();
        about = new AboutFragment();

        library.setPeer(PeerManager.instance().getLocalPeer());
    }

    private void setupInitialFragment(Bundle savedInstanceState) {
        Fragment fragment = null;

        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT_KEY);
        }
        if (fragment == null) {
            fragment = search;
            setSelectedItem(R.id.menu_main_search);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, FRAGMENT_STACK_TAG).commit();

        updateHeader(fragment);
    }

    private void updateHeader(Fragment fragment) {
        try {
            RelativeLayout placeholder = findView(R.id.activity_main_layout_header_placeholder);
            if (placeholder.getChildCount() > 0) {
                placeholder.removeAllViews();
            }

            if (fragment instanceof MainFragment) {
                View header = ((MainFragment) fragment).getHeader(this);
                if (header != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    placeholder.addView(header, params);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error updating main header", e);
        }
    }

    private void refreshPeersFragment() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof BrowsePeersFragment || fragment instanceof BrowsePeersDisabledFragment) {
            controller.switchFragment(R.id.menu_main_peers);
        }
    }

    /*
     * The following methods are only public to be able to use them from another package(internal).
     */

    public Fragment getFragmentByMenuId(int id) {
        switch (id) {
        case R.id.menu_main_search:
            return search;
        case R.id.menu_main_library:
            return library;
        case R.id.menu_main_transfers:
            return transfers;
        case R.id.menu_main_peers:
            return getWifiSharingFragment();
        case R.id.menu_main_about:
            return about;
        default:
            return null;
        }
    }

    public void switchContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment, FRAGMENT_STACK_TAG).addToBackStack(null).commit();
        updateHeader(fragment);
    }

    public Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(FRAGMENT_STACK_TAG);
    }

    public void closeSlideMenu() {
        drawerLayout.closeDrawer(leftDrawer);
    }
}
