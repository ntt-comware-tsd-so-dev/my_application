AMAP Android
==================

An application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls.


Building in Android Studio
=========================

// Requires Gradle 2.1+
// supported Android version: 4.4 and higher
// Each build requires the correct Ayla Mobile Library. export AYLA_LIB_BRANCH &
// AYLA_ZLIB_BRANCH to the shell environment if using other than 'master'.

$ git clone https://github.com/AylaNetworks/AMAP_Android.git
$ cd AMAP_Android/gradle_scripts
$ gradle -q execTasks

To build the project in Android Studio, click 'Build' and select 'Make Project'

  Troubleshooting
  ===============

  If you encounter a DexIndexOverflowException when building your app, this means you've probably hit the 65536 method limit issue.  Google has addressed this.  Please follow their official guide for building apps with over 65536 methods here: http://developer.android.com/tools/building/multidex.html

  If you encounter an OutOfMemoryException when building your app, you'll want to increase the amount of memory available to Android Studio.  Please read this article for more information: http://tools.android.com/tech-docs/configuration


Generating Documentation
===========================

$ gradle generateDebugJavadoc
(ignore warnings and the error/failure message)
Open app/build/docs/javadoc/index.html in your browser


Generate a library jar file
===========================

$ gradle clean compileReleaseJava jar                                                                   
Jar file will be generated at libraries/Android_AylaLibrary_Public/build/libs.
External jars required by the library need to be included in applications using this jar (gson-2.2.4, log4j-1.2.17).



Release Notes
===========================
AMAP v4.4.0 : AYLA_LIB_BRANCH=release/4.4.03, AYLA_ZIGBEE_LIB_BRANCH=release/4.4.00, AYLA_CORE_BRANCH=release/4.4.03 (4/6/2016)
- AMAP is now divided into two parts, AMAP_Core_Framework and AMAP_Android. This separation will allow developers much easier updates of the core functionality of AMAP while keeping custom UI elements separate. The standard AMAP install procedure will pull down the appropriate core framework. The code in AMAP_Core_Framework should not be modified locally, and contains no user interface elements. All user interface elements now live in the AMAP_Android project instead.
- Remove static receiver registration from manifest for AylaConnectivityListener.

New
- Unused permissions removed from manifest files
- All required permissions are now documented in comments with reasons why they are required
- Build SDK version is now 23
- Avoid delay in transition to device detail page
- New support for connecting a device to a hidden WiFi network in setup
- Zigbee node unregister & reset functionality
- Role-based sharing support

Bug Fixes:
- Android M wifi setup fixes
- Fixed crash when encountering null BSSID
- Fixed crash if user attempts to change password while offline
- Fixed crash if user attempts to delete account while offline
- Fixed keyboard not always dismissed when leaving Edit Profile screen
- Fixed crash on profile update
- Fixed crash on Schedules in device detail page


AMAP v4.2.0 : AYLA_LIB_BRANCH=release/4.2.00, AYLA_ZLIB_BRANCH=release/4.2.00 (11/23/2015)
- Multi-LAN Mode support
- Full Generic Gateway and node support
- Spanish localization
- Multi-gateway support
- New email and sms property & device notifications
- WiFi Setup and Registration improvements
- Scheduling and Timer improvements
- UI & UX Improvements
- Bug fixes
- Known Issues
  Android 6.x is not supported (Some devices work and some do not)
