package gmk57.yaphotos;

import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
    private String buildUrl(String albumPath, String lastSegment) {
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
        Album newAlbum = new Album(oldAlbum);

        try {
            String jsonString = downloadString(urlString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseJson(jsonBody, newAlbum);
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Failed to fetch album: " + e);
        } catch (JSONException | ParseException e) {
            Log.e(TAG, "Failed to parse JSON" + e);
        }

        return newAlbum;
    }

    /**
     * Parses provided JSON, constructs Photos and adds them to provided Album.
     * <p>
     * Photo image URL quality is chosen as a trade-off between quality and
     * network usage (based on device screen size and available image sizes).
     *
     * @param jsonBody JSON to parse
     * @param album    Album to add Photo objects
     * @throws JSONException
     * @throws ParseException
     */
    private void parseJson(JSONObject jsonBody, Album album)
            throws JSONException, ParseException {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int screenSize = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);

        JSONArray photoJsonArray = jsonBody.getJSONArray("entries");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            Photo photo = new Photo();
            photo.setTitle(photoJsonObject.getString("title"));
            photo.setAuthor(photoJsonObject.getString("author"));
            photo.setPageUrl(photoJsonObject.getJSONObject("links").getString("alternate"));

            JSONObject photoImgJsonObject = photoJsonObject.getJSONObject("img");
            if (!photoImgJsonObject.has("M")) {
                continue;  // Skip photos without thumbnails
            }
            photo.setThumbnailUrl(photoImgJsonObject.getJSONObject("M").getString("href"));

            if (screenSize > 1024 && photoImgJsonObject.has("XXXL")) {
                photo.setImageUrl(photoImgJsonObject.getJSONObject("XXXL").getString("href"));
            } else if (screenSize > 800 && photoImgJsonObject.has("XXL")) {
                photo.setImageUrl(photoImgJsonObject.getJSONObject("XXL").getString("href"));
            } else if (photoImgJsonObject.has("XL")) {
                photo.setImageUrl(photoImgJsonObject.getJSONObject("XL").getString("href"));
            } else {
                continue;  // Skip photos without full-size images
            }
            album.addPhoto(photo);
        }

        album.setNextPage(jsonBody.getJSONObject("links").optString("next",
                calculateNextPage(photoJsonArray)));
    }

    /**
     * Workaround to calculate next page for "Photos of the day" album.
     * <p>
     * For "Recent" and "Popular" albums next pages do not exist. For "Photos
     * of the day" they exist all the way back to year 2007, but for unknown
     * reason corresponding JSON entry is provided only for the first page.
     * <p>
     * As a workaround, nest page address for "Photos of the day" can be
     * calculated based on the last photo in the current page. It currently
     * works, but may break in the future.
     *
     * @param photoJsonArray Array of photos. May be empty.
     * @return URL of next page, or null if it cannot be calculated.
     * @throws ParseException
     */
    private String calculateNextPage(JSONArray photoJsonArray) throws ParseException {
        JSONObject lastPhoto = photoJsonArray.optJSONObject(photoJsonArray.length() - 1);
        if (lastPhoto == null || !lastPhoto.has("podDate")) return null;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date lastDate = dateFormat.parse(lastPhoto.optString("podDate"));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastDate);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date prevDate = calendar.getTime();

        String offset = "poddate;" + dateFormat.format(prevDate) + "/";
        return buildUrl("podhistory", offset);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntRange(from = 0, to = 2)  // Must match ALBUM_PATHS.length - 1
    public @interface AlbumType {
    }
}
