package gmk57.yaphotos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.parceler.Parcels;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import gmk57.yaphotos.data.Photo;

/**
 * Fragment to display a full-screen photo. Should be constructed through <code>newInstance</code>
 * factory method.
 */
public class PhotoFragment extends BaseFragment implements RequestListener<Drawable> {
    private static final String TAG = "PhotoFragment";
    private static final String ARG_PHOTO = "photo";

    private boolean mIsImageLoaded;
    private int mLoadFailedCount;
    private Callbacks mCallbacks;
    private ImageView mImageView;
    private Photo mPhoto;
    private ShareTask mShareTask;

    public static PhotoFragment newInstance(Photo photo) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_PHOTO, Parcels.wrap(photo));

        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhoto = Parcels.unwrap(getArguments().getParcelable(ARG_PHOTO));
        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        mImageView = view.findViewById(R.id.fullscreen_image_view);
        mImageView.setOnClickListener(v -> mCallbacks.onClick());

        setupProgressState(STATE_LOADING);
        loadImage();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo, menu);
        MenuItem shareMenuItem = menu.findItem(R.id.menu_item_share);
        shareMenuItem.setVisible(mIsImageLoaded);  // We can't share image before it's loaded

        // This fragment is currently selected, so we can set ActionBar subtitle
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        String subtitle = getString(R.string.subtitle_template, mPhoto.getTitle(),
                mPhoto.getAuthor());
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                startShareTask();
                return true;
            case R.id.menu_item_webpage:
                startActivity(new Intent(Intent.ACTION_VIEW, mPhoto.getPageUri()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startShareTask() {
        if (mShareTask == null || mShareTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            mShareTask = new ShareTask(getActivity(), mPhoto);
            mShareTask.execute();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onStop() {
        if (mShareTask != null) {
            mShareTask.cancel(false);
        }
        super.onStop();
    }

    @Override
    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                   DataSource dataSource, boolean isFirstResource) {
        setupProgressState(STATE_OK);
        if (mPhoto.getImageUrl().equals(model)) { // Full-size image loaded, we can show share button
            mIsImageLoaded = true;
            getActivity().invalidateOptionsMenu();
        }
        return false;
    }

    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target,
                                boolean isFirstResource) {
        if (++mLoadFailedCount == 2) {  // Show error only if thumbnail and full image both failed
            setupProgressState(STATE_ERROR);
        }
        return false;
    }

    @Override
    protected void tryAgain() {
        mLoadFailedCount = 0;
        loadImage();
    }

    /**
     * Loads image into ImageView. While loading full-size image, thumbnail is displayed to sweeten
     * the waiting time: it loads much faster and in many cases is already available in Glide memory
     * cache, thanks to being displayed in AlbumFragment.
     */
    private void loadImage() {
        RequestBuilder<Drawable> thumbnailRequest = GlideApp.with(this)
                .load(mPhoto.getThumbnailUrl())
                .listener(this);

        GlideApp.with(this)
                .load(mPhoto.getImageUrl())
                .override(Target.SIZE_ORIGINAL)
                .dontTransform()
                .thumbnail(thumbnailRequest)
                .listener(this)
                .into(mImageView);
    }

    /**
     * Required interface for hosting activities
     */
    public interface Callbacks {
        void onClick();
    }

    /**
     * Loads image again (hopefully from Glide disk cache) and shares it directly from cache through
     * FileProvider.
     * <p>
     * Code for dealing with Glide cache is based on
     * <a href="https://github.com/bumptech/glide/issues/459#issuecomment-99960446">
     * this code samples by TWiStErRob</a>
     */
    private static class ShareTask extends AsyncTask<Void, Void, Intent> {
        private static final String TAG = "ShareTask";
        private Activity mActivity;
        private Photo mPhoto;

        public ShareTask(Activity activity, Photo photo) {
            mActivity = activity;
            mPhoto = photo;
        }

        @Override
        protected Intent doInBackground(Void... params) {
            try {
                File file = GlideApp.with(mActivity)
                        .downloadOnly()
                        .load(mPhoto.getImageUrl())
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();

                if (isCancelled()) {  // Typically it's too fast to be cancelled, but just in case
                    return null;
                }
                Uri uri = FileProvider.getUriForFile(mActivity, "gmk57.yaphotos.fileprovider", file);
                return createIntent(uri);

            } catch (InterruptedException | ExecutionException e) {
                return null;  // We'll show toast in onPostExecute
            }
        }

        private Intent createIntent(Uri uri) {
            Intent intent = ShareCompat.IntentBuilder.from(mActivity)
                    .setStream(uri)
                    .setSubject(mPhoto.getTitle())
                    .setText(mPhoto.getTitle())
                    .setType("image/jpeg")
                    .getIntent();  // createChooserIntent() breaks workaround below for API < 16

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // Temporary permissions don't work prior to API 16, and recommended way
                // via intent.setData() breaks most target apps. See:
                // https://medium.com/@ashughes/after-further-investigation-it-seems-that-it-is-not-correct-to-call-setdata-cfd5361186ce
                List<ResolveInfo> resolveInfoList = mActivity.getPackageManager()
                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resolveInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    mActivity.grantUriPermission(packageName, uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            return intent;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            if (intent == null) {
                Toast.makeText(mActivity, R.string.sharing_failed, Toast.LENGTH_LONG).show();
                return;
            }

            mActivity.startActivity(Intent.createChooser(intent, mActivity.getResources()
                    .getString(R.string.share_chooser_title)));
        }
    }
}
