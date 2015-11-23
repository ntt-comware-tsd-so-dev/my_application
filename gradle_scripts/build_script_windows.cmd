echo checkout git repo
cd ..
rmdir /s /q libraries
mkdir libraries
cd libraries
git clone -b develop https://github.com/AylaNetworks/Android_AylaLibrary_Public.git
git clone -b develop https://github.com/AylaNetworks/Android_AylaZigbeeLibrary_Public.git
rmdir /s /q Android_AylaLibrary_Public\lib\src\com\aylanetworks\aaml\zigbee
xcopy Android_AylaZigbeeLibrary_Public\zigbee Android_AylaLibrary_Public\lib\src\com\aylanetworks\aaml\zigbee\ /s /e /y
rmdir /s /q Android_AylaZigbeeLibrary_Public
cd Android_AylaLibrary_Public
gradle build
