package gmk57.yaphotos;

import android.net.Uri;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.parceler.Parcel;

@Entity
@SuppressWarnings("WeakerAccess")
@Parcel
public class Photo {
    @Id(autoincrement = true)
    Long id;
    Long albumType;
    String title;
    String author;
    String thumbnailUrl;
    String imageUrl;
    String pageUrl;

    @Generated(hash = 2000401231)
    public Photo(Long id, Long albumType, String title, String author,
                 String thumbnailUrl, String imageUrl, String pageUrl) {
        this.id = id;
        this.albumType = albumType;
        this.title = title;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrl = imageUrl;
        this.pageUrl = pageUrl;
    }

    @Generated(hash = 1043664727)
    public Photo() {
    }

    @Override
    public String toString() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public Uri getPageUri() {
        return Uri.parse(pageUrl);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlbumType() {
        return this.albumType;
    }

    public void setAlbumType(Long albumType) {
        this.albumType = albumType;
    }
}
