#!/bin/bash

#
# Constants
#

NORMAL_DIR='normal-icons-pre-v11'
V11_DIR='black-square-icons-v11'

NORMAL_ARG='normal'
V11_ARG='v11'
SWAP_ARG='swap'
SHOW_ARG='show'
NONE_ARG='none'

NORMAL_MIN_API='3'
V11_MIN_API='11'

NORMAL_V11_COMMENT="\/\/\/\*v11\*\/"
V11_V11_COMMENT="\/\*v11\*\/"

#
# Functions
#

function swap_current {
    pushd $wd >/dev/null

    if [ $current = $NORMAL_DIR ]
    then
        ln -sfn $V11_DIR current
    else
        ln -sfn $NORMAL_DIR current
    fi

    current=`basename \`realpath current\``
    popd >/dev/null
}

function set_cur_to_req {
    pushd $wd >/dev/null

    ln -sfn $req_dir current

    current=`basename \`realpath current\``
    popd >/dev/null
}

function update_manifest {
    pushd $wd/.. >/dev/null

    manifest="AndroidManifest.xml"

    old_api_long=`printf "%03d" $old_api`
    new_api_long=`printf "%03d" $new_api`

    old_vc="versionCode=\\\"$old_api_long"
    new_vc="versionCode=\\\"$new_api_long"

    old_mv="minSdkVersion=\\\"$old_api\\\""
    new_mv="minSdkVersion=\\\"$new_api\\\""

    sed -i -e"s/$old_vc/$new_vc/" $manifest
    sed -i -e"s/$old_mv/$new_mv/" $manifest

    popd >/dev/null
}

function update_src {
    pushd $wd/.. >/dev/null

    for src in src/com/darshancomputing/BatteryIndicator*/*.java
    do
        sed -i -e"s/$old_comment/$new_comment/" $src
    done

    popd >/dev/null
}

#
# Main entry point
#

# Get working directory

wd=`dirname \`realpath $0\``
cd $wd

# Get current

current=`basename \`realpath current 2> /dev/null\` 2> /dev/null`

if [ $? -ne 0 ]
then
    current='none'
fi

old_cur=$current

# Get goal from arg 1 or assume goal is to swap

if [ $# -gt 0 ]
then
    if [ $1 = $NORMAL_ARG ]
    then
        req_dir=$NORMAL_DIR
    elif [ $1 = $V11_ARG ]
    then
        req_dir=$V11_DIR
    elif [ $1 = $SWAP_ARG ]
    then
        req_dir=$SWAP_ARG
    elif [ $1 = $NONE_ARG ]
    then
        req_dir=$NONE_ARG
    elif [ $1 = $SHOW_ARG ]
    then
        echo "Currently using $current"
        exit
    else
        echo "Error: '$1' not valid; please choose '$NORMAL_ARG' or '$V11_ARG', or leave off to swap."
        exit
    fi
else
    if [ $current = 'none' ]
    then
        echo "Please choose '$NORMAL_ARG' or '$V11_ARG' to set initial version."
        exit
    fi

    req_dir=$SWAP_ARG
fi

# Quit if we're done

if [ $req_dir = $current ]
then
    echo "Already set to $current"
    exit
elif [ $req_dir = 'none' ]
then
    rm_cur_files

    cd $wd
    rm current

    current='none'
else
    # Set current; removing old files if necessary

    if [ $current = 'none' ]
    then
        set_cur_to_req
    else
        swap_current

        if [ $req_dir = 'swap' ]
        then
            req_dir=$current
        fi
    fi
fi

# Set vars

if [[ $old_cur = $NORMAL_DIR || $old_cur = 'none' ]]
then
    old_api=$NORMAL_MIN_API
    old_comment=$NORMAL_V11_COMMENT
else
    old_api=$V11_MIN_API
    old_comment=$V11_V11_COMMENT
fi

if [[ $req_dir = $NORMAL_DIR || $req_dir = 'none' ]]
then
    new_api=$NORMAL_MIN_API
    new_comment=$NORMAL_V11_COMMENT
else
    new_api=$V11_MIN_API
    new_comment=$V11_V11_COMMENT
fi

# Update manifest and src if necessary

if [ $old_api != $new_api ]
then
    update_manifest
    update_src
fi

# Report and finish

echo "Switched to $current"
