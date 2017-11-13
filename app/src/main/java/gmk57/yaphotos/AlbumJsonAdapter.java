package gmk57.yaphotos;

import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AlbumJsonAdapter extends TypeAdapter<Album> {
    private static final String TAG = "AlbumJsonAdapter";
    private int mScreenSize;

    public AlbumJsonAdapter() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mScreenSize = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public AlbumJsonAdapter(int screenSize) {
        mScreenSize = screenSize;
    }

    @Override
    public void write(JsonWriter out, Album value) throws IOException {/* Not needed */}

    @Override
    public Album read(JsonReader in) throws IOException {
        Album album = new Album();
        String id = "";

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "id":
                    id = in.nextString();
                    break;
                case "entries":
                    in.beginArray();
                    while (in.hasNext()) {
                        readPhoto(in, album);
                    }
                    in.endArray();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        // Try workaround to generate next page offset, if applicable
        if (id.equals("urn:yandex:fotki:pod:history") && album.getLastPodDate() != null) {
            try {
                album.setNextOffset(calculateNextOffset(album.getLastPodDate()));
            } catch (ParseException e) {/* Workaround failed, not a big deal */}
        }

        return album;
    }

    private void readPhoto(JsonReader in, Album album) throws IOException {
        Photo photo = new Photo();

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "author":
                    photo.setAuthor(in.nextString());
                    break;
                case "title":
                    photo.setTitle(in.nextString());
                    break;
                case "podDate":
                    album.setLastPodDate(in.nextString());
                    break;
                case "links":
                    in.beginObject();
                    while (in.hasNext()) {
                        if (in.nextName().equals("alternate")) {
                            photo.setPageUrl(in.nextString());
                        } else {
                            in.skipValue();
                        }
                    }
                    in.endObject();
                    break;
                case "img":
                    readImgLinks(in, photo);
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        album.addPhoto(photo);
    }

    private void readImgLinks(JsonReader in, Photo photo) throws IOException {
        Map<String, String> imgLinks = new HashMap<>();

        in.beginObject();
        while (in.hasNext()) {
            String size = in.nextName();
            in.beginObject();
            while (in.hasNext()) {
                if (in.nextName().equals("href")) {
                    imgLinks.put(size, in.nextString());
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
        }
        in.endObject();

        photo.setThumbnailUrl(imgLinks.get("M"));

        // Photo image URL quality is chosen as a trade-off between quality and network usage
        // (based on device screen size and available image sizes)
        if (mScreenSize > 1024 && imgLinks.containsKey("XXXL")) {
            photo.setImageUrl(imgLinks.get("XXXL"));
        } else if (mScreenSize > 800 && imgLinks.containsKey("XXL")) {
            photo.setImageUrl(imgLinks.get("XXL"));
        } else if (imgLinks.containsKey("XL")) {
            photo.setImageUrl(imgLinks.get("XL"));
        } else {
            photo.setImageUrl(imgLinks.get("L"));
        }
    }

    /**
     * Workaround to calculate next page offset for "Photos of the day" album.
     * <p>
     * For "Recent" and "Popular" albums next pages do not exist. For "Photos of the day" they
     * exist all the way back to year 2007, but for unknown reason corresponding JSON entry is
     * not provided.
     * <p>
     * As a workaround, next page address for "Photos of the day" can be calculated based on the
     * last photo in current page. It currently works, but may break anytime in the future.
     *
     * @param lastPodDate podDate property of last photo in album
     * @return URL of next page
     * @throws ParseException If DateFormat parsing fails
     */
    private String calculateNextOffset(String lastPodDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date lastDate = dateFormat.parse(lastPodDate);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastDate);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date prevDate = calendar.getTime();

        return "poddate;" + dateFormat.format(prevDate) + "/";
    }
}
