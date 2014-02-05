/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.vuze;

import java.io.File;
import java.util.Date;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.minicastle.util.Arrays;

import com.frostwire.vuze.VuzeUtils.InfoSetQuery;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class VuzeDownloadManager {

    private final DownloadManager dm;

    private final String displayName;
    private final long size;
    private final byte[] hash;
    private final File savePath;
    private final Date creationDate;

    public VuzeDownloadManager(DownloadManager dm) {
        this.dm = dm;

        Set<DiskManagerFileInfo> noSkippedSet = VuzeUtils.getFileInfoSet(dm, InfoSetQuery.NO_SKIPPED);

        this.displayName = calculateDisplayName(dm, noSkippedSet);
        this.size = calculateSize(dm, noSkippedSet);

        this.hash = calculateHash(dm);
        this.savePath = dm.getSaveLocation();
        this.creationDate = new Date(dm.getCreationTime());
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSize() {
        return size;
    }

    /**
     * The client should be aware that if the array is modified, the inner state of
     * the object is changed (due to array mutability in java).
     * 
     * @return
     */
    public byte[] getHash() {
        return hash;
    }

    public File getSavePath() {
        return savePath;
    }

    /**
     * This method should be used with care, since Date is mutable, the user
     * of this class could mess with the inner state of the object.
     * 
     * @return
     */
    public Date getCreationDate() {
        return creationDate;
    }

    public String getStatus() {
        return DisplayFormatters.formatDownloadStatus(dm);
    }

    public int getDownloadCompleted() {
        return dm.getStats().getDownloadCompleted(true) / 10;
    }

    public boolean isResumable() {
        return ManagerUtils.isStartable(dm);
    }

    public boolean isPausable() {
        return ManagerUtils.isStopable(dm);
    }

    public boolean isComplete() {
        return dm.getAssumedComplete();
    }

    public boolean isDownloading() {
        return dm.getState() == DownloadManager.STATE_DOWNLOADING;
    }

    public boolean isSeeding() {
        return dm.getState() == DownloadManager.STATE_SEEDING;
    }

    public long getBytesReceived() {
        return dm.getStats().getTotalGoodDataBytesReceived();
    }

    public long getBytesSent() {
        return dm.getStats().getTotalDataBytesSent();
    }

    public long getDownloadSpeed() {
        return dm.getStats().getDataReceiveRate();
    }

    public long getUploadSpeed() {
        return dm.getStats().getDataSendRate();
    }

    public long getETA() {
        return dm.getStats().getETA();
    }

    public int getShareRatio() {
        return dm.getStats().getShareRatio();
    }

    public int getPeers() {
        int peers;

        TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();

        if (response != null && response.isValid()) {
            int trackerPeerCount = response.getPeers();
            peers = dm.getNbPeers();
            if (peers == 0 || trackerPeerCount > peers) {
                if (trackerPeerCount <= 0) {
                    peers = dm.getActivationCount();
                } else {
                    peers = trackerPeerCount;
                }
            }
        } else {
            peers = dm.getNbPeers();
        }

        return peers;
    }

    public int getSeeds() {
        int seeds;

        TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();

        if (response != null && response.isValid()) {
            seeds = Math.max(dm.getNbSeeds(), response.getSeeds());
        } else {
            seeds = dm.getNbSeeds();
        }

        return seeds;
    }

    public int getConnectedPeers() {
        return dm.getNbPeers();
    }

    public int getConnectedSeeds() {
        return dm.getNbSeeds();
    }

    public boolean hasStarted() {
        int state = dm.getState();
        return state == DownloadManager.STATE_SEEDING || state == DownloadManager.STATE_DOWNLOADING;
    }

    public boolean hasScrape() {
        TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
        return response != null && response.isValid();
    }

    public void start() {
        ManagerUtils.start(dm);
    }

    public void stop() {
        ManagerUtils.stop(dm);
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = false;

        if (o instanceof VuzeDownloadManager) {
            VuzeDownloadManager other = (VuzeDownloadManager) o;
            if (dm.equals(other.dm) || Arrays.areEqual(getHash(), other.getHash())) {
                equals = true;
            }
        }

        return equals;
    }

    DownloadManager getDM() {
        return dm;
    }

    private String calculateDisplayName(DownloadManager dm, Set<DiskManagerFileInfo> noSkippedSet) {
        String displayName = null;

        if (noSkippedSet.size() == 1) {
            displayName = FilenameUtils.getBaseName(noSkippedSet.iterator().next().getFile(false).getName());
        } else {
            displayName = dm.getDisplayName();
        }

        return displayName;
    }

    private long calculateSize(DownloadManager dm, Set<DiskManagerFileInfo> noSkippedSet) {
        long size = 0;

        boolean partial = noSkippedSet.size() == dm.getDiskManagerFileInfoSet().nbFiles();

        if (partial) {
            for (DiskManagerFileInfo fileInfo : noSkippedSet) {
                size += fileInfo.getLength();
            }
        } else {
            size = dm.getSize();
        }

        return size;
    }

    private byte[] calculateHash(DownloadManager dm) {
        try {
            return dm.getTorrent().getHash();
        } catch (TOTorrentException e) {
            throw new RuntimeException(e);
        }
    }
}
