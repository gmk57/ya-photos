package gmk57.yaphotos;

import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Class to perform low-level network operations and API-specific operations.
 * Does not have any internal state.
 */
public class YaDownloader {
    public static final String RECENT = "recent";
    public static final String POPULAR = "top";
    public static final String DAY = "podhistory";
    private static final String TAG = "YaDownloader";

    public String downloadString(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = null;

        try {
            in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlString);
            }

            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            return new String(out.toByteArray());

        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {/*closing quietly*/}
            try {
                out.close();
            } catch (IOException e) {/*closing quietly*/}
            connection.disconnect();
        }
    }

    /**
     * Builds URL to fetch album
     *
     * @param albumType   One of YaDownloader's constants: <code>RECENT</code>,
     *                    <code>POPULAR</code> or <code>DAY</code>.
     * @param lastSegment One more segment to fetch next page. May be empty,
     *                    but not null.
     * @return URL to fire
     */
    private String buildUrl(@NonNull String albumType, @NonNull String lastSegment) {
        return Uri.parse("http://api-fotki.yandex.ru/api/")
                .buildUpon()
                .appendEncodedPath(albumType + "/")
                .appendEncodedPath(lastSegment)
                .appendQueryParameter("format", "json")
                .build().toString();
    }

    /**
     * Fetches JSON from server and parses it.
     * <p>
     * If old album is provided, new album will be built on top of it, appending
     * photos of its <code>getNextPage()</code>.
     * Otherwise, new album will be built from scratch, according to album type
     * in shared preferences.
     *
     * @param preferenceConnector Connector to shared preferences (to avoid passing
     *                            Context)
     * @param oldAlbumVararg      Old album (to append) or empty (to create from scratch)
     * @return New album
     */
    public Album fetchAlbum(PreferenceConnector preferenceConnector, Album... oldAlbumVararg) {
        Album oldAlbum = null;
        String urlString;
        if (oldAlbumVararg.length > 0) {    // Create on top of oldAlbum
            oldAlbum = oldAlbumVararg[0];
            urlString = oldAlbum.getNextPage();
        } else {                            // Create from scratch
            String albumType = preferenceConnector.getAlbumType();
            urlString = buildUrl(albumType, "");
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
        return buildUrl(DAY, offset);
    }
}
