package gmk57.yaphotos;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Fragment to display full-screen photo
 */
public class PhotoFragment extends BaseFragment implements Callback {
    private static final String TAG = "PhotoFragment";
    private static final String EXTRA_IMAGE_URL = "gmk57.yaphotos.photoImageUrl";
    private static final String EXTRA_TITLE = "gmk57.yaphotos.photoTitle";
    private static final String KEY_UI_VISIBLE = "uiVisible";

    private boolean mUiVisible = true;
    private ImageView mImageView;
    private String mPhotoImageUrl;
    private String mPhotoTitle;


    public static PhotoFragment newInstance(Bundle args) {
        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates arguments for hosting activity to use (for example, in its
     * <code>newIntent</code> method)
     *
     * @param photoImageUrl Url to load image from
     * @param photoTitle    Title to display in ActionBar
     * @return Bundle of arguments
     */
    public static Bundle createArguments(String photoImageUrl, String photoTitle) {
        Bundle args = new Bundle();
        args.putString(EXTRA_IMAGE_URL, photoImageUrl);
        args.putString(EXTRA_TITLE, photoTitle);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mUiVisible = savedInstanceState.getBoolean(KEY_UI_VISIBLE, true);
        }
        mPhotoImageUrl = getArguments().getString(EXTRA_IMAGE_URL);
        mPhotoTitle = getArguments().getString(EXTRA_TITLE);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(mPhotoTitle);
        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        mImageView = view.findViewById(R.id.fullscreen_image_view);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiVisible = !mUiVisible;
                setupUiVisibility();
            }
        });

        setupProgressState(STATE_LOADING);
        Picasso.with(getActivity()).load(mPhotoImageUrl).into(mImageView, this);

        return view;
    }

    @Override
    public void onSuccess() {
        setupProgressState(STATE_OK);
    }

    @Override
    public void onError() {
        setupProgressState(STATE_ERROR);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                shareImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUiVisibility(); // Reset flags to persist if the user navigates out and back in
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_UI_VISIBLE, mUiVisible);
    }

    @Override
    protected void tryAgain() {
        Picasso.with(getActivity())
                .load(mPhotoImageUrl)
                .into(mImageView, this);
    }

    /**
     * Hides or shows system UI and ActionBar according to current
     * <code>mUiVisible</code> value. Status bar is completely hidden
     * (on API >= 16) and navigation bar is dimmed.
     */
    private void setupUiVisibility() {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (!mUiVisible) {
            activity.getSupportActionBar().hide();
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        } else {
            activity.getSupportActionBar().show();
        }
        mImageView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Loads image again (hopefully from Picasso memory cache), saves it to cache dir, builds and
     * fires implicit intent (with chooser). File is shared through FileProvider.
     * // TODO: File operations on background thread
     * // TODO: Delete old files
     */
    private void shareImage() {
        Picasso.with(getActivity()).load(mPhotoImageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    File shareDir = new File(getContext().getCacheDir(), "my_share");
                    shareDir.mkdirs();
                    File imageFile = File.createTempFile("photo", ".jpg", shareDir);
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                    outputStream.close();

                    Uri imageUri = FileProvider.getUriForFile(getActivity(),
                            "gmk57.yaphotos.fileprovider", imageFile);
                    Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                            .setStream(imageUri)
                            .setSubject(mPhotoTitle)
                            .setText(mPhotoTitle)
                            .setType("image/jpeg")
                            .getIntent();

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        // Temporary permissions don't work prior to API 16, and recommended way
                        // via intent.setData() breaks most target apps.
                        // See https://medium.com/@ashughes/after-further-investigation-it-seems-that-it-is-not-correct-to-call-setdata-cfd5361186ce
                        List<ResolveInfo> resolveInfoList = getActivity().getPackageManager()
                                .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resolveInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            getActivity().grantUriPermission(packageName, imageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }

                    // ShareCompat.IntentBuilder.createChooserIntent() looks nice, but breaks
                    // above workaround for API < 16.
                    startActivity(Intent.createChooser(shareIntent, getResources()
                            .getString(R.string.share_chooser_title)));

                } catch (IOException | SecurityException e) {
                    Log.e(TAG, "Can't save file for sharing: " + e);
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.e(TAG, "Can't load file for sharing");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {/* not needed */}
        });
    }
}
