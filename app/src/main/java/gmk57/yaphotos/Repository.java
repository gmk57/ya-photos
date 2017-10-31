package gmk57.yaphotos;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.database.Database;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import gmk57.yaphotos.DaoMaster.DevOpenHelper;
import gmk57.yaphotos.DaoMaster.OpenHelper;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Singleton class, responsible for holding model objects and retrieving them from database/network
 */
public class Repository {
    public static final String[] ALBUM_PATHS = {"recent", "top", "podhistory"};
    private static final String TAG = "Repository";
    private static Repository sInstance;
    private final AtomicReferenceArray<Album> mAlbums;
    private final AtomicBoolean[] mFetchRunning;
    private final Context mContext;

    private Repository(Context context) {
        int length = ALBUM_PATHS.length;
        mAlbums = new AtomicReferenceArray<>(length);
        mFetchRunning = new AtomicBoolean[length];
        for (int i = 0; i < length; i++) {
            mAlbums.set(i, new Album());
            mFetchRunning[i] = new AtomicBoolean();
        }
        mContext = context;
    }

    /**
     * Provides single instance of this class. Is NOT currently thread-safe.
     *
     * @param context Required for database operations. Any context passed will be converted to
     *                application context to prevent memory leaks
     * @return Single instance of this class.
     */
    public static Repository getInstance(Context context) {
        if (sInstance == null)
            sInstance = new Repository(context.getApplicationContext());
        return sInstance;
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
     * Provides single instance of database layer (with lazy and thread-safe initialization).
     * Single instance is required for reliable multi-thread access to database.
     * <p>
     * Using application-scope DaoSession is
     * <a href="http://greenrobot.org/greendao/documentation/how-to-get-started/#comment-45">
     * recommended by author of greenDAO.</a>
     */
    private static class DatabaseHolder {
        static DaoSession sDaoSession;

        @WorkerThread
        static synchronized DaoSession getDaoSession(Context context) {
            if (sDaoSession == null) {
                OpenHelper openHelper = new DevOpenHelper(context, "albums-db");
                Database database = openHelper.getWritableDb();
                sDaoSession = new DaoMaster(database).newSession();
            }
            return sDaoSession;
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
                album = fetchAlbumFromDb();
            }

            List<Photo> newPhotos = null;
            if (album == null) { // Fetching next page or mForceNetwork or DB empty => go to network
                album = fetchAlbumFromNetwork();
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
            EventBus.getDefault().post(new AlbumLoadedEvent(mAlbumType));
            mFetchRunning[mAlbumType].set(false);

            if (newPhotos != null) {  // We have something new to save in DB
                saveUpdatesToDb(album, newPhotos);
            }
        }

        @Nullable
        private Album fetchAlbumFromDb() {
            AlbumDao albumDao = DatabaseHolder.getDaoSession(mContext).getAlbumDao();
            Album album = albumDao.load((long) mAlbumType);
            if (album != null) {
                album.getPhotos();  // Otherwise photos are loaded lazily (== in UI thread)
            }
            return album;
        }

        @Nullable
        private Album fetchAlbumFromNetwork() {
            String albumPath = ALBUM_PATHS[mAlbumType];
            String offset = (mOldAlbum == null) ? "" : mOldAlbum.getNextOffset();
            NetworkLayer networkLayer = getNetworkLayer();

            Album album = null;
            try {
                Response<Album> response = networkLayer.downloadAlbum(albumPath, offset).execute();

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

        private void saveUpdatesToDb(Album album, List<Photo> newPhotos) {
            // Set key values for DB (not used by UI components, so synchronization is not needed)
            album.setType((long) mAlbumType);
            for (Photo photo : newPhotos) {
                photo.setAlbumType((long) mAlbumType);
            }

            DaoSession daoSession = DatabaseHolder.getDaoSession(mContext);
            AlbumDao albumDao = daoSession.getAlbumDao();
            PhotoDao photoDao = daoSession.getPhotoDao();

            if (mOldAlbum == null && mForceNetwork) { // Forced reload, clean up DB before inserting
                List<Photo> oldPhotos = photoDao.queryBuilder()
                        .where(PhotoDao.Properties.AlbumType.eq(mAlbumType)).list();
                photoDao.deleteInTx(oldPhotos);
            }

            albumDao.insertOrReplace(album);
            photoDao.insertOrReplaceInTx(newPhotos);
        }
    }
}
