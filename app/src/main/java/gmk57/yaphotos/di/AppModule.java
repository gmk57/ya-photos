package gmk57.yaphotos.di;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.database.Database;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import gmk57.yaphotos.data.DaoMaster;
import gmk57.yaphotos.data.DaoSession;
import gmk57.yaphotos.data.source.NetworkSource;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

    /**
     * Provides single instance of database connector. Single instance is required for reliable
     * multi-thread access to database. Using application-scope DaoSession is
     * <a href="http://greenrobot.org/greendao/documentation/how-to-get-started/#comment-45">
     * recommended by author of greenDAO.</a>
     */
    @Provides
    @Singleton
    public DaoSession provideDaoSession(Context context) {
        DaoMaster.OpenHelper openHelper = new DaoMaster.DevOpenHelper(context, "albums-db");
        Database database = openHelper.getWritableDb();
        return new DaoMaster(database).newSession();
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    public NetworkSource provideNetworkSource() {
        return new Retrofit.Builder()
                .baseUrl("http://api-fotki.yandex.ru/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NetworkSource.class);
    }
}
