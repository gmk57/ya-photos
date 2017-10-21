package gmk57.yaphotos;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Main app screen with ViewPager of AlbumFragments.
 */
public class AlbumActivity extends AppCompatActivity {
    private static final String TAG = "AlbumActivity";
    private static final String URL_ASSET_ABOUT = "file:///android_asset/about.htm";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new AlbumPagerAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(2);  // Keep all 3 fragments

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_album, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_about:
                startActivity(WebViewActivity.newIntent(this, URL_ASSET_ABOUT));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class AlbumPagerAdapter extends FragmentPagerAdapter {
        public AlbumPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return AlbumFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return Repository.ALBUM_PATHS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.album_names)[position];
        }
    }
}
