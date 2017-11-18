package gmk57.yaphotos;

import android.app.Application;

import gmk57.yaphotos.di.AppComponent;
import gmk57.yaphotos.di.AppModule;
import gmk57.yaphotos.di.DaggerAppComponent;

public class App extends Application {
    private static AppComponent sAppComponent;

    public static AppComponent getComponent() {
        return sAppComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }
}
