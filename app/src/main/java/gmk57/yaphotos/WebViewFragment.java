package gmk57.yaphotos;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Fragment to display WebView. Should be constructed through <code>newInstance</code> factory
 * method. WebView is very basic, without JavaScript or link interception.
 */
public class WebViewFragment extends Fragment {
    private static final String TAG = "WebViewFragment";
    private static final String ARG_URL = "url";

    /**
     * Factory method to create this fragment.
     *
     * @param url Valid url (local or remote)
     * @return Instance of this fragment
     */
    public static WebViewFragment newInstance(String url) {
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);

        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);

        String url = getArguments().getString(ARG_URL);
        WebView webView = view.findViewById(R.id.web_view);
        webView.loadUrl(url);

        return view;
    }
}
