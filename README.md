AMAP Android
==================

An application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls. 


Building in Android Studio
=========================

// Requires Gradle 2.1+
// Requires Android 1.4+
// Each build requires the correct Ayla Mobile Library. export AYLA_LIB_BRANCH &
// AYLA_ZLIB_BRANCH to the shell environment if using other than 'master'.
                                                                                       
$ git clone https://github.com/AylaNetworks/AMAP_Android.git
$ cd AMAP_Android/gradle_scripts
$ gradle -q execTasks
                                                                                                  
Open Android Studio -> Import Project -> Select build.gradle file in project folder(Agile_Link_Android)                
To build the project in Android Studio, click 'Build' and select 'Make Project'

  Troubleshooting
  ===============
  
  If you encounter a DexIndexOverflowException when building your app, this means you've probably hit the 65536 method limit issue.  Google has addressed this.  Please follow their official guide for building apps with over 65536 methods here: http://developer.android.com/tools/building/multidex.html
  
  If you encounter an OutOfMemoryException when building your app, you'll want to increase the amount of memory available to Android Studio.  Please read this article for more information: http://tools.android.com/tech-docs/configuration


Generating Documentation
========================

$ gradle generateDebugJavadoc
(ignore warnings and the error/failure message)
Open app/build/docs/javadoc/index.html in your browser


Generate a library jar file
===========================

$ gradle clean compileReleaseJava jar                                                                   
Jar file will be generated at libraries/Android_AylaLibrary_Public/build/libs.
External jars required by the library need to be included in applications using this jar (gson-2.2.4, log4j-1.2.17). 

            
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

