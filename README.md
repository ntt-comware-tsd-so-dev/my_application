Agile_Link_Android
==================

Application designed to support multiple devices, themes, with strong separation of MVC &amp; UI, business logic, and library calls. 



Building in Android Studio
=========================

git clone https://github.com/AylaNetworks/Agile_Link_Android.git
cd Agile_Link_Android/gradle_scripts
Add your local Android_AylaZigbeeLibrary repo path to ZIGBEE_PATH in 'aylaControl_build.sh' file
gradle -q execTasks
Open Android Studio -> Import Project -> Select build.gradle file in project folder(nexTurn_Android)
To build the project in Android Studio, click 'Build' and select 'Make Project'
