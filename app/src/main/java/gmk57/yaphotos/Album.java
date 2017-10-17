package gmk57.yaphotos;

import com.google.gson.annotations.JsonAdapter;

import java.util.ArrayList;
import java.util.List;

@JsonAdapter(YaDownloader.AlbumDeserializer.class)
public class Album {
    private List<Photo> mPhotos;
    private String mNextPage;

    public Album() {
        mPhotos = new ArrayList<>();
    }

    public void setPhotos(List<Photo> photos) {
        mPhotos = photos;
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

    public void appendToOldAlbum(Album oldAlbum) {
        List<Photo> photos = new ArrayList<>(oldAlbum.mPhotos);
        photos.addAll(mPhotos);
        mPhotos = photos;
    }
}
