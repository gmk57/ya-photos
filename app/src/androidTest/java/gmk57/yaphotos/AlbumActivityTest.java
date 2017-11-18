package gmk57.yaphotos;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.support.design.widget.TabLayout;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollRight;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollToLast;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AlbumActivityTest {
    @Rule
    public IntentsTestRule<AlbumActivity> mTestRule = new IntentsTestRule<>(AlbumActivity.class);

    private Repository mRepository;

    @Before
    public void setUp() throws Exception {
        // Couldn't find any other way to retrieve the same instance of Repository that is injected
        // by Dagger into AlbumFragments. Subclassed Dagger component in test package gives another
        // instance.
        FragmentManager manager = mTestRule.getActivity().getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag("android:switcher:" + R.id.pager + ":0");
        mRepository = ((AlbumFragment) fragment).mRepository;
    }

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
    public void lastAlbumSizeIsMinimum90() throws Exception {
        onView(withId(R.id.pager))
                .perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) ->
                        assertThat(((RecyclerView) view).getAdapter().getItemCount(),
                                greaterThanOrEqualTo(90)));
    }

    @Test
    public void aboutMenu_FiresProperIntent() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        intending(anyIntent()).respondWith(TestHelper.mStubResult);

        onView(allOf(withId(R.id.title), withText("About")))
                .perform(click());

        intended(allOf(hasComponent(WebViewActivity.class.getName()),
                hasExtra("gmk57.yaphotos.url", "file:///android_asset/about.htm")));
    }

    @Test
    public void aboutMenu_DisplaysAboutPage() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(allOf(withId(R.id.title), withText("About")))
                .perform(click());
        onView(withId(R.id.web_view))
                .check(matches(instanceOf(android.webkit.WebView.class)))
                .check((view, noViewFoundException) ->
                        assertEquals("file:///android_asset/about.htm",
                                ((WebView) view).getUrl()));
    }


    /**
     * Should be started from AlbumActivity to provide proper back stack
     */
    @Test
    public void upButtonInWebViewActivity_ReturnsToAlbumActivity() throws Exception {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(allOf(withId(R.id.title), withText("About"))).perform(click());

        onView(withId(R.id.pager)).check(doesNotExist());

        onView(withClassName(is(AppCompatImageButton.class.getName())))
                .check(matches(anything()))
                .perform(click());

        onView(withId(R.id.pager)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnThumbnail_FiresProperIntent() throws Exception {
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(actionOnItemAtPosition(40, click()));
        intended(allOf(hasComponent(PhotoActivity.class.getName()),
                hasExtra("gmk57.yaphotos.albumType", 0),
                hasExtra("gmk57.yaphotos.position", 40)));
    }

    @Test
    public void clickOnThumbnail_DisplaysPhotoActivity() throws Exception {
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(actionOnItemAtPosition(40, click()));
        Photo photo = mRepository.getAlbum(0).getPhoto(40);

        onView(withId(R.id.action_bar)).check(matches(anything()));
        onView(withId(R.id.action_bar)).check((view, noViewFoundException) -> {
            String subtitle = ((Toolbar) view).getSubtitle().toString();
            assertThat(subtitle, containsString(photo.getAuthor()));
            assertThat(subtitle, containsString(photo.getTitle()));
        });
    }

    @Test
    public void recreatedActivity_KeepsPositions() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(50));

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
    public void rotatedActivity_KeepsPositions() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(50));
        TestHelper.rotateScreen(mTestRule);

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
    public void rotatedActivity_ChangesSpanCount() throws Exception {
        RecyclerView recyclerView = mTestRule.getActivity().findViewById(R.id.album_recycler_view);
        int oldSpanCount = ((GridLayoutManager) recyclerView.getLayoutManager()).getSpanCount();
        TestHelper.rotateScreen(mTestRule);

        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> {
                    RecyclerView newRecyclerView = (RecyclerView) view;
                    GridLayoutManager mgr = (GridLayoutManager) newRecyclerView.getLayoutManager();
                    assertThat(mgr.getSpanCount(), is(not(oldSpanCount)));
                });
    }

    @Test
    public void scrollDown_HidesAppBar() throws Exception {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));

        onView(withId(R.id.coordinator)).perform(swipeUp());

        onView(withId(R.id.toolbar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.tabs)).check(matches(not(isDisplayed())));
    }

    @Test
    public void scrollBack_ShowsTabsThenToolbar() throws Exception {
        onView(withId(R.id.coordinator)).perform(swipeUp(), swipeUp(), swipeUp(), swipeDown());
        onView(withId(R.id.toolbar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));

        onView(withId(R.id.coordinator)).perform(swipeDown(), swipeDown(), swipeDown());
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    public void scrollToEnd_FetchesNextPage() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        int albumSize = mRepository.getAlbum(2).getSize();

        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> assertThat(
                        ((RecyclerView) view).getAdapter().getItemCount(), is(albumSize)))
                .perform(scrollToPosition(albumSize - 1));
        SystemClock.sleep(3000);

        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> assertThat(
                        ((RecyclerView) view).getAdapter().getItemCount(), greaterThan(albumSize)));
    }

    @Test
    public void pullToRefresh_ResetsAlbum() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToLast());
        int oldSize = mRepository.getAlbum(2).getSize();
        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(oldSize - 1));
        SystemClock.sleep(3000);

        int newSize = mRepository.getAlbum(2).getSize();
        assertThat(newSize, is(greaterThan(oldSize)));

        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .perform(scrollToPosition(0))
                .perform(TestHelper.withCustomConstraints(swipeDown(), isDisplayingAtLeast(50)));
        SystemClock.sleep(3000);

        onView(allOf(withId(R.id.album_recycler_view), isDisplayed()))
                .check((view, noViewFoundException) -> assertThat(
                        ((RecyclerView) view).getAdapter().getItemCount(), lessThan(newSize)));
    }

    @Test
    public void thumbnailsAreLoaded() throws Exception {
        SystemClock.sleep(1000);
        RecyclerView recyclerView = mTestRule.getActivity().findViewById(R.id.album_recycler_view);
        ImageView view0 = recyclerView.getChildAt(0).findViewById(R.id.thumbnail_image_view);
        ImageView view6 = recyclerView.getChildAt(6).findViewById(R.id.thumbnail_image_view);
        Bitmap bitmap0 = ((BitmapDrawable) view0.getDrawable()).getBitmap();
        Bitmap bitmap6 = ((BitmapDrawable) view6.getDrawable()).getBitmap();

        assertThat(bitmap0, is(not(bitmap6)));
    }
}
