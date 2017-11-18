package gmk57.yaphotos.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private Context mAppContext;

    public AppModule(Context appContext) {
        mAppContext = appContext;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return mAppContext;
    }
}
