package gmk57.yaphotos;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Helper class to store and retrieve preferences via SharedPreferences
 */

public class PreferenceConnector {
    private static final String PREF_ALBUM_TYPE = "albumType";
    private SharedPreferences mSharedPreferences;

    public PreferenceConnector(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getAlbumType() {
        return mSharedPreferences.getString(PREF_ALBUM_TYPE, YaDownloader.RECENT);
    }

    public void setAlbumType(String albumType) {
        mSharedPreferences.edit()
                .putString(PREF_ALBUM_TYPE, albumType)
                .apply();
    }
}
