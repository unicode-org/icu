#!/bin/bash
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.

if [ $# -eq 1 ];
then
    VERBOSE=1
else
    VERBOSE=0
fi

function verbose()
{
    if [ ${VERBOSE} -eq 1 ];
    then
        echo "$*"
    else
        echo -n .
    fi
}

function c()
{
    IN=$1
    EXP=$2
    OUT=`./icu2symver.sh $IN`
    if [ "x${OUT}" != "x${EXP}" ];
    then
        echo "Error: \"${IN}\" -> \"${OUT}\", expected ${EXP}" >&2
        exit 1
    else
        verbose "${IN} -> ${OUT}"
    fi

    OUT=`echo ${IN} | ./icu2symver.sh`
    if [ "x${OUT}" != "x${EXP}" ];
    then
        echo "Error: \"${IN}\" -> \"${OUT}\", expected ${EXP} (via stream)" >&2
        exit 1
    else
        verbose "${IN} -> ${OUT} (via stream)"
    fi
}

c	'3.6.2'		'36'
c	'1.0'		'10'
c	'4.8'		'48'
c	'4.8.1.1'	'48'
c	'4.0.2'		'40'
c	'4.1.2'		'41'
c	'49.1.2'	'49'
c	'49'		'49'
c	'50.0.3'	'50'
c	'51.0.0.1'	'51'

echo " OK!"

exit 0
