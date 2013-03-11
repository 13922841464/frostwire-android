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

package com.frostwire.android.gui.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.MediaType;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.android.util.FilenameUtils;
import com.frostwire.search.FileSearchResult;
import com.frostwire.search.SearchResult;
import com.frostwire.search.TorrentSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchResultListAdapter2 extends AbstractListAdapter<SearchResult> {

    private static final int NO_FILE_TYPE = -1;

    private final DownloadClickListener downloadClickListener;

    private int fileType;

    public SearchResultListAdapter2(Context context, List<SearchResult> list) {
        super(context, R.layout.view_bittorrent_search_result_list_item, list);

        downloadClickListener = new DownloadClickListener();

        this.fileType = NO_FILE_TYPE;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public void filter(int mediaTypeId) {
        setFileType(mediaTypeId);
        visualList = new ArrayList<SearchResult>();
        for (SearchResult sr : new ArrayList<SearchResult>(list)) {
            if (accept(sr, mediaTypeId)) {
                visualList.add(sr);
            }
        }
        notifyDataSetInvalidated();
    }

    public boolean accept(SearchResult sr, int mediaTypeId) {
        if (sr instanceof FileSearchResult) {
            MediaType mt = MediaType.getMediaTypeForExtension(FilenameUtils.getExtension(((FileSearchResult) sr).getFilename()));
            if (mt == null) {
                return false;
            }
            return mt.getId() == mediaTypeId;
        } else {
            return false;
        }
    }

    @Override
    protected void populateView(View view, SearchResult sr) {
        if (sr instanceof FileSearchResult) {
            populateFilePart(view, (FileSearchResult) sr);
        }
        if (sr instanceof TorrentSearchResult) {
            populateTorrentPart(view, (TorrentSearchResult) sr);
        }
    }

    protected void populateFilePart(View view, FileSearchResult sr) {
        ImageView fileTypeIcon = findView(view, R.id.view_bittorrent_search_result_list_item_filetype_icon);
        fileTypeIcon.setImageResource(getFileTypeIconId());

        TextView title = findView(view, R.id.view_bittorrent_search_result_list_item_title);
        title.setText(sr.getDisplayName());
        // if marked as downloading
        // title.setTextColor(GlobalConstants.COLOR_DARK_BLUE);

        TextView fileSize = findView(view, R.id.view_bittorrent_search_result_list_item_file_size);
        if (sr.getSize() > 0) {
            fileSize.setText(UIUtils.getBytesInHuman(sr.getSize()));
        } else {
            fileSize.setText("");
        }

        TextView extra = findView(view, R.id.view_bittorrent_search_result_list_item_text_extra);
        extra.setText(FilenameUtils.getExtension(sr.getFilename()));

        TextView seeds = findView(view, R.id.view_bittorrent_search_result_list_item_text_seeds);
        seeds.setText("");

        TextView sourceLink = findView(view, R.id.view_bittorrent_search_result_list_item_text_source);
        sourceLink.setText(Html.fromHtml("<a href=\"" + sr.getDetailsUrl() + "\">" + sr.getSource() + "</a>"), TextView.BufferType.SPANNABLE);
        sourceLink.setOnClickListener(new OnLinkClickListener(sr.getDetailsUrl()));
    }

    protected void populateTorrentPart(View view, TorrentSearchResult sr) {
        TextView seeds = findView(view, R.id.view_bittorrent_search_result_list_item_text_seeds);
        if (sr.getSeeds() > 0) {
            seeds.setText(getContext().getResources().getQuantityString(R.plurals.count_seeds_source, sr.getSeeds(), sr.getSeeds()));
        } else {
            seeds.setText("");
        }
    }

    @Override
    protected void onItemClicked(View v) {
        downloadClickListener.onClick(v);
    }

    protected void onStartTransfer(SearchResult sr) {
    }

    private int getFileTypeIconId() {
        switch (fileType) {
        case Constants.FILE_TYPE_APPLICATIONS:
            return R.drawable.browse_peer_application_icon_selector_off;
        case Constants.FILE_TYPE_AUDIO:
            return R.drawable.browse_peer_audio_icon_selector_off;
        case Constants.FILE_TYPE_DOCUMENTS:
            return R.drawable.browse_peer_document_icon_selector_off;
        case Constants.FILE_TYPE_PICTURES:
            return R.drawable.browse_peer_picture_icon_selector_off;
        case Constants.FILE_TYPE_VIDEOS:
            return R.drawable.browse_peer_video_icon_selector_off;
        case Constants.FILE_TYPE_TORRENTS:
            return R.drawable.browse_peer_torrent_icon_selector_off;
        default:
            return R.drawable.question_mark;
        }
    }

    private void startTransfer(final SearchResult sr) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                onStartTransfer(sr);
            }
        };

        task.execute();
    }

    private class OnLinkClickListener implements OnClickListener {

        private final String url;

        public OnLinkClickListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            getContext().startActivity(i);
        }
    }

    private final class DownloadClickListener implements OnClickListener {
        public void onClick(View v) {
            SearchResult sr = (SearchResult) v.getTag();
            startTransfer(sr);
        }
    }
}
