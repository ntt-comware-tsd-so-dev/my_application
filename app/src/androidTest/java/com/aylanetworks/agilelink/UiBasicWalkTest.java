package com.aylanetworks.agilelink;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.InstrumentationRegistry.*;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.runner.RunWith;
import org.junit.Rule;
import org.junit.Test;

import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

/*
 * Note: to set up your UI test user name and password in Android Studio, click on "Run" then
 * "Edit Configuration", then click on "+", find "Android Tests" input name "UiBasicWalkTest",
 * set extra options with " -e UiTestUser YOUR_USER -e UiTestPassword YOUR_PASSWORD"
 * Then you can run this UI tests. Your need to change code here to match the
 *  contents inside your own test account to make the test full PASS.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UiBasicWalkTest {

    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class);

    private void checkGroups() {
        onView(withContentDescription("Open")).perform(click());

        onView(withText("Groups")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Add Group")).perform(click());


        onView(withId(R.id.group_name)).perform(replaceText("TestGroup"));

        onView(withText("Add Group")).perform(click());
        onView(withText("Ayla EVB")).perform(click());
        onView(withText("Ayla EVB FF8a8 :)")).perform(click());

        onView(withText("OK")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Delete this group")).perform(click());
        onView(withText("OK")).perform(click());
        pressBack();
    }

    private void checkScenes() {
        onView(withContentDescription("Open")).perform(click());
        onView(withText("Scenes")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Add Scene")).perform(click());
        onView(withId(R.id.scene_name)).perform(replaceText("Test Scene"));
        onView(withText("Smart Bulb")).perform(click());
        onView(withText("Smart Plug")).perform(click());
        onView(withText("OK")).perform(click());

        pressBack();
    }

    private void checkGateways() {
        onView(withContentDescription("Open")).perform(click());
        onView(withText("Gateways")).perform(click());

        onView(withText("Generic Gateway")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Timezone...")).perform(click());
        onView(withText("Cancel")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Details...")).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.add_button)).perform(click());

        onView(withId(R.id.setup_btn)).perform(click());

        onView(withText("I've connected all the cables")).perform(click());

        onView(withText("The gateway is ready")).perform(click());

        onView(withId(R.id.register_btn)).perform(click());

        pressBack();
        pressBack();
        pressBack();
        pressBack();
        pressBack();
    }

    private void checkShares() {
        onView(withContentDescription("Open")).perform(click());
        onView(withText("Shares")).perform(click());

        onView(withId(R.id.add_button)).perform(click());

        swipeUp();

        pressBack();
        pressBack();
    }

    private void checkContactList() {
        onView(withContentDescription("Open")).perform(click());
        onView(withText("Contact List")).perform(click());

        onView(withId(R.id.add_button)).perform(click());

        pressBack();
    }

    private void checkDashboard() {
        onView(withContentDescription("Open")).perform(click());
        onView(withText("Dashboard")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Add Device")).perform(click());

        onView(withId(R.id.spinner_product_type)).perform(click());
        onView(withText("Smart Plug"));

        onView(withText("Ayla EVB")).perform(click());
        onView(withId(R.id.spinner_registration_type)).perform(click());

        onView(withText("Same-LAN")).perform(click());
        pressBack();

        onView(withText("Ayla EVB FF8a8 :)")).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Details...")).perform(click());
        pressBack();

        onView(withText("Schedule")).perform(click());
        pressBack();

        onView(withText("Notifications")).perform(click());
        pressBack();

        onView(withText("Share Device...")).perform(click());
        pressBack();
        pressBack();

        //onView(withText("Ayla f5d6")).perform(click());
        //pressBack();

        onView(withText("Smart Bulb")).perform(click());
        pressBack();

        onView(withText("Smart Plug")).perform(click());
        pressBack();

        //onView(withText("Generic Node")).perform(click());
        //pressBack();
    }

    String getAdbOption(String key) {
        Bundle extras = InstrumentationRegistry.getArguments();
        String v = null;

        if (extras != null) {
            if (extras.containsKey(key)) {
                v = extras.getString(key);
            } else {
                System.out.println("No user and password in extras");
            }
        } else {
            System.out.println("No extras");
        }

        return v;
    }

    /*
     * Note: to set up your UI test user name and password in Android Studio, click on "Run" then
     * "Edit Configuration", then click on "+", find "Android Tests" input name "UiBasicWalkTest",
     * set extra options with " -e UiTestUser YOUR_USER -e UiTestPassword YOUR_PASSWORD"
     * */
    private void login() {
        String user=getAdbOption("UiTestUser");
        String password=getAdbOption("UiTestPassword");
        onView(withId(R.id.userNameEditText)).perform(replaceText(user));
        onView(withId(R.id.passwordEditText)).perform(replaceText(password));

        onView(withId(R.id.buttonSignIn)).perform(click());
    }

    private void logout() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Sign Out")).perform(click());
        onView(withText("OK")).perform(click());
    }

    @Test
    public void testBasicWalk() {
        login();
        checkGroups();
        checkScenes();
        checkGateways();
        checkShares();

        onView(withContentDescription("Open")).perform(click());
        onView(withText("Account")).perform(click());
        pressBack();

        checkContactList();

        onView(withContentDescription("Open")).perform(click());
        onView(withText("Help")).perform(click());
        pressBack();

        onView(withContentDescription("Open")).perform(click());
        onView(withText("About")).perform(click());
        pressBack();

        checkDashboard();

        logout();
        assert(true);
    }

    public static Matcher<View> nthChildOf(final Matcher<View> parentMatcher, final int childPosition) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
            }
            @Override
            public boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof ViewGroup)) {
                    return false;
                }
                ViewGroup group = (ViewGroup) view.getParent();
                return parentMatcher.matches(group) && view.equals(group.getChildAt(childPosition));
            }
        };
    }

    public static ViewAction scrollToPosition(final int pos) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(android.support.v7.widget.RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "scroll to position";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((android.support.v7.widget.RecyclerView) view).scrollToPosition(pos);
            }
        };
    }



}