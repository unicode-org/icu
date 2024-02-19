#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.

# $@
OUT=$1
shift
# $*
VER=$1
shift

TINY=`./icu2symver.sh $VER`
OLDSYM=`./icu2symver.sh --pre44sym $VER`

echo "$0: Building ${OUT} for ${TINY} ------- " >&2
echo "oldsym = ${OLDSYM}"
#set -x 
URENAME=${SRC}/${VER}/${SOURCE}/common/unicode/urename.h
(
    cat ${GLUE}/gluren-top.h
    echo "/* Generated from ${URENAME} by ${0} */"
    echo "#define GLUREN_VER" ${TINY}
    echo "#define GLUREN_TINY" ${TINY}
    echo
    echo '/* old style (<4.4)*/'
    grep "^#define.*${OLDSYM}$" ${URENAME}   | fgrep -v '*' | sed -e "s@^#define \([^ ]*\) \([^ ]*\)@#define OICU_\1 \2@"
    echo '/* new style (4.4+) */'
    fgrep " U_ICU_ENTRY_POINT_RENAME(" ${URENAME} | sed -e "s@^#define \([^ ]*\) .*@#define OICU_\1 \1_${TINY}@"
    cat ${GLUE}/gluren-bottom.h
) |
    cat > ${OUT}
#    tee ${OUT}

