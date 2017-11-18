package gmk57.yaphotos.data.source;

import gmk57.yaphotos.data.Album;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit interface for accessing server API.
 */
public interface NetworkSource {
    @GET("{albumPath}/{offset}?format=json")
    Call<Album> downloadAlbum(@Path("albumPath") String albumPath,
                              @Path("offset") String offset);
}
