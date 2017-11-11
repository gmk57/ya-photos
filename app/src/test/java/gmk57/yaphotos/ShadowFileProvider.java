package gmk57.yaphotos;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Robolectric Shadow for mocking ImageFileProvider's call to <code>super.query()</code>, which
 * fails otherwise. Inspired by
 * <a href="https://github.com/robolectric/robolectric/issues/2199#issuecomment-208976402">idea of
 * JavierSP1209</a>
 */
@Implements(FileProvider.class)
public class ShadowFileProvider {
    @SuppressWarnings("unused")
    @Implementation
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if (uri == null) return null;

        String[] columns = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        Object[] values = {"dummy", 500};
        MatrixCursor cursor = new MatrixCursor(columns, 1);

        if (!uri.equals(Uri.EMPTY)) {
            cursor.addRow(values);
        }
        return cursor;
    }
}
