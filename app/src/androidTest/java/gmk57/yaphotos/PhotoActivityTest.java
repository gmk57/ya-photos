package gmk57.yaphotos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import gmk57.yaphotos.data.Photo;
import gmk57.yaphotos.data.source.AlbumRepository;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollToLast;
import static android.support.test.espresso.contrib.ViewPagerActions.scrollToPage;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class PhotoActivityTest {
    @Rule
    public IntentsTestRule<PhotoActivity> mTestRule = new PhotoActivityTestRule();

    private Photo mPhoto;
    private AlbumRepository mAlbumRepository;

    @Before
    public void setUp() throws Exception {
        mAlbumRepository = mTestRule.getActivity().mAlbumRepository;
        mPhoto = mAlbumRepository.getAlbum(2).getPhoto(0);
    }

    @Test
    public void actionbarSubtitle_DisplaysAuthorAndTitle() throws Exception {
        onView(withId(R.id.action_bar))
                .check(matches(anything()))
                .check((view, noViewFoundException) -> {
                    String subtitle = ((Toolbar) view).getSubtitle().toString();
                    assertThat(subtitle, containsString(mPhoto.getAuthor()));
                    assertThat(subtitle, containsString(mPhoto.getTitle()));
                });
    }

    @Test
    public void webpageButton_FiresProperIntent() throws Exception {
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(TestHelper.mStubResult);

        onView(withId(R.id.menu_item_webpage)).perform(click());

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(mPhoto.getPageUri())));
    }

    @Test
    public void shareButton_FiresProperIntent() throws Exception {
        onView(withId(R.id.menu_item_share)).check(doesNotExist());

        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(TestHelper.mStubResult);
        SystemClock.sleep(2000);

        onView(withId(R.id.menu_item_share)).perform(click());

        intended(TestHelper.chooser(allOf(hasAction(Intent.ACTION_SEND),
                hasType("image/jpeg"),
                hasExtra(Intent.EXTRA_SUBJECT, mPhoto.getTitle()),
                hasExtra(Intent.EXTRA_TEXT, mPhoto.getTitle()),
                hasExtraWithKey(Intent.EXTRA_STREAM))));
    }

    @Test
    public void clickOnImage_TogglesUiVisibility() throws Exception {
        onView(withId(R.id.action_bar)).check(matches(isCompletelyDisplayed()));

        onView(allOf(withId(R.id.fullscreen_image_view), isDisplayed())).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.action_bar)).check(matches(not(isDisplayed())));

        onView(allOf(withId(R.id.fullscreen_image_view), isDisplayed())).perform(click());

        onView(withId(R.id.action_bar)).check(matches(isCompletelyDisplayed()));
    }

    @Test
    public void rotatedActivity_KeepsUiVisibility() throws Exception {
        onView(allOf(withId(R.id.fullscreen_image_view), isDisplayed())).perform(click());

        TestHelper.rotateScreen(mTestRule);

        onView(withId(R.id.action_bar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void rotatedActivity_KeepsPosition() throws Exception {
        onView(withId(R.id.pager)).perform(scrollToPage(20));
        SystemClock.sleep(1000);
        String subtitle = mTestRule.getActivity().getSupportActionBar().getSubtitle().toString();

        TestHelper.rotateScreen(mTestRule);

        onView(withText(subtitle)).check(matches(anything()));
    }

    @Test
    public void swipe_ReplacesImageAndTitle() throws Exception {
        String subtitle = mTestRule.getActivity().getSupportActionBar().getSubtitle().toString();
        ImageView imageView1 = mTestRule.getActivity().findViewById(R.id.fullscreen_image_view);
        Bitmap bitmap1 = ((BitmapDrawable) imageView1.getDrawable()).getBitmap();

        onView(withId(R.id.pager)).perform(swipeLeft());
        SystemClock.sleep(1000);

        onView(withText(subtitle)).check(doesNotExist());
        ImageView imageView2 = mTestRule.getActivity().findViewById(R.id.fullscreen_image_view);
        Bitmap bitmap2 = ((BitmapDrawable) imageView2.getDrawable()).getBitmap();
        assertThat(bitmap2, is(not(bitmap1)));
    }

    @Test
    public void swipe_isEndless() throws Exception {
        int albumSize = mAlbumRepository.getAlbum(2).getSize();

        onView(withId(R.id.pager))
                .check((view, noViewFoundException) ->
                        assertThat(((ViewPager) view).getAdapter().getCount(), is(albumSize)))
                .perform(scrollToLast());
        SystemClock.sleep(3000);

        onView(withId(R.id.pager)).check((view, noViewFoundException) ->
                assertThat(((ViewPager) view).getAdapter().getCount(), is(greaterThan(albumSize))));
    }

    private static class PhotoActivityTestRule extends IntentsTestRule<PhotoActivity> {
        PhotoActivityTestRule() {
            super(PhotoActivity.class);
        }

        @Override
        protected Intent getActivityIntent() {
            return PhotoActivity.newIntent(getTargetContext(), 2, 0);
        }
    }
}
