AMAP Android
==================

An application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls.


Building in Android Studio
=========================

// Requires Android Studio 2.3+
// supported Android version: 4.4 and higher  
// Each build requires the correct Ayla SDK and Ayla Core Framework branch. export AYLA_SDK_BRANCH &  
// AYLA_CORE_BRANCH to the shell environment if using other than 'master'.

$ git clone https://github.com/AylaNetworks/AMAP_Android_Public.git  
$ cd AMAP_Android_Public/gradle_scripts  
$ ../gradlew -q execTasks  

To build the project in Android Studio, click 'Build' and select 'Make Project'

  Troubleshooting
  ===============

  If you encounter a DexIndexOverflowException when building your app, this means you've probably hit the 65536 method limit issue.  Google has addressed this.  Please follow their official guide for building apps with over 65536 methods here: http://developer.android.com/tools/building/multidex.html

  If you encounter an OutOfMemoryException when building your app, you'll want to increase the amount of memory available to Android Studio.  Please read this article for more information: http://tools.android.com/tech-docs/configuration


Generating Documentation
===========================

$ ./gradlew generateDebugJavadoc  
(ignore warnings and the error/failure message)  
Open app/build/docs/javadoc/index.html in your browser


Generate a library jar file
===========================

$ ./gradlew clean compileReleaseJava jar  
Jar file will be generated at libraries/Android_AylaLibrary_Public/build/libs.  
External jars required by the library need to be included in applications using this jar (gson-2.2.4, log4j-1.2.17).

Contribute your code
====================

If you would like to contribute your own code change to our project, please submit pull requests against the "incoming" branch on Github. We will review and approve your pull requests if appropriate.

Dependencies
============

Google Volley		http://developer.android.com/license.html  
Google Gson		http://developer.android.com/license.html  
NanoHttpd		https://github.com/NanoHttpd/nanohttpd/blob/master/LICENSE.md  
Autobahn Websockets	https://github.com/crossbario/autobahn-android/blob/master/LICENSE  
SpongyCastle		http://www.bouncycastle.org/licence.html  
Joda			http://www.joda.org/joda-time/license.html  
Robolectric 		https://github.com/robolectric/robolectric/blob/master/LICENSE.txt

Version of major build tools used
=================================
Android Studio: 2.3
Gradle:3.3

Release Notes
===========================

AMAP v5.5.01 : AYLA_SDK_BRANCH=release/5.5.01, AYLA_CORE_BRANCH=release/5.5.00 (04/11/2017)
- Support Android_AylaSDK 5.5.01
- Fix lifecycle bug
- Add minimum time interval for refreshing authorization

AMAP v5.5.00 : AYLA_SDK_BRANCH=release/5.5.00, AYLA_CORE_BRANCH=release/5.5.00 (03/28/2017)

New and Improved
- Geofence and beacon improvements
  - New batched action sets make it easier for automations to control multiple devices and/or actions per device
- Google OAuth2 AuthProvider Support
  - Replaces Webview OAuth which has been deprecated by Google
  - See app note /doc/AppNote-UsingGoogleSign-in.pdf for details
- Improved About page
  - Add service location
  - Add SDK library version

Bug Fixes & Chores
- Built using Android Studio v2.3 with latest play services & tools
- Built using new Ayla_SDK  v5.5.00
- Built using new AMAP Core Framework v5.5.00
- General bug fixes
- Updated documentation
- Known Issues
  - Nexus 5x will intermittently fail to bind to the network during WiFi Setup. Retry to work-around.

AMAP v5.4.00 : AYLA_SDK_BRANCH=release/5.4.00, AYLA_CORE_BRANCH=release/5.4.00 (01/24/2017)

New & Improved
- Beacon support
  Automatically control a nearby device based on proximity and direction

Bug Fixes & Chores
- Built using new Ayla_SDK  v5.4.00
- Built using new AMAP Core Framework v5.4.00
- Move GCM implementation from AMAP Core Framework to AMAP
- Improved geofence cross-platform management
- App reskin and graphics changes
- General bug fixes
- Updated documentation
- Known Issues
  Nexus 5x will intermittently fail to bind to the network during WiFi Setup. Retry to work-around.

AMAP v5.3.01 : AYLA_SDK_BRANCH=release/5.3.01, AYLA_CORE_BRANCH=release/5.3.01 (12/16/2016)
- account settings bug fix

AMAP v5.3.00 : AYLA_SDK_BRANCH=release/5.3.01, AYLA_CORE_BRANCH=release/5.3.00 (11/09/2016)
- Geo-fencing support: automatically control a device based on your location and direction
- TouchID support: uUse TouchID to sign-in

AMAP v5.2.01 : AYLA_SDK_BRANCH=release/5.2.00, AYLA_CORE_BRANCH=release/5.2.00 (9/16/2016)
- add AP mode registration support

AMAP v5.2.00 : AYLA_SDK_BRANCH=release/5.2.00, AYLA_CORE_BRANCH=release/5.2.00 (8/19/2016)

New & Improved
- Improved performance and reliability
- New Android Wearable watch app
- UI & UX Improvements
- Dropped support for Zigbee Gateways (use Generic Gateway)

Bug Fixes & Chores
- Built using new Ayla_SDK  v5.2.00
- Built using new AMAP Core Framework v5.2.00

Known Issues
- Wifi setup will not work with Android N Beta. Scanning for WLAN access points fails. See https://code.google.com/p/android/issues/detail?id=2192

AMAP v4.4.01 : AYLA_LIB_BRANCH=release/4.4.04, AYLA_ZIGBEE_LIB_BRANCH=release/4.4.00, AYLA_CORE_BRANCH=release/4.4.03 (5/11/2016)
- make push notification permission unique in Manifest file by including application ID

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
