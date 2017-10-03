#!/bin/bash
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.

PRE44=0

# output 4_0 instead of 40
if [ "$1" == "--pre44sym" ];
then
    PRE44=1
    shift
fi

INVER="0.0"
if [ $# -eq 0 ];
then
    read INVER
elif [ $# -eq 1 ];
then
    INVER=$1
else
    echo "$0: error: require one or zero arguments. If zero, read from stdin" >&2
    exit 1
fi

UND=`echo ${INVER} | tr '.' '_'`
MAJ0=`echo ${UND} | cut -d_ -f1`
MIN1=`echo ${UND} | cut -d_ -f2`
if [ ${MAJ0} -lt 49 ];
then
    if [ ${PRE44} -eq 0 ];
    then
        # pre 50:  paste together "4" and "8" to get 48
        echo -n "${MAJ0}${MIN1}"
    else
        # pre 50: 4_8
        echo -n "${MAJ0}_${MIN1}"
    fi
else
    # post 50:  just use the first #
    echo -n "${MAJ0}"
fi

exit 0
