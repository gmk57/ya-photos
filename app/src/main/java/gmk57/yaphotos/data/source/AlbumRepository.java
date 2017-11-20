package gmk57.yaphotos.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.inject.Inject;
import javax.inject.Singleton;

import gmk57.yaphotos.AlbumLoadedEvent;
import gmk57.yaphotos.data.Album;
import gmk57.yaphotos.data.AlbumType;
import gmk57.yaphotos.data.Photo;

/**
 * Singleton class, responsible for holding model objects and retrieving them from database/network
 */
@Singleton
public class AlbumRepository {
    public static final String[] ALBUM_PATHS = {"recent", "top", "podhistory"};
    private static final String TAG = "AlbumRepository";
    private final AtomicReferenceArray<Album> mAlbums;
    private final AtomicBoolean[] mFetchRunning;
    private final LocalSource mLocalSource;
    private final NetworkSource mNetworkSource;
    private final EventBus mEventBus;

    @Inject
    AlbumRepository(LocalSource localSource, NetworkSource networkSource, EventBus eventBus) {
        int length = ALBUM_PATHS.length;
        mAlbums = new AtomicReferenceArray<>(length);
        mFetchRunning = new AtomicBoolean[length];
        for (int i = 0; i < length; i++) {
            mAlbums.set(i, new Album());
            mFetchRunning[i] = new AtomicBoolean();
        }
        mLocalSource = localSource;
        mNetworkSource = networkSource;
        mEventBus = eventBus;
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
        if (album.getSize() == 0 && mFetchRunning[albumType].compareAndSet(false, true)) {
            new FetchAlbumThread(albumType, null, false).start();
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
            new FetchAlbumThread(albumType, null, true).start();
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
            new FetchAlbumThread(albumType, mAlbums.get(albumType), false).start();
        }
    }

    /**
     * Background thread to fetch album from database and/or network.
     */
    private class FetchAlbumThread extends Thread {
        private static final String TAG = "FetchAlbumThread";
        private int mAlbumType;
        private Album mOldAlbum;
        private boolean mForceNetwork;

        /**
         * Fetches Album from database, if possible. Otherwise, fetches from network, if:
         * 1) database is empty/missing or
         * 2) fetching next page is requested (old album provided) or
         * 3) force reload is requested
         * <p>
         * If old album is provided, new album will be built on top of it, appending photos of its
         * next page. Otherwise, album will be built from scratch, according to provided album type.
         * <p>
         * After successful fetch model in memory is updated. If loaded from network, database is
         * updated too. In case of fetch failure, model in memory and database remains untouched.
         * <p>
         * In any case subscribers are notified through EventBus that load is finished.
         *
         * @param albumType    Valid values = ALBUM_PATHS indexes
         * @param oldAlbum     Old album (to append) or null (to create from scratch)
         * @param forceNetwork Fetch only from network and delete old DB entries. Not used if old
         *                     album is not null
         */
        public FetchAlbumThread(@AlbumType int albumType, @Nullable Album oldAlbum,
                                boolean forceNetwork) {
            mAlbumType = albumType;
            mOldAlbum = oldAlbum;
            mForceNetwork = forceNetwork;
        }

        @Override
        public void run() {
            Album album = null;
            if (mOldAlbum == null && !mForceNetwork) {  // Cold start, try fetching DB first
                album = mLocalSource.fetchAlbum(mAlbumType);
            }

            List<Photo> newPhotos = null;
            if (album == null) { // Fetching next page or mForceNetwork or DB empty => go to network
                String offset = (mOldAlbum == null) ? "" : mOldAlbum.getNextOffset();
                album = mNetworkSource.fetchAlbum(mAlbumType, offset);

                if (album != null) {
                    newPhotos = album.getPhotos();
                }
            }

            if (album != null) {  // Load successful (otherwise we're just keeping old album)
                if (mOldAlbum != null) {
                    album.appendToOldAlbum(mOldAlbum);
                }
                mAlbums.set(mAlbumType, album);
            }

            // In any case we should notify subscribers that load is finished
            mEventBus.post(new AlbumLoadedEvent(mAlbumType));
            mFetchRunning[mAlbumType].set(false);

            if (newPhotos != null) {  // We have something new to save in DB
                saveUpdatesToDb(album, newPhotos);
            }
        }

        private void saveUpdatesToDb(Album album, List<Photo> newPhotos) {
            if (mOldAlbum == null && mForceNetwork) { // Forced reload, clean up DB before inserting
                mLocalSource.clearPhotos(mAlbumType);
            }

            mLocalSource.saveAlbum(album, mAlbumType);
            mLocalSource.savePhotos(newPhotos, mAlbumType);
        }
    }
}
