package gmk57.yaphotos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.hamcrest.Matcher;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class TestHelper {
    static final Instrumentation.ActivityResult mStubResult =
            new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);

    /**
     * Rotate device screen (from landscape to portrait or vice versa).
     * Source: <a href="http://blog.sqisland.com/2015/10/espresso-save-and-restore-state.html">
     * post by Chiu-Ki Chan</a>
     */
    public static void rotateScreen(ActivityTestRule<? extends Activity> testRule) {
        int currentOrientation = getTargetContext().getResources().getConfiguration().orientation;
        int targetOrientation = (currentOrientation == Configuration.ORIENTATION_PORTRAIT) ?
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        testRule.getActivity().setRequestedOrientation(targetOrientation);
    }

    /**
     * Matcher for intent that is wrapped in a chooser intent.
     * Source: <a href="https://groups.google.com/forum/#!topic/android-testing-support-library/Mj3tF5S7puU">
     * Google Groups answer</a>
     */
    public static Matcher<Intent> chooser(Matcher<Intent> matcher) {
        return allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), matcher));
    }

    /**
     * Wrapper for performing action with custom constraints. Can be useful for testing
     * pull-to-refresh, because Espresso by default refuses to swipe down a view that is less than
     * 90% visible.
     * Source: <a href="https://stackoverflow.com/a/33516360">Stack Overflow answer by Thomas Keller</a>
     */
    public static ViewAction withCustomConstraints(final ViewAction action,
                                                   final Matcher<View> constraints) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return constraints;
            }

            @Override
            public String getDescription() {
                return action.getDescription();
            }

            @Override
            public void perform(UiController uiController, View view) {
                action.perform(uiController, view);
            }
        };
    }
}
