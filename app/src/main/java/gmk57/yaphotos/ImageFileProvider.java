package gmk57.yaphotos;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;

/**
 * FileProvider subclass to provide appropriate MIME type and file extension (by default files in
 * Glide cache have neither). Without MIME type most apps can't share the image correctly. JPG file
 * extension is needed for some of them too. Extension is also useful in case recipient will save
 * received file.
 * <p>
 * Based on <a href="https://github.com/bumptech/glide/issues/459#issuecomment-227701570">
 * this code sample by TWiStErRob</a>
 */
public class ImageFileProvider extends FileProvider {
    private static final String TAG = "ImageFileProvider";

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor original = super.query(uri, projection, selection, selectionArgs, sortOrder);
        if (original == null) return null;

        MatrixCursor copy = new MatrixCursor(original.getColumnNames(), original.getCount());
        if (original.moveToFirst()) {
            copy.addRow(copyCurrentRowWithChangedDisplayName(original));
        }
        original.close();
        return copy;
    }

    private Object[] copyCurrentRowWithChangedDisplayName(Cursor original) {
        Object[] row = new Object[original.getColumnCount()];

        for (int i = 0; i < row.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(original.getColumnName(i))) {
                row[i] = String.valueOf(System.currentTimeMillis()) + ".jpg";
            } else if (OpenableColumns.SIZE.equals(original.getColumnName(i))) {
                row[i] = original.getLong(i);
            }
        }
        return row;
    }
}
