package gmk57.yaphotos;

import android.content.Context;
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

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Fragment to display full-screen photo
 */
public class PhotoFragment extends BaseFragment implements Callback {
    private static final String TAG = "PhotoFragment";
    private static final String ARG_PHOTO = "photo";

    private Callbacks mCallbacks;
    private ImageView mImageView;
    private Photo mPhoto;

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
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbacks.onClick();
            }
        });

        setupProgressState(STATE_LOADING);
        Picasso.with(getActivity())
                .load(mPhoto.getImageUrl())
                .into(mImageView, this);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo, menu);

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
                shareImage();
                return true;
            case R.id.menu_item_webpage:
                startActivity(new Intent(Intent.ACTION_VIEW, mPhoto.getPageUri()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
    public void onSuccess() {
        setupProgressState(STATE_OK);
    }

    @Override
    public void onError() {
        setupProgressState(STATE_ERROR);
    }

    @Override
    protected void tryAgain() {
        Picasso.with(getActivity())
                .load(mPhoto.getImageUrl())
                .into(mImageView, this);
    }

    /**
     * Loads image again (hopefully from Picasso memory cache), saves it to cache dir, builds and
     * fires implicit intent (with chooser). File is shared through FileProvider.
     * // TODO: File operations on background thread
     * // TODO: Delete old files
     * // TODO: Or switch to Glide and share from its cache?
     */
    private void shareImage() {
        Picasso.with(getActivity()).load(mPhoto.getImageUrl()).into(new Target() {
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
                            .setSubject(mPhoto.getTitle())
                            .setText(mPhoto.getTitle())
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

    /**
     * Required interface for hosting activities
     */
    public interface Callbacks {
        void onClick();
    }
}
