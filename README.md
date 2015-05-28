Agile_Link_Android
==================

Application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls. 



Building in Android Studio
=========================

Requires gradle version 2.1.+                                                                                         
git clone https://github.com/AylaNetworks/Agile_Link_Android.git                                                    
cd Agile_Link_Android/gradle_scripts                                                                                
gradle -q execTasks                                                                                                  
Open Android Studio -> Import Project -> Select build.gradle file in project folder(Agile_Link_Android)                
To build the project in Android Studio, click 'Build' and select 'Make Project'

Generating Documentation
========================

gradle generateDebugJavadoc
(ignore errors)
Open app/build/docs/javadoc/index.html in your favorite browser

