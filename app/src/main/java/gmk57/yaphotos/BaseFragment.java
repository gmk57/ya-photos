package gmk57.yaphotos;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Base Fragment class to handle state indication (progress and error)
 */
public abstract class BaseFragment extends Fragment {
    protected static final int STATE_OK = 0;
    protected static final int STATE_LOADING = 1;
    protected static final int STATE_ERROR = 2;

    private static final String TAG = "BaseFragment";
    private static final String KEY_PROGRESS_STATE = "progressState";

    private int mProgressState;
    private View mProgressIndicator;
    private View mErrorLayout;

    /**
     * Called to have the fragment instantiate its user interface view by
     * BaseFragment's {@link #onCreateView} with the same parameters.
     * <p>
     * For state indication to function properly, returned View should contain
     * Views with IDs <code>"progress_indicator"</code> and
     * <code>"error_layout"</code> (latter containing Button with ID
     * <code>"try_again_button"</code>).
     *
     * @param inflater           The LayoutInflater object to inflate views
     * @param container          If non-null, this is the parent view
     * @param savedInstanceState If non-null, this is the previous saved state
     * @return Return the View for the fragment's UI. Should not be null.
     */
    @NonNull
    protected abstract View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState);

    /**
     * Called by "Try again" button (visible in case of errors).
     * Subclasses should provide specific implementation to reload their content.
     * No need to call setupProgressState(STATE_LOADING), BaseFragment takes care of that.
     */
    protected abstract void tryAgain();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mProgressState = savedInstanceState.getInt(KEY_PROGRESS_STATE);
        }
    }

    @NonNull
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, container, savedInstanceState);

        mProgressIndicator = view.findViewById(R.id.progress_indicator);
        mErrorLayout = view.findViewById(R.id.error_layout);
        Button tryAgainButton = view.findViewById(R.id.try_again_button);
        if (tryAgainButton != null) {
            tryAgainButton.setOnClickListener(v -> {
                setupProgressState(STATE_LOADING);
                tryAgain();
            });
        }
        setupProgressState(mProgressState);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PROGRESS_STATE, mProgressState);
    }

    /**
     * Saves operation state in private variable (persisted via saved state Bundle)
     * and displays it if possible (= if view is already created).
     *
     * @param state One of protected constants: <code>STATE_OK</code>,
     *              <code>STATE_LOADING</code> or <code>STATE_ERROR</code>
     */
    protected void setupProgressState(int state) {
        mProgressState = state;
        if (mProgressIndicator != null) {
            mProgressIndicator.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        }
        if (mErrorLayout != null) {
            mErrorLayout.setVisibility(state == STATE_ERROR ? View.VISIBLE : View.GONE);
        }
    }
}
