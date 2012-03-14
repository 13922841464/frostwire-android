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

import android.content.Context;
import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
class DeepSearchTask extends TorrentSearchTask {

    private static final String TAG = "FW.DownloadTorrentsTask";

    private final Context context;
    private final SearchResultDisplayer displayer;
    private final String query;
    private LocalSearchEngine localSearchEngine;

    public DeepSearchTask(Context context, SearchResultDisplayer displayer, String query) {
        super("DeepSearchTask: " + query);
        this.context = context;
        this.displayer = displayer;
        this.query = query;
    }

    @Override
    public void run() {
        try {
            localSearchEngine = new LocalSearchEngine(context, this, displayer, query);
            localSearchEngine.deepSearch();
        } catch (Throwable e) {
            Log.e(TAG, "Error performing the deep search", e);
        }
    }
    
    @Override
    public void cancel() {
        super.cancel();
        
        if (localSearchEngine != null) {
               localSearchEngine.cancel();
        }
        
    }
}
