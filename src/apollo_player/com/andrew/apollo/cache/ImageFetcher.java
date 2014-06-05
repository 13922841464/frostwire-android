/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.cache;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.widget.ImageView;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.cache.ImageWorker.ImageType;
import com.andrew.apollo.utils.MusicUtils;
import com.frostwire.android.R;
import com.frostwire.android.util.ImageLoader;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher {

    /**
     * Used to distinguish album art from artist images
     */
    public static final String ALBUM_ART_SUFFIX = "album";

    public static final int IO_BUFFER_SIZE_BYTES = 1024;

    private final Context context;

    /**
     * Default album art
     */
    private final Bitmap mDefault;
    private final ImageLoader imageLoader;

    private static ImageFetcher sInstance = null;

    /**
     * Creates a new instance of {@link ImageFetcher}.
     *
     * @param context The {@link Context} to use.
     */
    public ImageFetcher(final Context context) {
        this.context = context.getApplicationContext();
        this.mDefault = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.default_artwork)).getBitmap();
        this.imageLoader = ImageLoader.getInstance(context);
    }

    /**
     * Used to create a singleton of the image fetcher
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static final ImageFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentArtwork(final ImageView imageView) {
        loadImage(generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName()), MusicUtils.getArtistName(), MusicUtils.getAlbumName(), MusicUtils.getCurrentAlbumId(), imageView, ImageType.ALBUM);
    }

    /**
     * Finds cached or downloads album art. Used in {@link MusicPlaybackService}
     * to set the current album art in the notification and lock screen
     *
     * @param albumName The name of the current album
     * @param albumId The ID of the current album
     * @param artistName The album artist in case we should have to download
     *            missing artwork
     * @return The album art as an {@link Bitmap}
     */
    public Bitmap getArtwork(final String albumName, final long albumId, final String artistName) {
        if (albumId < 0) {
            return null;
        }

        Bitmap artwork = null;

        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumId);

        if (isMain()) {
            artwork = getArtworkFromFile(context, uri);
        } else {
            artwork = imageLoader.get(uri);
        }

        return artwork != null ? artwork : getDefaultArtwork();
    }

    /**
     * Generates key used by album art cache. It needs both album name and artist name
     * to let to select correct image for the case when there are two albums with the
     * same artist.
     *
     * @param albumName The album name the cache key needs to be generated.
     * @param artistName The artist name the cache key needs to be generated.
     * @return
     */
    public static String generateAlbumCacheKey(final String albumName, final String artistName) {
        if (albumName == null || artistName == null) {
            return null;
        }
        return new StringBuilder(albumName).append("_").append(artistName).append("_").append(ALBUM_ART_SUFFIX).toString();
    }

    /**
     * @return The deafult artwork
     */
    public Bitmap getDefaultArtwork() {
        return mDefault;
    }

    protected void loadImage(final String key, final String artistName, final String albumName, final long albumId, final ImageView imageView, final ImageType imageType) {
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumId);
        imageLoader.load(uri, imageView, R.drawable.default_artwork);
    }

    public final Bitmap getArtworkFromFile(final Context context, Uri uri) {
        Bitmap artwork = null;
        try {
            final ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                artwork = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (final IllegalStateException e) {
            // Log.e(TAG, "IllegalStateExcetpion - getArtworkFromFile - ", e);
        } catch (final FileNotFoundException e) {
            // Log.e(TAG, "FileNotFoundException - getArtworkFromFile - ", e);
        } catch (final OutOfMemoryError evict) {
            // Log.e(TAG, "OutOfMemoryError - getArtworkFromFile - ", evict);
            imageLoader.clear();
        }
        return artwork;
    }

    static boolean isMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
