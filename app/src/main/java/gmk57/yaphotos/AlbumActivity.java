package gmk57.yaphotos;

import android.support.v4.app.Fragment;

public class AlbumActivity extends BaseActivity {

    @Override
    protected Fragment createFragment() {
        return new AlbumFragment();
    }
}
