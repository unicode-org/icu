#!/bin/sh
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.

# $@
OUT=$1
shift
# $*
VER=$1
shift

TINY=`./icu2symver.sh $VER`

echo "$0: Building ${OUT} for ${TINY} ------- " >&2
#set -x 

(
    cat ${GLUE}/gluren-top.h
    echo "#define GLUREN_VER" ${TINY}
    echo "#define GLUREN_TINY" ${TINY}
    echo
    echo '/* old style (<4.4)*/'
    grep "^#define.*${TINY}$" ${SRC}/${VER}/${SOURCE}/common/unicode/urename.h   | fgrep -v '*' | sed -e "s@^#define \([^ ]*\) \([^ ]*\)@#define OICU_\1 \2@"
    echo '/* new style (4.4+) */'
    fgrep " U_ICU_ENTRY_POINT_RENAME(" ${SRC}/${VER}/${SOURCE}/common/unicode/urename.h | sed -e "s@^#define \([^ ]*\) .*@#define OICU_\1 \1_${TINY}@"
    cat ${GLUE}/gluren-bottom.h
) |
    cat > ${OUT}
#    tee ${OUT}

