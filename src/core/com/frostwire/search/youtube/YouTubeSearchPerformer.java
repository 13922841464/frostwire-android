/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
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

package com.frostwire.search.youtube;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.frostwire.search.PagedWebSearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.youtube.YouTubeSearchResult.ResultType;
import com.frostwire.util.JsonUtils;
import com.frostwire.websearch.WebSearchResult;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class YouTubeSearchPerformer extends PagedWebSearchPerformer {

    private static final int MAX_RESULTS = 10;

    public YouTubeSearchPerformer(String keywords, int timeout) {
        super(keywords, timeout, 1);
    }

    @Override
    protected List<? extends SearchResult<?>> searchPage(int page) {
        List<SearchResult<WebSearchResult>> result = new LinkedList<SearchResult<WebSearchResult>>();

        YouTubeResponse response = searchYouTube();

        if (response != null && response.feed != null && response.feed.entry != null)
            for (YouTubeEntry entry : response.feed.entry) {

                if (!isStopped()) {
                    WebSearchResult vsr = new YouTubeSearchResult(entry, ResultType.VIDEO);
                    result.add(new SearchResult<WebSearchResult>(vsr));
                    WebSearchResult asr = new YouTubeSearchResult(entry, ResultType.AUDIO);
                    result.add(new SearchResult<WebSearchResult>(asr));
                }
            }

        return result;
    }

    private YouTubeResponse searchYouTube() {

        String url = encodeUrl(String.format(Locale.US, "https://gdata.youtube.com/feeds/api/videos?q=%s&orderby=relevance&start-index=1&max-results=%d&alt=json&prettyprint=true&v=2", keywords, MAX_RESULTS));
        String json = get(url);

        json = fixJson(json);

        YouTubeResponse response = JsonUtils.toObject(json, YouTubeResponse.class);

        return response;
    }

    private String fixJson(String json) {
        return json.replace("\"$t\"", "\"title\"").replace("\"yt$userId\"", "\"ytuserId\"");
    }
}
