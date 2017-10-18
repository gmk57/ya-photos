package gmk57.yaphotos;

import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class to perform low-level network operations and API-specific operations.
 * Does not have any internal state.
 */
public class YaDownloader {
    public static final String[] ALBUM_PATHS = {"recent", "top", "podhistory"};
    private static final String TAG = "YaDownloader";

    private String downloadString(String urlString) throws IOException {
        OkHttpClient client = Repository.getInstance().getOkHttpClient();
        Request request = new Request.Builder().url(urlString).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException(response.code() + " " + response.message() + ": " + urlString);
            }
            //noinspection ConstantConditions
            return response.body().string();
        } finally {
            if (response != null) response.close();
        }
    }

    /**
     * Builds URL to fetch album
     *
     * @param albumPath   One of the values in YaDownloader's <code>ALBUM_PATHS</code>.
     * @param lastSegment One more segment to fetch next page. May be empty, but not null.
     * @return URL to fire
     */
    public String buildUrl(String albumPath, String lastSegment) {
        return Uri.parse("http://api-fotki.yandex.ru/api/")
                .buildUpon()
                .appendEncodedPath(albumPath + "/")
                .appendEncodedPath(lastSegment)
                .appendQueryParameter("format", "json")
                .build().toString();
    }

    /**
     * Fetches JSON from server and parses it.
     * <p>
     * If old album is provided, new album will be built on top of it, appending
     * photos of its <code>getNextPage()</code>.
     * Otherwise, new album will be built from scratch, according to provided album type.
     *
     * @param albumType Valid values = YaDownloader.ALBUM_PATHS indexes
     * @param oldAlbum  Old album (to append) or null (to create from scratch)
     * @return New album
     */
    @WorkerThread
    @NonNull
    public Album fetchAlbum(@AlbumType int albumType, @Nullable Album oldAlbum) {
        String urlString;
        if (oldAlbum != null) {                     // Create on top of oldAlbum
            urlString = oldAlbum.getNextPage();
        } else {                                    // Create from scratch
            String albumPath = ALBUM_PATHS[albumType];
            urlString = buildUrl(albumPath, "");
        }
        Album newAlbum = new Album();

        try {
            String jsonString = downloadString(urlString);
            newAlbum = new Gson().fromJson(jsonString, Album.class);
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Failed to fetch album: " + e);
        }

        if (oldAlbum != null) {
            // If new album is null or error occurred, return old album
            newAlbum.appendToOldAlbum(oldAlbum);
        }
        return newAlbum;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntRange(from = 0, to = 2)  // Must match ALBUM_PATHS.length - 1
    public @interface AlbumType {
    }
}
