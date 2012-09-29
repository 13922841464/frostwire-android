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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.providers.UniversalStore.Applications;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.adapters.menu.DeleteFileMenuAction;
import com.frostwire.android.gui.adapters.menu.DownloadCheckedMenuAction;
import com.frostwire.android.gui.adapters.menu.DownloadMenuAction;
import com.frostwire.android.gui.adapters.menu.OpenMenuAction;
import com.frostwire.android.gui.adapters.menu.RenameFileMenuAction;
import com.frostwire.android.gui.adapters.menu.SendFileMenuAction;
import com.frostwire.android.gui.adapters.menu.SetAsRingtoneMenuAction;
import com.frostwire.android.gui.adapters.menu.SetAsWallpaperMenuAction;
import com.frostwire.android.gui.adapters.menu.SetSharedStateFileGrainedMenuAction;
import com.frostwire.android.gui.adapters.menu.ToggleFileGrainedSharingMenuAction;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.android.gui.views.ListAdapterFilter;
import com.frostwire.android.gui.views.MenuAction;
import com.frostwire.android.gui.views.MenuAdapter;
import com.frostwire.android.gui.views.MenuBuilder;
import com.frostwire.android.gui.views.ThumbnailLoader;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class FileListAdapter extends AbstractListAdapter<FileDescriptor> {

    @SuppressWarnings("unused")
    private static final String TAG = "FW.FileListAdapter";

    private final Peer peer;
    private final boolean local;
    private final byte fileType;
    private final ThumbnailLoader thumbnailLoader;
    private final Drawable fileTypeDrawable;

    private final PadLockClickListener padLockClickListener;
    private final DownloadButtonClickListener downloadButtonClickListener;

    public FileListAdapter(Context context, List<FileDescriptor> files, Peer peer, boolean local, byte fileType, ThumbnailLoader thumbnailLoader) {
        super(context, getViewItemId(local, fileType), files);

        setShowMenuOnClick(true);
        setAdapterFilter(new FileListFilter());

        this.peer = peer;
        this.local = local;
        this.fileType = fileType;
        this.thumbnailLoader = thumbnailLoader;
        this.fileTypeDrawable = getContext().getResources().getDrawable(getFileTypeIconId(fileType));

        this.padLockClickListener = new PadLockClickListener();
        this.downloadButtonClickListener = new DownloadButtonClickListener();
    }

    public byte getFileType() {
        return fileType;
    }

    @Override
    protected final void populateView(View view, FileDescriptor file) {
        if (getViewItemId() == R.layout.view_browse_thumbnail_peer_list_item) {
            populateViewThumbnail(view, file);
        } else {
            populateViewPlain(view, file);
        }
    }

    @Override
    protected MenuAdapter getMenuAdapter(View view) {
        Context context = getContext();

        List<MenuAction> items = new ArrayList<MenuAction>();

        FileDescriptor fd = (FileDescriptor) view.getTag();

        List<FileDescriptor> checked = new ArrayList<FileDescriptor>(getChecked());
        int numChecked = checked.size();

        boolean showSingleOptions = showSingleOptions(checked, fd);

        if (local) {
            if (showSingleOptions) {
                items.add(new OpenMenuAction(context, fd.filePath, fd.mime));

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new SendFileMenuAction(context, fd)); //applications cause a force close with GMail
                }

                if (fd.fileType == Constants.FILE_TYPE_RINGTONES && numChecked <= 1) {
                    items.add(new SetAsRingtoneMenuAction(context, fd));
                }

                if (fd.fileType == Constants.FILE_TYPE_PICTURES && numChecked <= 1) {
                    items.add(new SetAsWallpaperMenuAction(context, fd));
                }

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new RenameFileMenuAction(context, this, fd));
                }
            }

            List<FileDescriptor> list = checked;
            if (list.size() == 0) {
                list = Arrays.asList(fd);
            }

            //Share Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, true));

            //Unshare Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, false));

            //Toogle Shared States
            items.add(new ToggleFileGrainedSharingMenuAction(context, this, list));

            if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS) {
                items.add(new DeleteFileMenuAction(context, this, list));
            }
        } else {
            if (0 < numChecked && numChecked <= Constants.MAX_NUM_DOWNLOAD_CHECKED) {
                items.add(new DownloadCheckedMenuAction(context, this, checked, peer));
            }

            items.add(new DownloadMenuAction(context, this, peer, fd));
        }

        return new MenuAdapter(context, fd.title, items);
    }

    private void localPlay(FileDescriptor fd) {
        if (fd == null) {
            return;
        }

        if (fd.mime != null && fd.mime.contains("audio")) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                Engine.instance().getMediaPlayer().stop();
            } else {
                UIUtils.playEphemeralPlaylist(fd);
            }
            notifyDataSetChanged();
        } else {
            if (fd.filePath != null && fd.mime != null) {
                UIUtils.openFile(getContext(), fd.filePath, fd.mime);
            }
        }
    }

    /**
     * Start a transfer
     */
    private void startDownload(FileDescriptor fd) {
        TransferManager.instance().download(peer, fd);
        notifyDataSetChanged();
    }

    private void populateViewThumbnail(View view, FileDescriptor fd) {
        ImageView fileThumbnail = findView(view, R.id.view_browse_peer_list_item_file_thumbnail);
        fileThumbnail.setScaleType(ImageView.ScaleType.CENTER);

        if (local && fileType == Constants.FILE_TYPE_APPLICATIONS) {
            InputStream is = null;

            try {
                ContentResolver cr = getContext().getContentResolver();
                is = cr.openInputStream(Uri.withAppendedPath(Applications.Media.CONTENT_URI_ITEM, String.valueOf(fd.id)));
                Drawable icon = Drawable.createFromStream(is, "");
                fileThumbnail.setImageDrawable(icon);
            } catch (Throwable e) {
                fileThumbnail.setImageDrawable(fileTypeDrawable);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        } else {
            thumbnailLoader.displayImage(fd, fileThumbnail, fileTypeDrawable);
        }

        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        fileThumbnail.setTag(fd);
        fileThumbnail.setOnClickListener(downloadButtonClickListener);
    }

    /*
    private Drawable getDrawable(FileDescriptor fd) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = getContext().getContentResolver();

            if (fd.fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, fd.id, Images.Thumbnails.MICRO_KIND, null);
            } else if (fd.fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, fd.id, Video.Thumbnails.MICRO_KIND, null);
            }
        } catch (Throwable e) {
            // ignore
        }

        return bmp != null ? new BitmapDrawable(bmp) : null;
    }
    */

    /**
     * Same factors are considered to show the padlock icon state and color.
     * 
     * When the file is not local and it's been marked for download the text color appears as blue.
     * 
     * @param fd
     * @param padlock
     * @param title
     */
    private void populatePadlockAppearance(FileDescriptor fd, ImageButton padlock, TextView title) {
        if (local) {
            padlock.setVisibility(View.VISIBLE);
            padlock.setTag(fd);
            padlock.setOnClickListener(padLockClickListener);

            if (fd.shared) {
                padlock.setImageResource(R.drawable.browse_peer_padlock_unlocked_icon);
            } else {
                padlock.setImageResource(R.drawable.browse_peer_padlock_locked_icon);
            }
        } else {
            padlock.setVisibility(View.GONE);
        }
    }

    private void populateViewPlain(View view, FileDescriptor fd) {
        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        ImageButton downloadButton = findView(view, R.id.view_browse_peer_list_item_download);

        if (local) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                downloadButton.setImageResource(R.drawable.browse_peer_stop_icon);
            } else {
                downloadButton.setImageResource(R.drawable.browse_peer_play_icon);
            }
        }

        downloadButton.setTag(fd);
        downloadButton.setOnClickListener(downloadButtonClickListener);
    }

    private boolean showSingleOptions(List<FileDescriptor> checked, FileDescriptor fd) {
        if (checked.size() > 1) {
            return false;
        }
        if (checked.size() == 1) {
            return checked.get(0).equals(fd);
        }
        return true;
    }

    private static int getViewItemId(boolean local, byte fileType) {
        if (local && (fileType == Constants.FILE_TYPE_PICTURES || fileType == Constants.FILE_TYPE_VIDEOS || fileType == Constants.FILE_TYPE_APPLICATIONS)) {
            return R.layout.view_browse_thumbnail_peer_list_item;
        } else {
            return R.layout.view_browse_peer_list_item;
        }
    }

    private static int getFileTypeIconId(byte fileType) {
        switch (fileType) {
        case Constants.FILE_TYPE_APPLICATIONS:
            return R.drawable.browse_peer_application_icon_selector_off;
        case Constants.FILE_TYPE_AUDIO:
            return R.drawable.browse_peer_audio_icon_selector_off;
        case Constants.FILE_TYPE_DOCUMENTS:
            return R.drawable.browse_peer_document_icon_selector_off;
        case Constants.FILE_TYPE_PICTURES:
            return R.drawable.browse_peer_picture_icon_selector_off;
        case Constants.FILE_TYPE_RINGTONES:
            return R.drawable.browse_peer_ringtone_icon_selector_off;
        case Constants.FILE_TYPE_VIDEOS:
            return R.drawable.browse_peer_video_icon_selector_off;
        default:
            return R.drawable.question_mark;
        }
    }

    private class FileListFilter implements ListAdapterFilter<FileDescriptor> {
        public boolean accept(FileDescriptor obj, CharSequence constraint) {
            String keywords = constraint.toString();

            if (keywords == null || keywords.length() == 0) {
                return true;
            }

            keywords = keywords.toLowerCase();

            FileDescriptor fd = obj;

            if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
                return fd.album.trim().toLowerCase().contains(keywords) || fd.artist.trim().toLowerCase().contains(keywords) || fd.title.trim().toLowerCase().contains(keywords) || fd.filePath.trim().toLowerCase().contains(keywords);
            } else {
                return fd.title.trim().toLowerCase().contains(keywords) || fd.filePath.trim().toLowerCase().contains(keywords);
            }
        }
    }

    private final class PadLockClickListener implements OnClickListener {
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            fd.shared = !fd.shared;

            notifyDataSetChanged();
            Librarian.instance().updateSharedStates(fileType, Arrays.asList(fd));
        }
    }

    private final class DownloadButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            if (local) {
                localPlay(fd);
            } else {

                List<FileDescriptor> list = new ArrayList<FileDescriptor>(getChecked());

                if (list == null || list.size() == 0) {
                    // if no files are selected, they want to download this one.
                    startDownload(fd);

                    UIUtils.showLongMessage(getContext(), R.string.download_added_to_queue);
                } else {

                    // if many are selected... do they want to download many
                    // or just this one?
                    List<MenuAction> items = new ArrayList<MenuAction>(2);

                    items.add(new DownloadCheckedMenuAction(getContext(), FileListAdapter.this, list, peer));
                    items.add(new DownloadMenuAction(getContext(), FileListAdapter.this, peer, fd));

                    MenuAdapter menuAdapter = new MenuAdapter(getContext(), R.string.wanna_download_question, items);

                    trackDialog(new MenuBuilder(menuAdapter).show());
                }
            }
        }
    }
}