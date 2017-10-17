package gmk57.yaphotos;

import android.net.Uri;

import com.google.gson.annotations.JsonAdapter;

import org.parceler.Parcel;

@SuppressWarnings("WeakerAccess")
@Parcel
@JsonAdapter(YaDownloader.PhotoDeserializer.class)
public class Photo {
    String mTitle;
    String mAuthor;
    String mThumbnailUrl;
    String mImageUrl;
    String mPageUrl;
    String mPodDate;

    @Override
    public String toString() {
        return mTitle;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        mThumbnailUrl = thumbnailUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public String getPageUrl() {
        return mPageUrl;
    }

    public void setPageUrl(String pageUrl) {
        mPageUrl = pageUrl;
    }

    public Uri getPageUri() {
        return Uri.parse(mPageUrl);
    }

    public String getPodDate() {
        return mPodDate;
    }

    public void setPodDate(String podDate) {
        mPodDate = podDate;
    }
}
