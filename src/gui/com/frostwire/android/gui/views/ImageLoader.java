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

package com.frostwire.android.gui.views;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.util.MusicUtils;
import com.squareup.picasso.Loader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Builder;
import com.squareup.picasso.UrlConnectionLoader;


/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ImageLoader {

    private static final int CACHE_SIZE = 1024 * 1024 * 4; // 2MB

    private final Context context;

    private static ImageLoader defaultInstance;

    private final Picasso picasso;

    public static ImageLoader getDefault() {
        return defaultInstance;
    }

    public static void createDefaultInstance(Context context) {
        if (defaultInstance == null) {
            defaultInstance = new ImageLoader(context);
        }
    }

    public ImageLoader(Context context) {
        this.context = context;
        picasso = new Builder(context).loader(new ThumbnailLoader()).memoryCache(new LruCache(CACHE_SIZE)).build();
    }

    public void displayImage(FileDescriptor image, ImageView imageView, Drawable defaultDrawable) {
        displayImage(image, imageView, defaultDrawable, 1);
    }

    public void displayImage(FileDescriptor fd, ImageView imageView, Drawable defaultDrawable, int sampleSize) {
        StringBuilder path = getResourceIdentifier(fd);
        displayImage(path.toString(), imageView, defaultDrawable, sampleSize);
    }

    /**
     * @param fd
     * @return Depending on the file type returns either video:<id> or image:<id>
     */
    private StringBuilder getResourceIdentifier(FileDescriptor fd) {
        StringBuilder path = new StringBuilder();

        switch (fd.fileType) {
        case Constants.FILE_TYPE_PICTURES:
            path.append("image:");
            break;
        case Constants.FILE_TYPE_VIDEOS:
            path.append("video:");
            break;
        case Constants.FILE_TYPE_AUDIO:
            path.append("audio:");
            break;
        default:
            path.append("image:");
            break;
        }
        path.append(fd.id);
        return path;
    }

    public void displayImage(String imageSrc, ImageView imageView, Drawable defaultDrawable, int sampleSize) {
        if (defaultDrawable != null) {
            imageView.setScaleType(ScaleType.CENTER);
            picasso.setDebugging(true);
            picasso.load(imageSrc).placeholder(defaultDrawable).into(imageView);
        }
    }

    /**
    private Bitmap getBitmap(Context context, Object key, int sampleSize) {
        if (isKeyRemote(key)) {
            return getBitmap((String) key, sampleSize);
        } else if (key instanceof FileDescriptor) {
            return getBitmap(context, (FileDescriptor) key);
        } else {
            return null;
        }
    }

    private Bitmap getBitmap(String url, int sampleSize) {
        Bitmap bmp = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPurgeable = true;
            opts.inSampleSize = sampleSize;
            opts.inPreferQualityOverSpeed = false;

            bmp = BitmapFactory.decodeStream(conn.getInputStream(), null, opts);
        } catch (Throwable e) {
            bmp = null;
            // ignore
        }

        return bmp;
    }
    */

    private Bitmap overlayVideoIcon(Context context, Bitmap bmp) {
        Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(bitmap);

        canvas.drawBitmap(bmp, 0, 0, null);

        Bitmap playIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_icon_transparent);
        Rect src = new Rect(0, 0, playIcon.getWidth(), playIcon.getHeight());
        int dx = (bmp.getWidth() - src.width()) / 2;
        int dy = (bmp.getHeight() - src.height()) / 2;
        Rect dst = new Rect(dx, dy, src.width() + dx, src.height() + dy);
        canvas.drawBitmap(playIcon, src, dst, null);

        return bitmap;
    }

    private Bitmap getBitmap(Context context, byte fileType, long id) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = context.getContentResolver();

            if (fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, id, Images.Thumbnails.MICRO_KIND, null);
            } else if (fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, id, Video.Thumbnails.MICRO_KIND, null);
                bmp = overlayVideoIcon(context, bmp);
            } else if (fileType == Constants.FILE_TYPE_AUDIO) {
                bmp = MusicUtils.getArtwork(context, id, -1);
            }
        } catch (Throwable e) {
            bmp = null;
            // ignore
        }

        return bmp;
    }

    private class ThumbnailLoader implements Loader {
        
        private final Loader fallback;
        
        public ThumbnailLoader() {
            fallback = new UrlConnectionLoader(context);
        }

        /**
         * @param itemIdentifier video:<videoId>, or image:<imageId>, where the Id is an Integer.
         */
        @Override
        public Response load(String itemIdentifier, boolean localCacheOnly) throws IOException {
            Response response = null;
            
            byte fileType = getFileType(itemIdentifier);

            if (fileType != -1) {
                long id = getFileId(itemIdentifier);
                Bitmap bitmap = getBitmap(context, fileType, id);

                if (bitmap == null) {
                    throw new IOException("ThumbnailLoader - bitmap not found.");
                }

                response = new Response(convertToStream(bitmap), localCacheOnly);
            } else {
                response = fallback.load(itemIdentifier, localCacheOnly);
            }
            
            return response;
        }

        private InputStream convertToStream(Bitmap bitmap) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(bitmap.getByteCount());
            bitmap.compress(CompressFormat.PNG, 100, baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return bais;
        }

        private byte getFileType(String itemIdentifier) {
            byte fileType = -1;

            if (itemIdentifier.startsWith("image:")) {
                fileType = Constants.FILE_TYPE_PICTURES;
            } else if (itemIdentifier.startsWith("video:")) {
                fileType = Constants.FILE_TYPE_VIDEOS;
            } else if (itemIdentifier.startsWith("audio:")) {
                fileType = Constants.FILE_TYPE_AUDIO;
            }
            return fileType;
        }

        private long getFileId(String itemIdentifier) {
            return Long.valueOf(itemIdentifier.substring(6));
        }
    }
}