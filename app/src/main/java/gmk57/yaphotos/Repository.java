package gmk57.yaphotos;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import gmk57.yaphotos.YaDownloader.AlbumType;
import okhttp3.OkHttpClient;

/**
 * Singleton class, responsible for creating and holding model objects.
 */
public class Repository {
    private static final String TAG = "Repository";
    private final AtomicReferenceArray<Album> mAlbums;
    private final AtomicBoolean[] mFetchRunning;

    private Repository() {
        int length = YaDownloader.ALBUM_PATHS.length;
        mAlbums = new AtomicReferenceArray<>(length);
        mFetchRunning = new AtomicBoolean[length];
        for (int i = 0; i < mFetchRunning.length; i++) {
            mAlbums.set(i, new Album(null));
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
     * Starts fetching next page of album, if it exists and this album isn't currently loading.
     * Callers should subscribe to AlbumLoadedEvent through EventBus to receive results.
     *
     * @param albumType Album type
     */
    @UiThread
    public void fetchNextPage(@AlbumType int albumType) {
        if (mAlbums.get(albumType).getNextPage() != null
                && mFetchRunning[albumType].compareAndSet(false, true)) {
            new FetchAlbumThread(albumType, mAlbums.get(albumType)).start();
        }
    }

    /**
     * Provides single instance of OkHttpClient (as recommended by its authors), initialized on the
     * first invocation.
     *
     * @return Instance of OkHttpClient
     * @see <a href="https://plus.google.com/118239425803358296962/posts/5nzAvPaitHu">Answer by
     * Jake Wharton</a>
     */
    @WorkerThread
    public OkHttpClient getOkHttpClient() {
        return OkHttpClientHolder.OK_HTTP_CLIENT;
    }

    private static class RepositoryHolder {
        private final static Repository INSTANCE = new Repository();
    }

    private static class OkHttpClientHolder {
        private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    }

    private class FetchAlbumThread extends Thread {
        private static final String TAG = "FetchAlbumThread";
        private int mAlbumType;
        private Album mOldAlbum;

        public FetchAlbumThread(@AlbumType int albumType, @Nullable Album oldAlbum) {
            mAlbumType = albumType;
            mOldAlbum = oldAlbum;
        }

        @Override
        public void run() {
            Album album = new YaDownloader().fetchAlbum(mAlbumType, mOldAlbum);
            mAlbums.set(mAlbumType, album);
            mFetchRunning[mAlbumType].set(false);
            EventBus.getDefault().post(new AlbumLoadedEvent(mAlbumType, album));
        }
    }
}
