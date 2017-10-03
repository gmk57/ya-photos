package gmk57.yaphotos;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Parcel
public class Album {
    List<Photo> mPhotos;
    String mNextPage;
    int mOldSize;

    /**
     * Creates new Album
     *
     * @param oldAlbum If provided, new album will contain all photos of old album
     */
    public Album(Album oldAlbum) {
        if (oldAlbum == null) {
            mPhotos = new ArrayList<>();
        } else {
            mPhotos = new ArrayList<>(oldAlbum.mPhotos);
            mOldSize = oldAlbum.getSize();
        }
    }

    public Album() {
    }

    public String getNextPage() {
        return mNextPage;
    }

    public void setNextPage(String nextPage) {
        mNextPage = nextPage;
    }

    public void addPhoto(Photo photo) {
        mPhotos.add(photo);
    }

    public Photo getPhoto(int position) {
        return mPhotos.get(position);
    }

    public int getSize() {
        return mPhotos.size();
    }

    public int getOldSize() {
        return mOldSize;
    }
}
