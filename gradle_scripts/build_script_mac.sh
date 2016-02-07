#!/bin/bash
#
# by default, with no need of any build variable configuration, this should build current repo of the current branch
# by automatically fetching the corresponding libraries of the same branch.
#
# However, you can chage the default behavior by the following optional environment variables:
# AYLA_BUILD_BRANCH: default to the current branch
# AYLA_LIB_BRANCH: default to AYLA_BUILD_BRANCH
# AYLA_ZIGBEE_LIB_BRANCH: default to AYLA_BUILD_BRANCH
# AYLA_LIB_REPO: default to https://github.com/AylaNetworks/Android_AylaLibrary(_Public).git
# AYLA_ZIGBEE_LIB_REPO: default to https://github.com/AylaNetworks/Android_AylaZigbeeLibrary(_Public).git
# AYLA_REMOTE: default to origin
#

# conext display: show value whenever related environment variables are set
build_var_name_list="AYLA_BUILD_BRANCH AYLA_LIB_BRANCH AYLA_ZIGBEE_LIB_BRANCH AYLA_LIB_REPO AYLA_ZIGBEE_LIB_REPO AYLA_REMOTE"
for n in $build_var_name_list; do
    [ `printenv | grep "$n"` ] && echo -e "Your $n is set to \"${!n}\""
done

# set to origin if AYLA_REMOTE not set or empty
AYLA_REMOTE=${AYLA_REMOTE:-origin}

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
   # default all branches to the current branch if variable not set or empty
   AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-$cur_branch}
fi

public_repo_pattern=".*_Public\.git .*"
if [[ $cur_repo =~ $public_repo_pattern ]]; then
    # public build defaults, set to cur branch unless not found (very rare)
    AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-release/4.3.0}
    if [[ $AYLA_BUILD_BRANCH =~ .*\..* ]]; then
        # name as 4.2.0, lib branch has one more 0
        AYLA_LIB_BRANCH=${AYLA_LIB_BRANCH:-${AYLA_BUILD_BRANCH}0}
        AYLA_ZIGBEE_LIB_BRANCH=${AYLA_ZIGBEE_LIB_BRANCH:-${AYLA_BUILD_BRANCH}0}
    else
        # name as "master"
        AYLA_LIB_BRANCH=${AYLA_LIB_BRANCH:-${AYLA_BUILD_BRANCH}}
        AYLA_ZIGBEE_LIB_BRANCH=${AYLA_ZIGBEE_LIB_BRANCH:-${AYLA_BUILD_BRANCH}}
    fi

    AYLA_PUBLIC=_Public
    repo_type="public"
else
    # internal developers default
    AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-develop}
    AYLA_LIB_BRANCH=${AYLA_LIB_BRANCH:-${AYLA_BUILD_BRANCH}}
    AYLA_ZIGBEE_LIB_BRANCH=${AYLA_ZIGBEE_LIB_BRANCH:-${AYLA_BUILD_BRANCH}}

    AYLA_PUBLIC=
    repo_type="internal"
fi
AYLA_LIB_REPO=${AYLA_LIB_REPO:-https://github.com/AylaNetworks/Android_AylaLibrary${AYLA_PUBLIC}.git}
AYLA_ZIGBEE_LIB_REPO=${AYLA_ZIGBEE_LIB_REPO:-https://github.com/AylaNetworks/Android_AylaZigbeeLibrary${AYLA_PUBLIC}.git}

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
echo "(Want another branch? switch to that branch or set AYLA_BUILD_BRANCH environment variable to build it)"
echo "(Want libs from another branch? set AYLA_LIB_BRANCH and AYLA_ZIGBEE_LIB_BRANCH to build it)"
echo -e "(want to customize lib or zigbee lib repos? use AYLA_LIB_REPO and AYLA_ZIGBEE_LIB_REPO)\n"

for n in $build_var_name_list; do
    echo -e "Now $n = \"${!n}\""
done

echo -e "\ncheckout git repo"
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
