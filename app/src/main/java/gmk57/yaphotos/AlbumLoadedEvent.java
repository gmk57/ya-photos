package gmk57.yaphotos;

import gmk57.yaphotos.data.AlbumType;

public class AlbumLoadedEvent {
    private int mAlbumType;

    public AlbumLoadedEvent(@AlbumType int albumType) {
        mAlbumType = albumType;
    }

    public int getAlbumType() {
        return mAlbumType;
    }
}
