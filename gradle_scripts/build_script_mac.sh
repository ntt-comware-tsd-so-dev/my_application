#!/bin/bash

cur_repo=`git remote -v`
if [ $? -ne 0 ]; then
   echo "can not find your git remote repository"
   exit 1
fi

git_pattern=".*\.git .*"
if ! [[ $cur_repo =~ $git_pattern ]]; then
   echo "not git remote repository."
   exit 2
fi

branch_string=`git branch |grep -E "\*"`
if [ $? -ne 0 ]; then
    echo "No current branch found"
else
   cur_branch=`echo "${branch_string}" | cut -d' ' -f 2`
   # default all branches to the current branch
   : ${AYLA_BUILD_BRANCH=$cur_branch}
fi

public_repo_pattern=".*_Public\.git .*"
if [[ $cur_repo =~ $public_repo_pattern ]]; then
    AYLA_PUBLIC=_Public
    # public build defaults
    : ${AYLA_BUILD_BRANCH=release/4.3.0}
    # lib branch has one more 0
    : ${AYLA_LIB_BRANCH=${AYLA_BUILD_BRANCH}0}
    : ${AYLA_ZIGLIB_BRANCH=${AYLA_BUILD_BRANCH}0}
    echo -e "\n*** Building public repo on branch ${AYLA_BUILD_BRANCH} ***"
else
    AYLA_PUBLIC=
    # internal developers default
    : ${AYLA_BUILD_BRANCH=develop}
    : ${AYLA_LIB_BRANCH=${AYLA_BUILD_BRANCH}}
    : ${AYLA_ZIGLIB_BRANCH=${AYLA_BUILD_BRANCH}}
    echo -e "\n*** Building internal repo on branch ${AYLA_BUILD_BRANCH} ***"
fi
echo -e "*** Build for another branch? you can switch to that branch or set environment variables to rebuild ***\n"

echo ' checkout git repo'
# if AYLA_BUILD_BRANCH not set, then set to master
: ${MASTER=master}
: ${AYLA_REMOTE=origin}

cd ..
git fetch $AYLA_REMOTE
git branch $AYLA_BUILD_BRANCH $AYLA_REMOTE/$AYLA_BUILD_BRANCH
git checkout $AYLA_BUILD_BRANCH
git pull
rm -rf libraries
mkdir libraries
cd libraries
git clone https://github.com/AylaNetworks/Android_AylaLibrary$AYLA_PUBLIC.git
git clone https://github.com/AylaNetworks/Android_AylaZigbeeLibrary$AYLA_PUBLIC.git

if [ "$AYLA_PUBLIC" == "" ]; then
    echo "Set up symbolic link for gradle project dependency"
    ln -sf Android_AylaLibrary Android_AylaLibrary_Public
    ln -sf Android_AylaZigbeeLibrary Android_AylaZigbeeLibrary_Public
fi

export ZIGBEE_PATH=$PWD

if [ "$AYLA_BUILD_BRANCH" == "$MASTER" ]
then
    echo already have $AYLA_BUILD_BRANCH
    pushd Android_AylaLibrary$AYLA_PUBLIC
else
    pushd Android_AylaZigbeeLibrary$AYLA_PUBLIC
    echo Get Android_AylaZigbeeLibrary from branch $AYLA_ZIGLIB_BRANCH
    git fetch $AYLA_REMOTE
    git branch $AYLA_ZIGLIB_BRANCH $AYLA_REMOTE/$AYLA_ZIGLIB_BRANCH
    git checkout $AYLA_ZIGLIB_BRANCH
    popd
    pushd Android_AylaLibrary$AYLA_PUBLIC
    echo Get Android_AylaLibrary from branch $AYLA_LIB_BRANCH
    git fetch $AYLA_REMOTE
    git branch $AYLA_LIB_BRANCH $AYLA_REMOTE/$AYLA_LIB_BRANCH
    git checkout $AYLA_LIB_BRANCH
fi

rm -rf lib/src/com/aylanetworks/aaml/zigbee
ln -s $ZIGBEE_PATH/Android_AylaZigbeeLibrary$AYLA_PUBLIC/zigbee lib/src/com/aylanetworks/aaml/zigbee

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
