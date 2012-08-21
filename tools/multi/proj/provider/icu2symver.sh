#!/bin/sh
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.

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
    # pre 50:  paste together "4" and "8" to get 48
    echo -n "${MAJ0}${MIN1}"
else
    # post 50:  just use the first #
    echo -n "${MAJ0}"
fi

exit 0