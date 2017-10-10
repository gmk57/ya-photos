package gmk57.yaphotos;

import java.util.ArrayList;
import java.util.List;

public class Album {
    private List<Photo> mPhotos;
    private String mNextPage;

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
        }
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
}
