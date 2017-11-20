package gmk57.yaphotos.data.source;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import gmk57.yaphotos.data.Album;
import gmk57.yaphotos.data.AlbumDao;
import gmk57.yaphotos.data.AlbumType;
import gmk57.yaphotos.data.DaoSession;
import gmk57.yaphotos.data.Photo;
import gmk57.yaphotos.data.PhotoDao;

/**
 * Wrapper around greenDAO session. All methods are synchronous and should be called from background
 * thread. DaoSession is lazily initialized on first invocation.
 */
@Singleton
public class LocalSource {
    private final Lazy<DaoSession> mDaoSessionLazy;

    @Inject
    LocalSource(Lazy<DaoSession> daoSessionLazy) {
        mDaoSessionLazy = daoSessionLazy;
    }

    /**
     * Fetch album (with photos) from DB
     *
     * @param albumType Album type
     * @return Album if succeeded, null if failed
     */
    @Nullable
    @WorkerThread
    Album fetchAlbum(@AlbumType long albumType) {
        AlbumDao albumDao = mDaoSessionLazy.get().getAlbumDao();
        Album album = albumDao.load(albumType);
        if (album != null) {
            album.getPhotos();  // Otherwise photos are loaded lazily (== in UI thread)
        }
        return album;
    }

    /**
     * Clears from DB all photos of specified album type
     *
     * @param albumType Album type
     */
    @WorkerThread
    void clearPhotos(@AlbumType long albumType) {
        PhotoDao photoDao = mDaoSessionLazy.get().getPhotoDao();
        List<Photo> oldPhotos = photoDao.queryBuilder()
                .where(PhotoDao.Properties.AlbumType.eq(albumType)).list();
        photoDao.deleteInTx(oldPhotos);
    }

    /**
     * Saves album (without photos) to DB. Provided album type (primary key) is saved inside album
     * to allow retrieving it later by that key.
     *
     * @param album     Album to save
     * @param albumType Its album type
     */
    @WorkerThread
    void saveAlbum(Album album, @AlbumType long albumType) {
        album.setType(albumType);

        AlbumDao albumDao = mDaoSessionLazy.get().getAlbumDao();
        albumDao.insertOrReplace(album);
    }

    /**
     * Saves photos to DB. Provided album type (foreign key) is saved inside photos to allow
     * retrieving them later by that key. Primary key is auto-incremented, so saving photos without
     * preliminary call to {@link #clearPhotos} effectively appends them to the end of the album.
     *
     * @param photos    List of photos to save
     * @param albumType Their album type
     */
    @WorkerThread
    void savePhotos(List<Photo> photos, @AlbumType long albumType) {
        for (Photo photo : photos) {
            photo.setAlbumType(albumType);
        }

        PhotoDao photoDao = mDaoSessionLazy.get().getPhotoDao();
        photoDao.insertOrReplaceInTx(photos);
    }
}
