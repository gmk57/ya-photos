package gmk57.yaphotos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PhotoActivity extends BaseActivity {

    /**
     * Creates Intent to start this Activity with necessary arguments.
     *
     * @param context       Context to build Intent
     * @param photoImageUrl Url to load image from
     * @param photoTitle    Title to display in ActionBar
     * @return Intent to start this Activity
     */
    public static Intent newIntent(Context context, String photoImageUrl, String photoTitle) {
        Intent intent = new Intent(context, PhotoActivity.class);
        Bundle args = PhotoFragment.createArguments(photoImageUrl, photoTitle);
        intent.putExtras(args);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoFragment.newInstance(getIntent().getExtras());
    }
}
