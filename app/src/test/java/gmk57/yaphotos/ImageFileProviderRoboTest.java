package gmk57.yaphotos;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowFileProvider.class)
public class ImageFileProviderRoboTest {
    private ImageFileProvider mProvider;

    @Before
    public void setUp() throws Exception {
        mProvider = new ImageFileProvider();
    }

    @Test
    public void getType_ReturnsJpeg() throws Exception {
        assertThat("Wrong type for null Uri", mProvider.getType(null),
                is("image/jpeg"));

        Uri uri = Uri.parse("content://gmk57.yaphotos.fileprovider/share/f347ead59ad5ad934710.0");

        assertThat("Wrong type for non-null Uri", mProvider.getType(uri),
                is("image/jpeg"));
    }

    @Test
    public void query_NullResult() throws Exception {
        Cursor result = mProvider.query(null, null, null, null,
                null);

        assertThat("Null result from super should be returned", result, is(nullValue()));
    }

    @Test
    public void query_EmptyResult() throws Exception {
        Cursor result = mProvider.query(Uri.EMPTY, null, null, null,
                null);

        assertThat("Result should not be null", result, is(not(nullValue())));
        assertThat("Empty result from super should be returned", result.moveToFirst(),
                is(false));
    }

    @Test
    public void query_HasResult_JpgExtensionReturned() throws Exception {
        Uri uri = Uri.parse("content://gmk57.yaphotos.fileprovider/share/f347ead59ad5ad934710.0");
        String[] projection = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        Cursor result = mProvider.query(uri, projection, null, null,
                null);

        assertThat("Cursor is null", result, is(not(nullValue())));
        assertThat("Cursor is empty", result.moveToFirst(), is(true));

        int nameIdx = result.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        assertThat("Display name should have .jpg extension", result.getString(nameIdx),
                endsWith(".jpg"));

        int sizeIdx = result.getColumnIndex(OpenableColumns.SIZE);
        assertThat("Size should be taken from superclass", result.getInt(sizeIdx),
                is(500));
    }
}
