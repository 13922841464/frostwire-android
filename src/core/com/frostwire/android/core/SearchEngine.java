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

package com.frostwire.android.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;

import com.frostwire.android.bittorrent.websearch.clearbits.ClearBitsWebSearchPerformer;
import com.frostwire.android.bittorrent.websearch.extratorrent.ExtratorrentWebSearchPerformer;
import com.frostwire.android.bittorrent.websearch.isohunt.ISOHuntWebSearchPerformer;
import com.frostwire.android.bittorrent.websearch.mininova.MininovaWebSearchPerformer;
import com.frostwire.android.bittorrent.websearch.vertor.VertorWebSearchPerformer;
import com.frostwire.websearch.WebSearchPerformer;
import com.frostwire.websearch.youtube.YouTubeSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class SearchEngine {

    public static final int CLEARBITS_ID = 0;
    public static final int MININOVA_ID = 1;
    public static final int ISOHUNT_ID = 2;
    //public static final int BTJUNKIE_ID = 3;
    public static final int EXTRATORRENT_ID = 4;
    public static final int VERTOR_ID = 5;
    //public static final int TPB_ID = 6;
    public static final int MONOVA_ID = 7;
    //public static final int KAT_ID = 8;
    public static final int YOUTUBE_ID = 9;

    private final int id;
    private final String name;
    private final WebSearchPerformer performer;
    private final String preferenceKey;

    private boolean active;

    //public static final SearchEngine BTJUNKIE = new SearchEngine(BTJUNKIE_ID, "BTJunkie", new BTJunkieWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_BTJUNKIE);
    public static final SearchEngine CLEARBITS = new SearchEngine(CLEARBITS_ID, "ClearBits", new ClearBitsWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_CLEARBITS);
    public static final SearchEngine EXTRATORRENT = new SearchEngine(EXTRATORRENT_ID, "Extratorrent", new ExtratorrentWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_EXTRATORRENT);
    public static final SearchEngine ISOHUNT = new SearchEngine(ISOHUNT_ID, "ISOHunt", new ISOHuntWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_ISOHUNT);
    public static final SearchEngine MININOVA = new SearchEngine(MININOVA_ID, "Mininova", new MininovaWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_MININOVA);
    //public static final SearchEngine TPB = new SearchEngine(TPB_ID, "TPB", new TPBWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_TPB);
    public static final SearchEngine VERTOR = new SearchEngine(VERTOR_ID, "Vertor", new VertorWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_VERTOR);
    //public static final SearchEngine KAT = new SearchEngine(KAT_ID, "KAT", new KATWebSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_KAT);
    public static final SearchEngine YOUTUBE = new SearchEngine(YOUTUBE_ID, "YouTube", new YouTubeSearchPerformer(), Constants.PREF_KEY_SEARCH_USE_YOUTUBE);

    private SearchEngine(int id, String name, WebSearchPerformer performer, String preferenceKey) {
        this.id = id;
        this.name = name;
        this.performer = performer;
        this.preferenceKey = preferenceKey;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WebSearchPerformer getPerformer() {
        return performer;
    }

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public boolean isEnabled() {
        return isActive() && ConfigurationManager.instance().getBoolean(preferenceKey);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object obj) {
        return id == ((SearchEngine) obj).id;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<SearchEngine> getSearchEngines() {
        return Arrays.asList(CLEARBITS, MININOVA, ISOHUNT, EXTRATORRENT, VERTOR/*, TPB*//*,KAT*/, YOUTUBE);
    }

    public static SearchEngine getSearchEngine(int id) {
        for (SearchEngine engine : getSearchEngines()) {
            if (engine.getId() == id) {
                return engine;
            }
        }

        return null;
    }

    public static SearchEngine getSearchEngine(String name) {
        for (SearchEngine engine : getSearchEngines()) {
            if (engine.getName().equals(name)) {
                return engine;
            }
        }

        return null;
    }

    @SuppressLint("UseSparseArrays")
    public static Map<Integer, SearchEngine> getSearchEngineMap() {
        HashMap<Integer, SearchEngine> map = new HashMap<Integer, SearchEngine>();

        for (SearchEngine engine : getSearchEngines()) {
            map.put(engine.getId(), engine);
        }

        return map;
    }
}
