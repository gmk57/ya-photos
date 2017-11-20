package gmk57.yaphotos.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import gmk57.yaphotos.data.Album;
import gmk57.yaphotos.data.AlbumType;
import retrofit2.Response;

/**
 * Wrapper around Retrofit interface
 */
@Singleton
public class NetworkSource {
    private static final String TAG = "NetworkSource";
    private final NetworkApi mNetworkApi;

    @Inject
    public NetworkSource(NetworkApi networkApi) {
        mNetworkApi = networkApi;
    }

    /**
     * Synchronously fetches album from network.
     *
     * @param albumType Album type
     * @param offset    Last url segment for fetching next page. May be empty, but not null
     * @return Album if succeeded, null if failed
     */
    @Nullable
    @WorkerThread
    Album fetchAlbum(@AlbumType int albumType, @NonNull String offset) {
        String albumPath = AlbumRepository.ALBUM_PATHS[albumType];

        Album album = null;
        try {
            Response<Album> response = mNetworkApi.downloadAlbum(albumPath, offset).execute();

            if (response.isSuccessful()) {
                album = response.body();

            } else {
                Log.e(TAG, "Failed to fetch album: " + response.code() + " "
                        + response.message());
            }
        } catch (IOException | NullPointerException e) {
            Log.e(TAG, "Failed to fetch album: " + e);
        }
        return album;
    }
}
