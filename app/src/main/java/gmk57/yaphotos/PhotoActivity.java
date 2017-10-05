package gmk57.yaphotos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.parceler.Parcels;

public class PhotoActivity extends AppCompatActivity implements PhotoFragment.Callbacks {
    private static final String TAG = "PhotoActivity";
    private static final String EXTRA_ALBUM = "gmk57.yaphotos.album";
    private static final String EXTRA_POSITION = "gmk57.yaphotos.position";
    private static final String KEY_UI_VISIBLE = "uiVisible";

    private boolean mUiVisible = true;
    private Album mAlbum;
    private ViewPager mViewPager;

    /**
     * Creates Intent to start this Activity with necessary arguments.
     *
     * @param context  Context to build Intent
     * @param album    Current album
     * @param position Current position
     * @return Intent to start this activity
     */
    public static Intent newIntent(Context context, Album album, int position) {
        Intent intent = new Intent(context, PhotoActivity.class);
        intent.putExtra(EXTRA_ALBUM, Parcels.wrap(album));
        intent.putExtra(EXTRA_POSITION, position);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);

        mAlbum = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_ALBUM));
        int position = getIntent().getIntExtra(EXTRA_POSITION, 0);
        if (savedInstanceState != null) {
            mUiVisible = savedInstanceState.getBoolean(KEY_UI_VISIBLE, true);
        }

        setContentView(R.layout.viewpager);
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(new PhotoPagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(position);
        mViewPager.setOffscreenPageLimit(3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupUiVisibility(mUiVisible); // Reset flags to persist if the user navigates out and back in
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_UI_VISIBLE, mUiVisible);
    }

    /**
     * Hides or shows system UI and ActionBar. Status bar is completely hidden (on API >= 16)
     * and navigation bar is dimmed. Visibility state is saved into class field for persistence.
     *
     * @param uiVisible Should UI be visible?
     */
    private void setupUiVisibility(boolean uiVisible) {
        mUiVisible = uiVisible;
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (!uiVisible) {
            getSupportActionBar().hide();
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        } else {
            getSupportActionBar().show();
        }
        mViewPager.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onClick() {
        setupUiVisibility(!mUiVisible);
    }

    private class PhotoPagerAdapter extends FragmentStatePagerAdapter {
        public PhotoPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return PhotoFragment.newInstance(mAlbum.getPhoto(position));
        }

        @Override
        public int getCount() {
            return mAlbum.getSize();
        }
    }
}
