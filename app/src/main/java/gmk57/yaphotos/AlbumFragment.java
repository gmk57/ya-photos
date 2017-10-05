package gmk57.yaphotos;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Main app fragment to display album thumbnails, with scrolling (endless, if
 * possible)
 */
public class AlbumFragment extends BaseFragment {
    private static final String TAG = "AlbumFragment";
    private static final String ARG_ALBUM_TYPE = "albumType";

    private boolean mFetchRunning;
    private int mAlbumType;
    private Album mCurrentAlbum = new Album(null);
    private FetchAlbumTask mFetchAlbumTask;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mPhotoAdapter;
    private RecyclerView mRecyclerView;

    public static AlbumFragment newInstance(int albumType) {
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
        setRetainInstance(true);

        setupProgressState(STATE_LOADING);
        startFetchingAlbum();
    }

    @NonNull
    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        mRecyclerView = view.findViewById(R.id.album_recycler_view);

        mPhotoAdapter = new PhotoAdapter(mCurrentAlbum);
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
                fetchNextPageIfNeeded();
            }
        });

        return view;
    }

    @Override
    protected void tryAgain() {
        startFetchingAlbum();
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

    private void fetchNextPageIfNeeded() {
        if (!mFetchRunning && mCurrentAlbum.getNextPage() != null &&
                mLayoutManager.findLastVisibleItemPosition() + 40 > mLayoutManager.getItemCount()) {
            startFetchingAlbum(mCurrentAlbum);
        }
    }

    /**
     * Cancels current fetching and starts a new one via <code>FetchAlbumTask</code>.
     * <p>
     * If old album is provided, new album will be built on top of it, appending
     * photos of its <code>getNextPage()</code>.
     * Otherwise, new album will be built from scratch, according to album type
     * in shared preferences.
     *
     * @param oldAlbumVararg Old album (to append) or empty (to create from scratch)
     */
    private void startFetchingAlbum(Album... oldAlbumVararg) {
        mFetchRunning = true;
        if (mFetchAlbumTask != null) {
            mFetchAlbumTask.cancel(false);
        }
        mFetchAlbumTask = new FetchAlbumTask();
        mFetchAlbumTask.execute(oldAlbumVararg);
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

            Picasso.with(getActivity())
                    .load(photo.getThumbnailUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(mThumbnailImageView);
        }

        @Override
        public void onClick(View v) {
            startActivity(PhotoActivity.newIntent(getActivity(), mCurrentAlbum, mPosition));
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private static final String TAG = "PhotoAdapter";
        private Album mAlbum;

        public PhotoAdapter(Album album) {
            mAlbum = album;
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

            for (int offset : new int[]{8, -8, 16, -16, 24, -24}) {
                if (position + offset >= 0 && position + offset < mAlbum.getSize()) {
                    String url = mAlbum.getPhoto(position + offset).getThumbnailUrl();
                    Picasso.with(getActivity())
                            .load(url)
                            .priority(Picasso.Priority.LOW).fetch();
                }
            }
        }

        @Override
        public int getItemCount() {
            return mAlbum.getSize();
        }

        public void setAlbum(Album album) {
            mAlbum = album;
            if (album.getSize() == 0) {         // Clearing album
                notifyDataSetChanged();
            } else {                            // Appending new photos
                int newItems = album.getSize() - album.getOldSize();
                notifyItemRangeInserted(album.getOldSize(), newItems);
            }
        }
    }

    private class FetchAlbumTask extends AsyncTask<Album, Void, Album> {
        private static final String TAG = "FetchAlbumTask";

        @Override
        protected Album doInBackground(Album... oldAlbumVararg) {
            return new YaDownloader().fetchAlbum(mAlbumType, oldAlbumVararg);
        }

        @Override
        protected void onPostExecute(Album album) {
            mCurrentAlbum = album;
            if (isAdded() && mPhotoAdapter != null && album.getSize() > 0) {
                mPhotoAdapter.setAlbum(album);
                setupProgressState(STATE_OK);
            } else {
                setupProgressState(STATE_ERROR);
            }
            mFetchRunning = false;
        }
    }
}
