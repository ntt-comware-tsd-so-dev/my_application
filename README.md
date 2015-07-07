Agile_Link_Android
==================

Application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls. 


Building in Android Studio
=========================

Requires gradle version 2.1.+                                                                                         
git clone https://github.com/AylaNetworks/Agile_Link_Android.git                                                    
cd Agile_Link_Android/gradle_scripts                                                                                
Set environment variable AYLA_BUILD_BRANCH (defaults to master) to the branch to be built                                                                                                                             
gradle -q execTasks                                                                                                  
Open Android Studio -> Import Project -> Select build.gradle file in project folder(Agile_Link_Android)                
To build the project in Android Studio, click 'Build' and select 'Make Project'

  Troubleshooting
  ===============
  
  If you encounter a DexIndexOverflowException when building your app, this means you've probably hit the 65536 method limit issue.  Google has addressed this.  Please follow their official guide for building apps with over 65536 methods here: http://developer.android.com/tools/building/multidex.html
  
  If you encounter an OutOfMemoryException when building your app, you'll want to increase the amount of memory available to Android Studio.  Please read this article for more information: http://tools.android.com/tech-docs/configuration


Generating Documentation
========================

gradle generateDebugJavadoc
(ignore errors)
Open app/build/docs/javadoc/index.html in your favorite browser

