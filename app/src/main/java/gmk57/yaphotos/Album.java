package gmk57.yaphotos;

import com.google.gson.annotations.JsonAdapter;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.OrderBy;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
@JsonAdapter(AlbumJsonAdapter.class)
public class Album {
    @Id
    private Long type;
    @ToMany(referencedJoinProperty = "albumType")
    @OrderBy("id ASC")
    private List<Photo> photos;
    private String nextOffset;
    private String lastPodDate;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 172302968)
    private transient AlbumDao myDao;

    public Album() {
        photos = new ArrayList<>();
    }

    @Generated(hash = 1664509784)
    public Album(Long type, String nextOffset, String lastPodDate) {
        this.type = type;
        this.nextOffset = nextOffset;
        this.lastPodDate = lastPodDate;
    }

    public String getNextOffset() {
        return nextOffset;
    }

    public void setNextOffset(String nextOffset) {
        this.nextOffset = nextOffset;
    }

    public String getLastPodDate() {
        return lastPodDate;
    }

    public void setLastPodDate(String lastPodDate) {
        this.lastPodDate = lastPodDate;
    }

    public void addPhoto(Photo photo) {
        photos.add(photo);
    }

    public Photo getPhoto(int position) {
        return photos.get(position);
    }

    public int getSize() {
        return getPhotos().size();
    }

    public void appendToOldAlbum(Album oldAlbum) {
        List<Photo> photos = new ArrayList<>(oldAlbum.photos);
        photos.addAll(this.photos);
        this.photos = photos;
    }

    public Long getType() {
        return this.type;
    }

    public void setType(Long type) {
        this.type = type;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 175879697)
    public List<Photo> getPhotos() {
        if (photos == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            PhotoDao targetDao = daoSession.getPhotoDao();
            List<Photo> photosNew = targetDao._queryAlbum_Photos(type);
            synchronized (this) {
                if (photos == null) {
                    photos = photosNew;
                }
            }
        }
        return photos;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 781103891)
    public synchronized void resetPhotos() {
        photos = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1023911229)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getAlbumDao() : null;
    }
}
