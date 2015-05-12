#!/bin/bash
echo ' checkout git repo'
cd ..
rm -rf libraries
mkdir libraries
cd libraries
git clone https://github.com/AylaNetworks/Android_AylaLibrary.git
git clone https://github.com/AylaNetworks/Android_AylaZigbeeLibrary.git

export ZIGBEE_PATH=$PWD
cd Android_AylaLibrary
git checkout -b master_candidate origin/master_candidate
#git checkout -b master_candidate_merge3 origin/master_candidate_merge3
rm -rf lib/src/com/aylanetworks/aaml/zigbee
ln -s $ZIGBEE_PATH/Android_AylaZigbeeLibrary/zigbee lib/src/com/aylanetworks/aaml/zigbee
cd lib
gradle build


