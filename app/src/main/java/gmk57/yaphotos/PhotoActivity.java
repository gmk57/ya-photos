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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import gmk57.yaphotos.Repository.AlbumType;

/**
 * Detail screen with ViewPager of PhotoFragments. Should be started with intent constructed through
 * <code>newIntent</code> factory method.
 */
public class PhotoActivity extends AppCompatActivity implements PhotoFragment.Callbacks {
    private static final String TAG = "PhotoActivity";
    private static final String EXTRA_ALBUM_TYPE = "gmk57.yaphotos.albumType";
    private static final String EXTRA_POSITION = "gmk57.yaphotos.position";
    private static final String KEY_UI_VISIBLE = "uiVisible";

    private boolean mUiVisible = true;
    private int mAlbumType;
    private Album mAlbum;
    private ViewPager mViewPager;

    /**
     * Creates Intent to start this Activity with necessary arguments.
     *
     * @param context   Context to build Intent
     * @param albumType Current album type
     * @param position  Current position
     * @return Intent to start this activity
     */
    public static Intent newIntent(Context context, @AlbumType int albumType, int position) {
        Intent intent = new Intent(context, PhotoActivity.class);
        intent.putExtra(EXTRA_ALBUM_TYPE, albumType);
        intent.putExtra(EXTRA_POSITION, position);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);

        mAlbumType = getIntent().getIntExtra(EXTRA_ALBUM_TYPE, 0);
        int position = getIntent().getIntExtra(EXTRA_POSITION, 0);
        if (savedInstanceState != null) {
            mUiVisible = savedInstanceState.getBoolean(KEY_UI_VISIBLE, true);
        }

        EventBus.getDefault().register(this);
        mAlbum = Repository.getInstance(this).getAlbum(mAlbumType);

        setContentView(R.layout.viewpager);
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(new PhotoPagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(position);
        mViewPager.setOffscreenPageLimit(2);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position + 10 > mAlbum.getSize()) {
                    Repository.getInstance(PhotoActivity.this).fetchNextPage(mAlbumType);
                }
            }
        });
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

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlbumLoaded(AlbumLoadedEvent event) {
        if (event.getAlbumType() == mAlbumType) {
            mAlbum = Repository.getInstance(this).getAlbum(mAlbumType);
            mViewPager.getAdapter().notifyDataSetChanged();
        }
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
