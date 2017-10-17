package gmk57.yaphotos;

import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    public static class AlbumDeserializer implements JsonDeserializer<Album> {
        @Override
        public Album deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Album album = new Album();
            JsonObject albumJson = json.getAsJsonObject();
            Type photosType = new TypeToken<List<Photo>>(){}.getType();
            List<Photo> photos = context.deserialize(albumJson.get("entries"), photosType);
            album.setPhotos(photos);

            if (albumJson.getAsJsonObject("links").has("next")) {
                album.setNextPage(albumJson.getAsJsonObject("links").get("next").getAsString());
            } else if (photos.size() > 0) {
                try {
                    album.setNextPage(calculateNextPage(photos));
                } catch (ParseException e) {/* Workaround failed, not a big deal */}
            }

            return album;
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
         * @param photos List of photos. May be empty, but not null.
         * @return URL of next page, or null if it cannot be calculated.
         * @throws ParseException If DateFormat parsing fails
         */
        @Nullable
        private String calculateNextPage(@NonNull List<Photo> photos) throws ParseException {
            Photo lastPhoto = photos.get(photos.size() - 1);
            if (lastPhoto.getPodDate() == null) return null;

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date lastDate = dateFormat.parse(lastPhoto.getPodDate());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(lastDate);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date prevDate = calendar.getTime();

            String offset = "poddate;" + dateFormat.format(prevDate) + "/";
            return new YaDownloader().buildUrl("podhistory", offset);
        }
    }

    public static class PhotoDeserializer implements JsonDeserializer<Photo> {
        private int mScreenSize;

        public PhotoDeserializer() {
            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
            mScreenSize = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        }

        @Override
        public Photo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Photo photo = new Photo();
            JsonObject photoJson = json.getAsJsonObject();
            photo.setAuthor(photoJson.get("author").getAsString());
            photo.setTitle(photoJson.get("title").getAsString());
            photo.setPageUrl(photoJson.getAsJsonObject("links").get("alternate").getAsString());

            // Photo image URL quality is chosen as a trade-off between quality and network usage
            // (based on device screen size and available image sizes)
            JsonObject imgJson = photoJson.get("img").getAsJsonObject();
            photo.setThumbnailUrl(imgJson.getAsJsonObject("M").get("href").getAsString());
            if (mScreenSize > 1024 && imgJson.has("XXXL")) {
                photo.setImageUrl(imgJson.getAsJsonObject("XXXL").get("href").getAsString());
            } else if (mScreenSize > 800 && imgJson.has("XXL")) {
                photo.setImageUrl(imgJson.getAsJsonObject("XXL").get("href").getAsString());
            } else if (imgJson.has("XL")) {
                photo.setImageUrl(imgJson.getAsJsonObject("XL").get("href").getAsString());
            } else if (imgJson.has("L")) {
                photo.setImageUrl(imgJson.getAsJsonObject("L").get("href").getAsString());
            }

            if (photoJson.has("podDate")) {
                photo.setPodDate(photoJson.get("podDate").getAsString());
            }

            return photo;
        }
    }
}
