echo checkout git repo
cd ..
rmdir /s /q libraries
mkdir libraries
cd libraries
git clone https://github.com/AylaNetworks/Android_AylaLibrary.git
cd Android_AylaLibrary
: git checkout -b master_candidate_merge3 origin/master_candidate_merge3
git checkout -b master_candidate origin/master_candidate
cd ..
git clone https://github.com/AylaNetworks/Android_AylaZigbeeLibrary.git
rmdir /s /q Android_AylaLibrary\lib\src\com\aylanetworks\aaml\zigbee
xcopy Android_AylaZigbeeLibrary\zigbee Android_AylaLibrary\lib\src\com\aylanetworks\aaml\zigbee\ /s /e /y
rmdir /s /q Android_AylaZigbeeLibrary
cd Android_AylaLibrary
gradle build