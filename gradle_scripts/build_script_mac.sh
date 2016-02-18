#!/bin/bash
#
# The following environment variables are optional for you to control your build:
# AYLA_BUILD_BRANCH: default to the current branch
# AYLA_LIB_BRANCH: default to AYLA_BUILD_BRANCH
# AYLA_ZIGBEE_LIB_BRANCH: default to AYLA_BUILD_BRANCH
#
# The following are for rare cases when you use a git protocol other than https or a different remote
# script detects "public" and "internal" repos automatically
# AYLA_LIB_REPO: default to https://github.com/AylaNetworks/Android_AylaLibrary(_Public).git
# AYLA_ZIGBEE_LIB_REPO: default to https://github.com/AylaNetworks/Android_AylaZigbeeLibrary(_Public).git
# AYLA_REMOTE: default to origin
#

# if you do not specifiy branch, the script use your current branch for build and library branches
# if you want to build a branch other than your crrent branch, switch to that branch to build it
AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-}  #release/4.4.00

# must set lib branch here for public release by replacing $AYLA_BUILD_BRANCH with release/4.4.00 etc because
# their branch names are different; for internal repos, lib branches can be the same as build branch such as "develop" etc
AYLA_LIB_BRANCH=${AYLA_LIB_BRANCH:-$AYLA_BUILD_BRANCH} #release/4.4.00
AYLA_ZIGBEE_LIB_BRANCH=${AYLA_ZIGBEE_LIB_BRANCH:-$AYLA_BUILD_BRANCH} #release/4.4.00
AYLA_REMOTE=${AYLA_REMOTE:-origin}

# script can detect lib repos automatically. If you want something diferent, set values here
AYLA_LIB_REPO=${AYLA_LIB_REPO:-}
AYLA_ZIGBEE_LIB_REPO=${AYLA_ZIGBEE_LIB_REPO:-}

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
    AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-master}
else
   cur_branch=`echo "${branch_string}" | cut -d' ' -f 2`
   # default all branches to the current branch if variable not set or empty
   AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-$cur_branch}
fi

public_repo_pattern=".*_Public\.git .*"
if [[ $cur_repo =~ $public_repo_pattern ]]; then
    AYLA_PUBLIC=_Public
    repo_type="public"
else
    # internal developers default
    AYLA_PUBLIC=
    repo_type="internal"
fi
AYLA_LIB_REPO=${AYLA_LIB_REPO:-https://github.com/AylaNetworks/Android_AylaLibrary${AYLA_PUBLIC}.git}
AYLA_ZIGBEE_LIB_REPO=${AYLA_ZIGBEE_LIB_REPO:-https://github.com/AylaNetworks/Android_AylaZigbeeLibrary${AYLA_PUBLIC}.git}

# in case when AYLA_BUILD_BRANCH is default to current branch
AYLA_LIB_BRANCH=${AYLA_LIB_BRANCH:-$AYLA_BUILD_BRANCH}
AYLA_ZIGBEE_LIB_BRANCH=${AYLA_ZIGBEE_LIB_BRANCH:-$AYLA_BUILD_BRANCH}

release_branch_pattern="release.*"
if [[ $cur_branch =~ $release_branch_pattern ]]; then
    if [ "X$AYLA_LIB_BRANCH" == "X${AYLA_BUILD_BRANCH}" ]; then
        echo "Warning: for release branches, library branch should be different form build branch."
        echo -e "Please set your AYLA_LIB_BRANCH to the right value.\n"
    fi
    if [ "X$AYLA_ZIGBEE_LIB_BRANCH" == "X${AYLA_BUILD_BRANCH}" ]; then
        echo "Warning: for release branches, zigbee library branch should be different form build branch."
        echo -e "Please set your AYLA_ZIGBEE_LIB_BRANCH to the right value.\n"
    fi
fi

# conext display: show value whenever related environment variables are set
build_var_name_list="AYLA_BUILD_BRANCH AYLA_LIB_BRANCH AYLA_ZIGBEE_LIB_BRANCH AYLA_LIB_REPO AYLA_ZIGBEE_LIB_REPO AYLA_REMOTE"
for n in $build_var_name_list; do
    [ `printenv | grep "$n"` ] && echo -e "Your $n is set to \"${!n}\""
done

green=`tput setaf 2`
reset=`tput sgr0`
styled_repo_type=${green}${repo_type}${reset}
styled_branch=${green}${AYLA_BUILD_BRANCH}${reset}
styled_lib_branch=${green}${AYLA_LIB_BRANCH}${reset}
styled_zigbee_lib_branch=${green}${AYLA_ZIGBEE_LIB_BRANCH}${reset}
styled_lib_repo=${green}${AYLA_LIB_REPO}${reset}
styled_zigbee_lib_repo=${green}${AYLA_ZIGBEE_LIB_REPO}${reset}
echo -e "\n*** Building ${styled_repo_type} repo on branch ${styled_branch} with lib branch ${styled_lib_branch} and zigbee branch ${styled_zigbee_lib_branch}  ***"
echo "*** lib repo: ${styled_lib_repo}  zigbee lib repo: ${styled_zigbee_lib_repo} ***"

for n in $build_var_name_list; do
    echo -e "Now $n = \"${!n}\""
done

echo -e "\ncheckout git repo"

cd ..
git fetch $AYLA_REMOTE
git branch $AYLA_BUILD_BRANCH $AYLA_REMOTE/$AYLA_BUILD_BRANCH
git checkout $AYLA_BUILD_BRANCH
git pull

rm -rf libraries
mkdir libraries
cd libraries
git clone $AYLA_LIB_REPO
git clone $AYLA_ZIGBEE_LIB_REPO

if [ "$AYLA_PUBLIC" == "" ]; then
    echo "Set up symbolic link for gradle project dependency"
    ln -sf Android_AylaLibrary Android_AylaLibrary_Public
    ln -sf Android_AylaZigbeeLibrary Android_AylaZigbeeLibrary_Public
fi

export ZIGBEE_PATH=$PWD

pushd Android_AylaZigbeeLibrary$AYLA_PUBLIC
echo Get Android_AylaZigbeeLibrary from branch $AYLA_ZIGBEE_LIB_BRANCH
git fetch $AYLA_REMOTE
git branch $AYLA_ZIGBEE_LIB_BRANCH $AYLA_REMOTE/$AYLA_ZIGBEE_LIB_BRANCH
git checkout $AYLA_ZIGBEE_LIB_BRANCH
popd

pushd Android_AylaLibrary$AYLA_PUBLIC
echo Get Android_AylaLibrary from branch $AYLA_LIB_BRANCH
git fetch $AYLA_REMOTE
git branch $AYLA_LIB_BRANCH $AYLA_REMOTE/$AYLA_LIB_BRANCH
git checkout $AYLA_LIB_BRANCH

rm -rf lib/src/com/aylanetworks/aaml/zigbee
ln -s $ZIGBEE_PATH/Android_AylaZigbeeLibrary$AYLA_PUBLIC/zigbee lib/src/com/aylanetworks/aaml/zigbee

#check if symlink is created
export symlinkPath=$PWD/lib/src/com/aylanetworks/aaml/zigbee
if [[ -L $symlinkPath ]]; then
    #statements
    echo -e '\nCreated symlink for zigbee package\n '
    cd lib
    gradle build
else
    echo 'Symlink creation for zigbee package failed. '
    echo 'BUILD FAILED'
fi
