package gmk57.yaphotos;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }

        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            fm.beginTransaction()
                    .add(android.R.id.content, createFragment())
                    .commit();
        }
    }
}
