package gmk57.yaphotos;

import com.google.gson.stream.JsonReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import gmk57.yaphotos.data.Album;
import gmk57.yaphotos.data.Photo;
import gmk57.yaphotos.data.source.AlbumJsonAdapter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AlbumJsonAdapterRoboTest {
    @Test
    public void jsonBasicParsing() throws Exception {
        Album album = parseJsonFromFile("basic.json");

        assertThat("Album has wrong next offset", album.getNextOffset(), is(nullValue()));
        assertThat("Album has wrong size", album.getSize(), is(1));

        Photo photo = album.getPhoto(0);

        assertThat("Photo has wrong author", photo.getAuthor(), is("thai-cats"));
        assertThat("Photo has wrong title", photo.getTitle(), is("DSC_0061.JPG"));
        assertThat("Photo has wrong page url", photo.getPageUrl(),
                is("http://fotki.yandex.ru/users/thai-cats/view/1197620/"));
        assertThat("Photo has wrong thumb url", photo.getThumbnailUrl(),
                is("https://img4-fotki.yandex.net/get/764457/11436194.2ad/0_STATIC124634_f3f9028a_M"));
        assertThat("Photo has wrong image url", photo.getImageUrl(),
                is("https://img4-fotki.yandex.net/get/764457/11436194.2ad/0_STATIC124634_f3f9028a_XL"));
    }

    @Test
    public void smallAlbumParsing() throws Exception {
        Album album = parseJsonFromFile("small.json");

        assertThat("Album has wrong next offset", album.getNextOffset(),
                is("poddate;2007-05-31T00:00:00Z/"));
        assertThat("Album has wrong size", album.getSize(), is(2));

        Photo photo = album.getPhoto(0);

        assertThat("Unicode is not properly handled", photo.getTitle(),
                is("Турецкая жаба"));
        assertThat("Image missing size fallback is broken", photo.getImageUrl(),
                is("http://img-fotki.yandex.ru/get/1/mugler.0/0_41d_30354026_L"));
    }

    @Test
    public void emptyAlbumParsing() throws Exception {
        Album album = parseJsonFromFile("empty.json");

        assertThat("Album should never be null", album, is(not(nullValue())));
        assertThat("Album has wrong next offset", album.getNextOffset(), is(nullValue()));
        assertThat("Album has wrong size", album.getSize(), is(0));
    }

    @Test
    public void bigAlbumParsing() throws Exception {
        Album album = parseJsonFromFile("big.json");

        assertThat("Album has wrong next offset", album.getNextOffset(),
                is("poddate;2017-08-01T00:00:00Z/"));
        assertThat("Album has wrong size", album.getSize(), is(100));

        assertThat("First photo title is wrong", album.getPhoto(0).getTitle(),
                is("DSC_7561.jpg"));
        assertThat("7th photo title is wrong", album.getPhoto(7).getTitle(),
                is("DSC01308.JPG"));
        assertThat("Last photo title is wrong", album.getPhoto(99).getTitle(),
                is("генеральная репетиция парада в честь Дня военно-морского флота"));
    }

    private Album parseJsonFromFile(String filename) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
        JsonReader jsonReader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        return new AlbumJsonAdapter().read(jsonReader);
    }
}
