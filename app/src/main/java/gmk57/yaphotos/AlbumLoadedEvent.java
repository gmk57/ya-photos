package gmk57.yaphotos;

import gmk57.yaphotos.Repository.AlbumType;

public class AlbumLoadedEvent {
    private int mAlbumType;
    private Album mAlbum;

    public AlbumLoadedEvent(@AlbumType int albumType, Album album) {
        mAlbumType = albumType;
        mAlbum = album;
    }

    public int getAlbumType() {
        return mAlbumType;
    }

    public Album getAlbum() {
        return mAlbum;
    }
}
