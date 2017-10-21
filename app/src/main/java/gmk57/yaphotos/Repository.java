package gmk57.yaphotos;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Singleton class, responsible for creating and holding model objects.
 */
public class Repository {
    public static final String[] ALBUM_PATHS = {"recent", "top", "podhistory"};
    private static final String TAG = "Repository";
    private final AtomicReferenceArray<Album> mAlbums;
    private final AtomicBoolean[] mFetchRunning;

    private Repository() {
        int length = ALBUM_PATHS.length;
        mAlbums = new AtomicReferenceArray<>(length);
        mFetchRunning = new AtomicBoolean[length];
        for (int i = 0; i < length; i++) {
            mAlbums.set(i, new Album());
            mFetchRunning[i] = new AtomicBoolean();
        }
    }

    public static Repository getInstance() {
        return RepositoryHolder.INSTANCE;
    }

    /**
     * Tries to provide album of specified type immediately. If album is not available, returns
     * empty album and automatically starts asynchronous request. Callers should subscribe to
     * AlbumLoadedEvent through EventBus to receive its results.
     *
     * @param albumType Album type
     * @return Album. May be empty, but not null.
     */
    @UiThread
    @NonNull
    public Album getAlbum(@AlbumType int albumType) {
        Album album = mAlbums.get(albumType);
        if (album.getSize() == 0) {
            reloadAlbum(albumType);
        }
        return album;
    }

    /**
     * Starts fetching album of specified type from network (if it isn't currently loading),
     * regardless of whether it's already available. Callers should subscribe to AlbumLoadedEvent
     * through EventBus to receive results.
     *
     * @param albumType Album type
     */
    @UiThread
    public void reloadAlbum(@AlbumType int albumType) {
        if (mFetchRunning[albumType].compareAndSet(false, true)) {
            new FetchAlbumThread(albumType, null).start();
        }
    }

    /**
     * Starts fetching next page of album, if next page exists and this album isn't currently
     * loading. Callers should subscribe to AlbumLoadedEvent through EventBus to receive results.
     *
     * @param albumType Album type
     */
    @UiThread
    public void fetchNextPage(@AlbumType int albumType) {
        if (mAlbums.get(albumType).getNextOffset() != null
                && mFetchRunning[albumType].compareAndSet(false, true)) {
            new FetchAlbumThread(albumType, mAlbums.get(albumType)).start();
        }
    }

    /**
     * Provides single instance of NetworkLayer, initialized on first invocation.
     *
     * @return Instance of NetworkLayer
     */
    @WorkerThread
    private NetworkLayer getNetworkLayer() {
        return NetworkHolder.NETWORK_LAYER;
    }

    /**
     * Value constraint annotation to make sure only valid album types are used.
     * Valid values = valid ALBUM_PATHS indexes
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntRange(from = 0, to = 2)  // Must match ALBUM_PATHS.length - 1
    public @interface AlbumType {
    }

    /**
     * Retrofit interface for accessing server API.
     */
    interface NetworkLayer {
        @GET("{albumPath}/{offset}?format=json")
        Call<Album> downloadAlbum(@Path("albumPath") String albumPath,
                                  @Path("offset") String offset);
    }

    /**
     * Initialization-on-demand holder singleton implementation.
     */
    private static class RepositoryHolder {
        static final Repository INSTANCE = new Repository();
    }

    /**
     * Provides lazy and thread-safe initialization of NetworkLayer single instance,
     * like in initialization-on-demand holder singleton idiom.
     */
    private static class NetworkHolder {
        static final NetworkLayer NETWORK_LAYER = new Retrofit.Builder()
                .baseUrl("http://api-fotki.yandex.ru/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NetworkLayer.class);
    }

    /**
     * Background thread to fetch album from network.
     */
    private class FetchAlbumThread extends Thread {
        private static final String TAG = "FetchAlbumThread";
        private int mAlbumType;
        private Album mOldAlbum;

        /**
         * Fetches JSON from server, parses it and sends AlbumLoadedEvent to subscribers.
         * <p>
         * If old album is provided, new album will be built on top of it, appending photos of its
         * next page. Otherwise, album will be built from scratch, according to provided album type.
         * <p>
         * In case of fetch failure, album in repository remains untouched.
         *
         * @param albumType Valid values = ALBUM_PATHS indexes
         * @param oldAlbum  Old album (to append) or null (to create from scratch)
         */
        public FetchAlbumThread(@AlbumType int albumType, @Nullable Album oldAlbum) {
            mAlbumType = albumType;
            mOldAlbum = oldAlbum;
        }

        @Override
        public void run() {
            String albumPath = ALBUM_PATHS[mAlbumType];
            String offset = (mOldAlbum == null) ? "" : mOldAlbum.getNextOffset();
            NetworkLayer networkLayer = getNetworkLayer();

            try {
                Response<Album> response = networkLayer.downloadAlbum(albumPath, offset).execute();

                if (response.isSuccessful()) {
                    Album album = response.body();
                    if (mOldAlbum != null && album != null) {
                        album.appendToOldAlbum(mOldAlbum);
                    }
                    mAlbums.set(mAlbumType, album);

                } else {
                    Log.e(TAG, "Failed to fetch album: " + response.code() + " "
                            + response.message());
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to fetch album: " + e);
            }

            EventBus.getDefault().post(new AlbumLoadedEvent(mAlbumType));
            mFetchRunning[mAlbumType].set(false);
        }
    }
}
