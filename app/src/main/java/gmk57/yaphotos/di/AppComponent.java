package gmk57.yaphotos.di;

import javax.inject.Singleton;

import dagger.Component;
import gmk57.yaphotos.AlbumFragment;
import gmk57.yaphotos.PhotoActivity;

@Component(modules = AppModule.class)
@Singleton
public interface AppComponent {
    void inject(AlbumFragment albumFragment);

    void inject(PhotoActivity photoActivity);
}
