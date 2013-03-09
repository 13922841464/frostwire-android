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

package com.frostwire.search;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;

import com.frostwire.android.util.FilenameUtils;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class TorrentDeepSearchResult implements TorrentSearchResult {

    private TorrentWebSearchResult sr;
    private TOTorrentFile file;

    public TorrentDeepSearchResult(TorrentWebSearchResult sr, TOTorrentFile file) {
        this.sr = sr;
        this.file = file;
    }

    @Override
    public String getDisplayName() {
        return FilenameUtils.getName(file.getRelativePath());
    }

    @Override
    public String getFilename() {
        return file.getRelativePath();
    }

    @Override
    public long getSize() {
        return file.getLength();
    }

    public long getCreationTime() {
        return sr.getCreationTime();
    }

    public String getTorrentURI() {
        return sr.getTorrentURI();
    }

    public String getSource() {
        return sr.getSource();
    }

    @Override
    public String getDetailsUrl() {
        return sr.getDetailsUrl();
    }

    @Override
    public int getSeeds() {
        return sr.getSeeds();
    }
    
    @Override
    public String getHash() {
        return sr.getHash();
    }

    @Override
    public String toString() {
        return sr.toString();
    }
}
