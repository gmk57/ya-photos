package gmk57.yaphotos;

import android.content.Intent;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class WebViewActivityTest {
    @Rule
    public IntentsTestRule<WebViewActivity> mTestRule = new WebViewActivityTestRule();

    @Test
    public void actionbarDisplaysAppName() throws Exception {
        onView(withId(R.id.action_bar))
                .check(matches(anything()))
                .check((view, noViewFoundException) ->
                        assertThat(((Toolbar) view).getTitle(), is("YaPhotos")));
    }

    @Test
    public void webView_DisplaysAboutPage() throws Exception {
        onView(withId(R.id.web_view))
                .check(matches(instanceOf(android.webkit.WebView.class)))
                .check((view, noViewFoundException) ->
                        assertEquals("file:///android_asset/about.htm",
                                ((WebView) view).getUrl()));
    }

    @Test
    @Ignore("fails on main test device")  // TODO: Replace with UI Automator?
    public void webView_HasLinkToGithub() throws Exception {
        WebView webView = mTestRule.getActivity().findViewById(R.id.web_view);
        getInstrumentation().runOnMainSync(() -> webView.getSettings().setJavaScriptEnabled(true));
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(TestHelper.mStubResult);

        onWebView().withElement(findElement(Locator.XPATH,
                "//a[text()='View source code on GitHub.']"))
                .check(webMatches(getText(), containsString("code")))
                .perform(webClick());

        intended(allOf(hasAction(Intent.ACTION_VIEW),
                hasData("https://github.com/gmk57/ya-photos")));
    }

    private static class WebViewActivityTestRule extends IntentsTestRule<WebViewActivity> {
        WebViewActivityTestRule() {
            super(WebViewActivity.class);
        }

        @Override
        protected Intent getActivityIntent() {
            return WebViewActivity.newIntent(getTargetContext(),
                    "file:///android_asset/about.htm");
        }
    }
}