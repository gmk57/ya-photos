package gmk57.yaphotos;

import com.google.gson.annotations.JsonAdapter;

import java.util.ArrayList;
import java.util.List;

@JsonAdapter(AlbumJsonAdapter.class)
public class Album {
    private List<Photo> mPhotos;
    private String mNextOffset;
    private String mLastPodDate;

    public Album() {
        mPhotos = new ArrayList<>();
    }

    public String getNextOffset() {
        return mNextOffset;
    }

    public void setNextOffset(String nextOffset) {
        mNextOffset = nextOffset;
    }

    public String getLastPodDate() {
        return mLastPodDate;
    }

    public void setLastPodDate(String lastPodDate) {
        mLastPodDate = lastPodDate;
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

    public void appendToOldAlbum(Album oldAlbum) {
        List<Photo> photos = new ArrayList<>(oldAlbum.mPhotos);
        photos.addAll(mPhotos);
        mPhotos = photos;
    }
}
