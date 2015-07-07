#!/bin/bash
echo ' checkout git repo'
# if AYLA_BUILD_BRANCH not set, then set to master
: ${AYLA_BUILD_BRANCH=master}
echo Building from branch $AYLA_BUILD_BRANCH

cd ..

rm -rf libraries
mkdir libraries

cd libraries
git clone https://github.com/AylaNetworks/Android_AylaLibrary.git
git clone https://github.com/AylaNetworks/Android_AylaZigbeeLibrary.git

export ZIGBEE_PATH=$PWD
cd Android_AylaLibrary
git checkout -b $AYLA_BUILD_BRANCH origin/$AYLA_BUILD_BRANCH
#git checkout -b master_candidate_merge3 origin/master_candidate_merge3
rm -rf lib/src/com/aylanetworks/aaml/zigbee
ln -s $ZIGBEE_PATH/Android_AylaZigbeeLibrary/zigbee lib/src/com/aylanetworks/aaml/zigbee


#check if symlink is created
export symlinkPath=$PWD/lib/src/com/aylanetworks/aaml/zigbee
if [[ -L $symlinkPath ]]; then
	#statements
	echo 'Created symlink for zigbee package '
	cd lib
	gradle build
else 
	echo 'Symlink creation for zigbee package failed. '
	echo 'BUILD FAILED'
fi
	




