echo checkout git repo
cd ..
rmdir /s /q libraries
mkdir libraries
cd libraries
git clone -b develop https://github.com/AylaNetworks/Android_AylaLibrary.git
git clone -b develop https://github.com/AylaNetworks/Android_AylaZigbeeLibrary.git
rmdir /s /q Android_AylaLibrary\lib\src\com\aylanetworks\aaml\zigbee
xcopy Android_AylaZigbeeLibrary\zigbee Android_AylaLibrary\lib\src\com\aylanetworks\aaml\zigbee\ /s /e /y
rmdir /s /q Android_AylaZigbeeLibrary
cd Android_AylaLibrary
gradle build
