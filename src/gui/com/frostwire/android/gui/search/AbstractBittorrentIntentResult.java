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

import com.frostwire.search.TorrentSearchResult;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class AbstractBittorrentIntentResult implements TorrentSearchResult {

    @Override
    public String getFilename() {
        return null;
    }
    
    @Override
    public String getTorrentURI() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }
    
    @Override
    public int getSeeds() {
        return 0;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public String getCacheKey() {
        return null;
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }
}