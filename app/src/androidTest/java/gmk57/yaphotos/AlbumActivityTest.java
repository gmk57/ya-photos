package gmk57.yaphotos;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.design.widget.TabLayout;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.webkit.WebView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollRight;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollToLast;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AlbumActivityTest {
    @Rule
    public IntentsTestRule<AlbumActivity> mTestRule = new IntentsTestRule<>(AlbumActivity.class);

    @Test
    public void toolbarDisplaysAppName() throws Exception {
        Toolbar toolbar = mTestRule.getActivity().findViewById(R.id.toolbar);
        assertEquals("YaPhotos", toolbar.getTitle());
    }

    @Test
    public void toolbarDisplaysAppName2() throws Exception {
        onView(withId(R.id.toolbar)).check((view, noViewFoundException) ->
                assertEquals("YaPhotos", ((Toolbar) view).getTitle()));
    }

    @Test
    public void toolbarDisplaysAppName3() throws Exception {
        onView(withText("YaPhotos")).check(matches(isDisplayed()));
    }

    @Test
    public void toolbarHasMenuWithAbout() throws Exception {
        Toolbar toolbar = mTestRule.getActivity().findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        assertTrue(menu.hasVisibleItems());
        assertEquals(1, menu.size());
        assertEquals("About", menu.getItem(0).getTitle());
    }

    @Test
    public void toolbarHasMenuWithAbout2() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(allOf(withId(R.id.title), withText("About")))
                .check(matches(isDisplayed()));
    }

    @Test
    public void pagerCountIsThree() throws Exception {
        ViewPager viewPager = mTestRule.getActivity().findViewById(R.id.pager);
        assertEquals(3, viewPager.getAdapter().getCount());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void tabsHaveProperNames() throws Exception {
        TabLayout tabs = mTestRule.getActivity().findViewById(R.id.tabs);
        assertEquals("Recent", tabs.getTabAt(0).getText());
        assertEquals("Popular", tabs.getTabAt(1).getText());
        assertEquals("Of the day", tabs.getTabAt(2).getText());
    }

    @Test
    public void tabsHaveProperNames2() throws Exception {
        onView(withText("Recent")).check(matches(isDisplayed()));
        onView(withText("Popular")).check(matches(isDisplayed()));
        onView(withText("Of the day")).check(matches(isDisplayed()));
    }

    @Test
    public void firstAlbumSizeIs100() throws Exception {
        RecyclerView recyclerView = mTestRule.getActivity().findViewById(R.id.album_recycler_view);
        assertEquals(100, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void secondAlbumSizeIs50() throws Exception {
        onView(withId(R.id.pager))
                .perform(scrollRight());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> assertEquals(50,
                        ((RecyclerView) view).getAdapter().getItemCount()));
    }

    @Test
    public void lastAlbumSizeIsMinimum100() throws Exception {
        onView(withId(R.id.pager))
                .perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) ->
                        assertThat(((RecyclerView) view).getAdapter().getItemCount(),
                                greaterThanOrEqualTo(100)));
    }

    @Test
    public void aboutMenuFiresProperIntent() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(allOf(withId(R.id.title), withText("About")))
                .perform(click());
        intended(allOf(hasComponent(WebViewActivity.class.getName()),
                hasExtra("gmk57.yaphotos.url", "file:///android_asset/about.htm")
        ));
    }

    @Test
    public void aboutMenuDisplaysAboutPage() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(allOf(withId(R.id.title), withText("About")))
                .perform(click());
        onView(withId(R.id.web_view))
                .check(matches(instanceOf(android.webkit.WebView.class)))
                .check((view, noViewFoundException) ->
                        assertEquals("file:///android_asset/about.htm",
                                ((WebView) view).getUrl()));
    }

    @Test
    public void clickOnThumbnailFiresProperIntent() throws Exception {
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(actionOnItemAtPosition(40, click()));
        intended(allOf(hasComponent(PhotoActivity.class.getName()),
                hasExtra("gmk57.yaphotos.albumType", 0),
                hasExtra("gmk57.yaphotos.position", 40)));
    }

    @Test
    public void clickOnThumbnailDisplaysPhotoActivity() throws Exception {
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(actionOnItemAtPosition(40, click()));
        Photo photo = Repository.getInstance(getTargetContext())
                .getAlbum(0).getPhoto(40);
        onView(withId(R.id.action_bar)).check(matches(anything()));
        onView(withId(R.id.action_bar)).check((view, noViewFoundException) -> {
            String subtitle = ((Toolbar) view).getSubtitle().toString();
            assertThat(subtitle, containsString(photo.getAuthor()));
            assertThat(subtitle, containsString(photo.getTitle()));
        });
    }

    @Test
    public void recreatedActivityKeepsPositions() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(80));

        getInstrumentation().runOnMainSync(() -> mTestRule.getActivity().recreate());

        onView(withId(R.id.pager)).check((view, noViewFoundException) ->
                assertThat(((ViewPager) view).getCurrentItem(), is(2)));
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> {
                    RecyclerView recyclerView = (RecyclerView) view;
                    GridLayoutManager mgr = (GridLayoutManager) recyclerView.getLayoutManager();
                    assertThat(mgr.findFirstVisibleItemPosition(), greaterThan(0));
                });
    }

    @Test
    public void rotatedActivityKeepsPositions() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(80));

        int currentOrientation = getTargetContext().getResources().getConfiguration().orientation;
        int targetOrientation = (currentOrientation == Configuration.ORIENTATION_PORTRAIT) ?
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        mTestRule.getActivity().setRequestedOrientation(targetOrientation);

        onView(withId(R.id.pager)).check((view, noViewFoundException) ->
                assertThat(((ViewPager) view).getCurrentItem(), is(2)));
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> {
                    RecyclerView recyclerView = (RecyclerView) view;
                    GridLayoutManager mgr = (GridLayoutManager) recyclerView.getLayoutManager();
                    assertThat(mgr.findFirstVisibleItemPosition(), greaterThan(0));
                });
    }
}