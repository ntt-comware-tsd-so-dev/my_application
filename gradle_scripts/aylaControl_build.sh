#!/bin/bash
echo ' checkout git repo'
cd ..
rm -rf libraries
mkdir libraries
cd libraries
git clone https://github.com/AylaNetworks/Android_AylaLibrary.git
git clone https://github.com/AylaNetworks/Android_AylaZigbeeLibrary.git

# replace ZIGBEE_PATH with path to your zigbee library
export ZIGBEE_PATH=/Desktop/Agile_Link_Android/libraries/Android_AylaZigbeeLibrary

cd Android_AylaLibrary
git checkout -b master_candidate_merge3 origin/master_candidate_merge3
rm -rf lib/src/com/aylanetworks/aaml/zigbee
ln -s $ZIGBEE_PATH/zigbee lib/src/com/aylanetworks/aaml/zigbee
gradle build


