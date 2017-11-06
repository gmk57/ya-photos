package gmk57.yaphotos;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class AlbumTest {
    @Test
    public void constructor_photosEmptyButNotNull() throws Exception {
        Album album = new Album();
        assertNotNull(album.getPhotos());
        assertThat(album.getSize(), is(0));
    }

    @Test
    public void appendToOldAlbum() throws Exception {
        Album oldAlbum = new Album();
        Photo first = new Photo();
        oldAlbum.addPhoto(first);
        oldAlbum.addPhoto(new Photo());

        Album newAlbum = new Album();
        Photo last = new Photo();
        newAlbum.addPhoto(last);
        newAlbum.setNextOffset("dummy_offset");
        newAlbum.appendToOldAlbum(oldAlbum);

        assertThat(newAlbum.getSize(), is(3));
        assertThat(newAlbum.getPhoto(0), is(first));
        assertThat(newAlbum.getPhoto(2), is(last));
        assertThat(newAlbum.getNextOffset(), is("dummy_offset"));
        assertThat(oldAlbum.getSize(), is(2));  // Old album should not be altered
    }

    @Test
    public void appendToOldAlbum_oldIsEmpty() throws Exception {
        Album oldAlbum = new Album();

        Album newAlbum = new Album();
        newAlbum.addPhoto(new Photo());
        newAlbum.appendToOldAlbum(oldAlbum);

        assertThat(newAlbum.getSize(), is(1));
        assertThat(oldAlbum.getSize(), is(0));  // Old album should not be altered
    }

    @Test
    public void appendToOldAlbum_newIsEmpty() throws Exception {
        Album oldAlbum = new Album();
        oldAlbum.addPhoto(new Photo());
        oldAlbum.addPhoto(new Photo());
        oldAlbum.setNextOffset("old_offset");

        Album newAlbum = new Album();
        newAlbum.appendToOldAlbum(oldAlbum);

        assertThat(newAlbum.getSize(), is(2));
        assertNull(newAlbum.getNextOffset());  // Offset should be updated
    }
}