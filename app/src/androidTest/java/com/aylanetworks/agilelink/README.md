Note:

This folder inclues the UI test files that can sign into AMAP in emulator or a real device and walk through all major screens.

However before you run the test on your Android Studio, it is required to set up your UI test user name and password. Otherwise, the test can not run.

In Android Studio, click on "Run" then "Edit Configuration", then click on "+", find "Android Tests" input name "UiBasicWalkTest", set extra options with " -e UiTestUser YOUR_USER -e UiTestPassword YOUR_PASSWORD".Now run the "UiBasicWalkTest", select the emulator or real device, then you can see it go through all major screens. However the sample code uses a user with some devices configured only by this user. So please modify the code to match the device names and number in your account to make the test complete successfully for your user.
