package gmk57.yaphotos;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.ListPreloader.PreloadModelProvider;
import com.bumptech.glide.ListPreloader.PreloadSizeProvider;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.FixedPreloadSizeProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import gmk57.yaphotos.Repository.AlbumType;

/**
 * Main app fragment to display album thumbnails, with scrolling (endless, if possible). Should be
 * constructed through <code>newInstance</code> factory method.
 */
public class AlbumFragment extends BaseFragment {
    private static final String TAG = "AlbumFragment";
    private static final String ARG_ALBUM_TYPE = "albumType";

    @Inject
    Repository mRepository;
    private int mAlbumType;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mPhotoAdapter;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static AlbumFragment newInstance(@AlbumType int albumType) {
        Bundle args = new Bundle();
        args.putInt(ARG_ALBUM_TYPE, albumType);

        AlbumFragment fragment = new AlbumFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbumType = getArguments().getInt(ARG_ALBUM_TYPE);
        App.getComponent().inject(this);
    }

    @NonNull
    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        mRecyclerView = view.findViewById(R.id.album_recycler_view);

        // If possible, get album immediately to preserve scroll position
        EventBus.getDefault().register(this);
        Album album = mRepository.getAlbum(mAlbumType);
        if (album.getSize() > 0) {
            setupProgressState(STATE_OK);
        } else {
            setupProgressState(STATE_LOADING);
        }

        mPhotoAdapter = new PhotoAdapter(album);
        mRecyclerView.setAdapter(mPhotoAdapter);

        // Initial span count = 1 causes LayoutManager to lose scroll position on rotation sometimes
        mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        setupSpanCount(this);
                    }

                });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mLayoutManager.findLastVisibleItemPosition() + 40 > mLayoutManager.getItemCount()) {
                    mRepository.fetchNextPage(mAlbumType);
                }

            }
        });

        PreloadSizeProvider<Photo> sizeProvider =
                new FixedPreloadSizeProvider<>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        RecyclerViewPreloader<Photo> preloader = new RecyclerViewPreloader<>(this,
                mPhotoAdapter, sizeProvider, 24);
        mRecyclerView.addOnScrollListener(preloader);

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mRepository.reloadAlbum(mAlbumType));

        return view;
    }

    @Override
    protected void tryAgain() {
        mRepository.reloadAlbum(mAlbumType);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    /**
     * If RecyclerView is already sized, calculates and sets span count of GridLayoutManager,
     * then removes listener.
     *
     * @param listener Listener to remove
     */
    private void setupSpanCount(ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (mRecyclerView.getWidth() > 0) {
            int thumbnailSize = (int) getResources().getDimension(R.dimen.thumbnail_size);
            int spanCount = mRecyclerView.getWidth() / thumbnailSize;
            mLayoutManager.setSpanCount(spanCount);

            ViewTreeObserver viewTreeObserver = mRecyclerView.getViewTreeObserver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                viewTreeObserver.removeOnGlobalLayoutListener(listener);
            } else {
                //noinspection deprecation
                viewTreeObserver.removeGlobalOnLayoutListener(listener);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlbumLoaded(AlbumLoadedEvent event) {
        if (event.getAlbumType() == mAlbumType) {
            mSwipeRefreshLayout.setRefreshing(false);
            Album album = mRepository.getAlbum(mAlbumType);

            if (album.getSize() > 0) {
                mPhotoAdapter.setAlbum(album);
                setupProgressState(STATE_OK);
            } else {
                setupProgressState(STATE_ERROR);
            }
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mThumbnailImageView;
        private int mPosition;

        public PhotoHolder(View itemView) {
            super(itemView);
            mThumbnailImageView = itemView.findViewById(R.id.thumbnail_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindPhoto(Photo photo, int position) {
            mPosition = position;

            GlideApp.with(AlbumFragment.this)
                    .load(photo.getThumbnailUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    // Prevents image corruption and keeps memory cache working on rotation
                    .override(Target.SIZE_ORIGINAL)
                    .dontTransform()  // Allows using memory cache for preloaded images
                    .into(mThumbnailImageView);
        }

        @Override
        public void onClick(View v) {
            startActivity(PhotoActivity.newIntent(getActivity(), mAlbumType, mPosition));
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>
            implements PreloadModelProvider<Photo> {
        private static final String TAG = "PhotoAdapter";
        private Album mAlbum;

        public PhotoAdapter(Album album) {
            mAlbum = album;
        }

        public void setAlbum(Album album) {
            mAlbum = album;
            notifyDataSetChanged();
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.thumbnail, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            Photo photo = mAlbum.getPhoto(position);
            holder.bindPhoto(photo, position);
        }

        @Override
        public int getItemCount() {
            return mAlbum.getSize();
        }

        @NonNull
        @Override
        public List<Photo> getPreloadItems(int position) {
            return Collections.singletonList(mAlbum.getPhoto(position));
        }

        @Override
        public RequestBuilder getPreloadRequestBuilder(Photo photo) {
            return GlideApp.with(AlbumFragment.this)
                    .load(photo.getThumbnailUrl())
                    .override(Target.SIZE_ORIGINAL)
                    .dontTransform()
                    .priority(Priority.LOW);
        }
    }
}
