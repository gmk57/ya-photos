package gmk57.yaphotos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity to display WebViewFragment. Should be started with intent constructed through
 * <code>newIntent</code> factory method.
 */
public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    private static final String EXTRA_URL = "gmk57.yaphotos.url";

    /**
     * Creates Intent to start this Activity with necessary arguments.
     *
     * @param context Context to build Intent
     * @param url     Valid url (local or remote)
     * @return Intent to start this activity
     */
    public static Intent newIntent(Context context, String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            String url = getIntent().getStringExtra(EXTRA_URL);
            Fragment fragment = WebViewFragment.newInstance(url);
            fm.beginTransaction()
                    .add(android.R.id.content, fragment).commit();
        }
    }
}
