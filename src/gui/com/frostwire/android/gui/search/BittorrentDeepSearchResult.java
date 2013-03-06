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

package com.frostwire.android.gui.search;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;

import com.frostwire.android.util.FilenameUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class BittorrentDeepSearchResult implements BittorrentSearchResult {

    private BittorrentWebSearchResult bittorrentResult;
    private TOTorrentFile torrentFile;

    public BittorrentDeepSearchResult(BittorrentWebSearchResult bittorrentResult, TOTorrentFile torrentFile) {
        this.bittorrentResult = bittorrentResult;
        this.torrentFile = torrentFile;
    }

    public String getDisplayName() {
        return FilenameUtils.getName(torrentFile.getRelativePath());
    }

    public String getFileName() {
        return torrentFile.getRelativePath();
    }

    public int getRank() {
        return bittorrentResult.getRank();
    }

    public long getSize() {
        return torrentFile.getLength();
    }

    public long getCreationTime() {
        return bittorrentResult.getCreationTime();
    }

    @Override
    public String getHash() {
        return bittorrentResult.getHash();
    }
    
    @Override
    public String getDetailsUrl() {
        return bittorrentResult.getDetailsUrl();
    }

    @Override
    public String getTorrentURI() {
        return bittorrentResult.getTorrentURI();
    }

    @Override
    public String getSource() {
        return bittorrentResult.getSource();
    }
}
