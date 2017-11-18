package gmk57.yaphotos.data;

import android.support.annotation.IntRange;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Value constraint annotation to make sure only valid album types are used.
 * Valid values = valid ALBUM_PATHS indexes
 */
@Retention(RetentionPolicy.SOURCE)
@IntRange(from = 0, to = 2)  // Must match ALBUM_PATHS.length - 1
public @interface AlbumType {
}
