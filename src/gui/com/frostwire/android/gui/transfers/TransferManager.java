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

package com.frostwire.android.gui.transfers;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.Peer;
import com.frostwire.logging.Logger;
import com.frostwire.search.HttpSearchResult;
import com.frostwire.search.SearchResult;
import com.frostwire.search.soundcloud.SoundcloudSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.search.youtube.YouTubeCrawledSearchResult;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;
import com.frostwire.vuze.VuzeDownloadFactory;
import com.frostwire.vuze.VuzeDownloadManager;
import com.frostwire.vuze.VuzeKeys;
import com.frostwire.vuze.VuzeManager;
import com.frostwire.vuze.VuzeManager.LoadTorrentsListener;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class TransferManager implements VuzeKeys {

    private static final Logger LOG = Logger.getLogger(TransferManager.class);

    private final List<DownloadTransfer> downloads;
    private final List<UploadTransfer> uploads;
    private final List<BittorrentDownload> bittorrentDownloads;

    private int downloadsToReview;

    private final Object alreadyDownloadingMonitor = new Object();

    private static TransferManager instance;
    
    private OnSharedPreferenceChangeListener preferenceListener;

    public static TransferManager instance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    private TransferManager() {
        registerPreferencesChangeListener();

        this.downloads = new LinkedList<DownloadTransfer>();
        this.uploads = new LinkedList<UploadTransfer>();
        this.bittorrentDownloads = new LinkedList<BittorrentDownload>();

        this.downloadsToReview = 0;

        loadTorrents();
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();

        if (downloads != null) {
            transfers.addAll(downloads);
        }

        if (uploads != null) {
            transfers.addAll(uploads);
        }

        if (bittorrentDownloads != null) {
            transfers.addAll(bittorrentDownloads);
        }

        return transfers;
    }

    private boolean alreadyDownloading(String detailsUrl) {
        synchronized (alreadyDownloadingMonitor) {
            for (DownloadTransfer dt : downloads) {
                if (dt.isDownloading()) {
                    if (dt.getDetailsUrl() != null && dt.getDetailsUrl().equals(detailsUrl)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public DownloadTransfer download(SearchResult sr) {
        if (alreadyDownloading(sr.getDetailsUrl())) {
            return new ExistingDownload();
        }

        if (sr instanceof TorrentSearchResult) {
            return newBittorrentDownload((TorrentSearchResult) sr);
        } else if (sr instanceof HttpSlideSearchResult) {
            return newHttpDownload((HttpSlideSearchResult) sr);
        } else if (sr instanceof YouTubeCrawledSearchResult) {
            return newYouTubeDownload((YouTubeCrawledSearchResult) sr);
        } else if (sr instanceof SoundcloudSearchResult) {
            return newSoundcloudDownload((SoundcloudSearchResult) sr);
        } else if (sr instanceof HttpSearchResult) {
            return newHttpDownload((HttpSearchResult) sr);
        } else {
            return new InvalidDownload();
        }
    }

    public DownloadTransfer download(Peer peer, FileDescriptor fd) {
        PeerHttpDownload download = new PeerHttpDownload(this, peer, fd);

        if (alreadyDownloading(download.getDetailsUrl())) {
            return new ExistingDownload();
        }

        downloads.add(download);
        download.start();

        UXStats.instance().log(UXAction.WIFI_SHARING_DOWNLOAD);

        return download;
    }

    public PeerHttpUpload upload(FileDescriptor fd) {
        PeerHttpUpload upload = new PeerHttpUpload(this, fd);
        uploads.add(upload);
        return upload;
    }

    public DesktopTransfer desktopTransfer(DesktopUploadRequest dur, FileDescriptor fd) {
        DesktopTransfer transfer = null;

        for (DownloadTransfer downloadTransfer : downloads) {
            if (downloadTransfer instanceof DesktopTransfer) {
                DesktopTransfer desktopTransfer = (DesktopTransfer) downloadTransfer;
                if (desktopTransfer.getDUR().equals(dur)) {
                    transfer = desktopTransfer;
                    break;
                }
            }
        }

        if (transfer == null) {
            transfer = new DesktopTransfer(this, dur, fd);
            downloads.add(transfer);
        } else {
            transfer.addFileDescriptor(fd);
        }

        return transfer;
    }

    public void clearComplete() {
        List<Transfer> transfers = getTransfers();

        for (Transfer transfer : transfers) {
            if (transfer != null && transfer.isComplete()) {
                if (transfer instanceof BittorrentDownload) {
                    BittorrentDownload bd = (BittorrentDownload) transfer;
                    if (bd != null && bd.isResumable()) {
                        bd.cancel();
                    }
                } else {
                    transfer.cancel();
                }
            }
        }
    }

    public int getActiveDownloads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        for (DownloadTransfer d : downloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        return count;
    }

    public int getActiveUploads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isSeeding()) {
                count++;
            }
        }

        for (UploadTransfer u : uploads) {
            if (!u.isComplete() && u.isUploading()) {
                count++;
            }
        }

        return count;
    }

    public long getDownloadsBandwidth() {
        long torrenDownloadsBandwidth = VuzeManager.getInstance().getDataReceiveRate();

        long peerDownloadsBandwidth = 0;
        for (DownloadTransfer d : downloads) {
            peerDownloadsBandwidth += d.getDownloadSpeed() / 1000;
        }

        return torrenDownloadsBandwidth + peerDownloadsBandwidth;
    }

    public double getUploadsBandwidth() {
        long torrenUploadsBandwidth = VuzeManager.getInstance().getDataSendRate();

        long peerUploadsBandwidth = 0;
        for (UploadTransfer u : uploads) {
            peerUploadsBandwidth += u.getUploadSpeed() / 1000;
        }

        return torrenUploadsBandwidth + peerUploadsBandwidth;
    }

    public int getDownloadsToReview() {
        return downloadsToReview;
    }

    public void incrementDownloadsToReview() {
        downloadsToReview++;
    }

    public void clearDownloadsToReview() {
        downloadsToReview = 0;
    }

    public void stopSeedingTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            if (d.isSeeding() || d.isComplete()) {
                d.pause();
            }
        }
    }

    public void loadTorrents() {
        bittorrentDownloads.clear();

        boolean stop = false;
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS)) {
            stop = true;
        } else {
            if (!NetworkManager.instance().isDataWIFIUp() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY)) {
                stop = true;
            }
        }

        VuzeManager.getInstance().loadTorrents(stop, new LoadTorrentsListener() {

            @Override
            public void onLoad(List<VuzeDownloadManager> dms) {
                //bittorrentDownloads.addAll(dms).add(BittorrentDownloadCreator.create(TransferManager.this, dm));
            }
        });
    }

    List<BittorrentDownload> getBittorrentDownloads() {
        return new LinkedList<BittorrentDownload>(bittorrentDownloads);
    }

    boolean remove(Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            return bittorrentDownloads.remove(transfer);
        } else if (transfer instanceof DownloadTransfer) {
            return downloads.remove(transfer);
        } else if (transfer instanceof UploadTransfer) {
            return uploads.remove(transfer);
        }

        return false;
    }

    public void pauseTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            d.pause();
        }
    }

    public BittorrentDownload downloadTorrent(String uri) {
        try {
            BittorrentDownload download = new AzureusBittorrentDownload(this, VuzeDownloadFactory.create(new URI(uri)));

            if (!(download instanceof InvalidBittorrentDownload)) {
                if (!bittorrentDownloads.contains(download)) {
                    bittorrentDownloads.add(download);
                }
            }

            return download;
        } catch (Throwable e) {
            LOG.warn("Error creating download from uri: " + uri);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private BittorrentDownload newBittorrentDownload(TorrentSearchResult sr) {
        try {
            BittorrentDownload download = new AzureusBittorrentDownload(this, VuzeDownloadFactory.create(sr));

            if (!(download instanceof InvalidBittorrentDownload)) {
                if (!bittorrentDownloads.contains(download)) {
                    bittorrentDownloads.add(download);
                }
            }

            return download;
        } catch (Throwable e) {
            LOG.warn("Error creating download from search result: " + sr);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private HttpDownload newHttpDownload(HttpSlideSearchResult sr) {
        HttpDownload download = new HttpDownload(this, sr.getDownloadLink());

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newYouTubeDownload(YouTubeCrawledSearchResult sr) {
        YouTubeDownload download = new YouTubeDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newSoundcloudDownload(SoundcloudSearchResult sr) {
        SoundcloudDownload download = new SoundcloudDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newHttpDownload(HttpSearchResult sr) {
        HttpDownload download = new HttpDownload(this, new HttpSearchResultDownloadLink(sr));

        downloads.add(download);
        download.start();

        return download;
    }

    /** Stops all HttpDownloads (Cloud and Wi-Fi) */
    public void stopHttpTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.addAll(downloads);
        transfers.addAll(uploads);

        for (Transfer t : transfers) {
            if (t instanceof DownloadTransfer) {
                DownloadTransfer d = (DownloadTransfer) t;
                if (!d.isComplete() && d.isDownloading()) {
                    d.cancel();
                }
            } else if (t instanceof UploadTransfer) {
                UploadTransfer u = (UploadTransfer) t;

                if (!u.isComplete() && u.isUploading()) {
                    u.cancel();
                }
            }
        }
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED)) {
                    setAzureusParameter(MAX_DOWNLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOAD_SPEED)) {
                    setAzureusParameter(MAX_UPLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOADS)) {
                    setAzureusParameter(MAX_DOWNLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOADS)) {
                    setAzureusParameter(MAX_UPLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS)) {
                    setAzureusParameter(MAX_TOTAL_CONNECTIONS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TORRENT_CONNECTIONS)) {
                    setAzureusParameter(MAX_TORRENT_CONNECTIONS);
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }

    private void setAzureusParameter(String key) {
        VuzeManager.getInstance().setParameter(key, ConfigurationManager.instance().getLong(key));
    }
}
