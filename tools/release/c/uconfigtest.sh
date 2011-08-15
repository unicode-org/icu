#!/bin/sh
# Exhaust(ive, ing)  (Mean, Multi)  (Test, Trouble)
# Copyright (c) 2002-2011 IBM All Rights Reserved

# Builds ICU a whole lotta times and with different options
# Set the options below and execute this script with the shell.

# This script is checked into tools/trunk/release/c. It assumes that the
# icu directory is at the same level as the tools  directory. If this
# is not the case, use the uconfigtest.local file to set the
# SRC_DIR variable to point at the ICU source directory. 
# Or, alternatively, you can make a symlink tools/icu -> your_icu_dir
# You can also use the uconfigtest.local file to override the BUILD_DIR
# and ICUPLATFORM variables.

# It can be handy to run 'tail -F uconfigtest/stats' in another window to see where things are.


#------------------- Find full path names  -----------------------
JOPT=-j2

# check for uconfigtest.local
if [ -f ./uconfigtest.local ]
then
    . ./uconfigtest.local
fi

# location of this script
S=$(pwd)

# Build root - tools/release/c/uconfigtest
BUILD_DIR=${BUILD_DIR:-${S}/uconfigtest}

FAILS=${BUILD_DIR}/fails
STATS=${BUILD_DIR}/stats

>${FAILS}
>${STATS}
echo >> ${STATS}
echo >> ${STATS}

# the runConfigureICU platform name
ICUPLATFORM=${ICUPLATFORM:-Linux}

# Global Config options to use
#export COPTS=" --with-data-packaging=archive"
export COPTS=

# Global testing options to use
export INTLTESTOPTS=-w 
export CINTLTEST_OPTS=-w
# --- Probably will not need to modify the following variables ---

# ICU directory is $S/../../../icu
ICU=$(dirname $(dirname $(dirname ${S})))/icu

# Source directory
SRC_DIR=${SRC_DIR:-${ICU}/source}

# ------------ End of config variables
UCONFIG_H=$SRC_DIR/common/unicode/uconfig.h
UCONFIG_LOCAL_H=uconfig_local.h
UCONFIG_USE_LOCAL=UCONFIG_USE_LOCAL
# Prepare uconfig.h
if grep -q ${UCONFIG_LOCAL_H}  $UCONFIG_H ;
then
    echo "# $UCONFIG_H already contains our patch, no change"
else
    mv $UCONFIG_H ${UCONFIG_H}.orig
    cat > $UCONFIG_H  <<EOF
#if defined(${UCONFIG_USE_LOCAL})
#include ${UCONFIG_LOCAL_H}"
#endif
/* for uconfigtest.sh - you may REMOVE above this line */
/* ----------------------------------------------------------- */
EOF
cat ${UCONFIG_H}.orig >> ${UCONFIG_H}
    echo "# $UCONFIG_H updated"
fi


# Start,  set a default name to start with in case something goes wrong

export NAME=foo
mkdir -p ${BUILD_DIR} ${BUILD_DIR}/times 2>/dev/null

# Banner function - print a separator to split the output
ban()
{
    echo
    echo
    echo "#- -----------------------$NAME------------- -#"
    echo
    echo "CPPFLAGS = $CPPFLAGS"
    echo "UCONFIGS = $UCONFIGS"
    echo
    echo " build to ${BUILD_DIR}/${NAME} and install in ${BUILD_DIR}/I${NAME} "
    echo

    echo "${NAME} ---------------------------------------" >> ${STATS}
}

# Clean up the old tree before building again
clean()
{
    stats clean
    echo cleaning ${BUILD_DIR}/${NAME} and ${BUILD_DIR}/I${NAME}
    rm -rf ${BUILD_DIR}/I${NAME} ${BUILD_DIR}/${NAME}
    mkdir -p ${BUILD_DIR}/${NAME}
}

# Run configure with the appropriate options (out of source build)
config()
{
    stats config
    mkdir -p ${BUILD_DIR}/${NAME} 2>/dev/null
    cd ${BUILD_DIR}/${NAME}
    mkdir emtinc 2>/dev/null

    # myconfig.h
    cat > emtinc/${UCONFIG_LOCAL_H} <<EOF
/* NAME=${NAME}            */
/* UCONFIGS=${UCONFIGS}    */
/* CPPFLAGS=${CPPFLAGS}    */
#ifndef _MYCONFIG_H
#define _MYCONFIG_H

EOF
    for what in `echo $UCONFIGS`;
    do
        echo "#define UCONFIG_${what} 1" >> emtinc/${UCONFIG_LOCAL_H}
    done
    cat >> emtinc/${UCONFIG_LOCAL_H} <<EOF
#endif
EOF
    CPPFLAGS="${CPPFLAGS} -D${UCONFIG_USE_LOCAL} -I${BUILD_DIR}/${NAME}/emtinc"
    echo "CPPFLAGS=\"$CPPFLAGS\" Configure $COPTS --srcdir=$SRC_DIR"
    $SRC_DIR/runConfigureICU ${ICUPLATFORM} $COPTS --prefix=${BUILD_DIR}/I${NAME} --srcdir=$SRC_DIR 2>&1 > ${BUILD_DIR}/${NAME}/config.out
}

stats()
{
    STATUS="${NAME}: ${1} 	"`date`
    echo ${STATUS} >> ${STATS}
    echo "[1m*** ${NAME} ********* ${1} ************* [m"
}

fail()
{
    FAILURE="error: ${BUILD_DIR}/${NAME}: ${1} "`date`
    echo ${FAILURE} >> ${FAILS}
    echo ${FAILURE} >> ${STATS}
    echo "[7m${FAILURE}[m"
}

# Do an actual build
bld()
{
##*##  Stream filter to put 'NAME: ' in front of
##*##  every line:
##*##      . . .   2>&1 | tee -a ./bld.log | sed -e "s/^/${NAME}: /"
    cd ${BUILD_DIR}/${NAME}
    stats "make -k ${JOPT} all ${1}"
    /usr/bin/time -o ${BUILD_DIR}/times/${NAME}.all     make -k ${JOPT} all ${1}    DEPS=                  || fail make
    stats install
    /usr/bin/time -o ${BUILD_DIR}/times/${NAME}.install make -k install ${1}        DEPS=          INSTALL_DATA='ln -svf '  || fail install
    /usr/bin/time -o ${BUILD_DIR}/times/${NAME}.il      make -k install-local ${1}  DEPS=          || fail install-local
    stats check
    /usr/bin/time -o ${BUILD_DIR}/times/${NAME}.chk     make -k ${JOPT} check ${1} INTLTEST_OPTS=-w CINTLTST_OPTS=-w DEPS= || fail check
    stats hdrtst
    PATH=${BUILD_DIR}/I${NAME}/bin:$PATH make -k -C ${BUILD_DIR}/${NAME}/test/hdrtst/  DEPS= check    || fail hdrtst
}


# Do a complete cycle for a run
# arg: opts to build
doit()
{
ban ; clean ; config ; bld ${1}
}

# Set up the variables for convenience
NO_COL="NO_COLLATION"
NO_BRK="NO_BREAK_ITERATION"
NO_FMT="NO_FORMATTING"
NO_UCM="NO_LEGACY_CONVERSION"
# Since NO_CONVERSION is only meant to allow the common and i18n
# libraries to be built, we don't test this configuration.
#NO_CNV="NO_CONVERSION"
NO_FIO="NO_FILE_IO"
NO_XLT="NO_TRANSLITERATION"
NO_RGX="NO_REGULAR_EXPRESSIONS"
JS_COL="ONLY_COLLATION"
NO_NRM="NO_NORMALIZATION"
NO_IDN="NO_IDNA"
NO_SVC="NO_SERVICE"
NO_MST="$NO_COL $NO_BRK $NO_FMT $NO_UCM $NO_FIO $NO_RGX $NO_XLT $NO_NRM $NO_IDN $NO_SVC"
NO_ALL="$NO_MST $NO_SVC"

# Now, come the actual test runs
# Each one sets a NAME, and CPPFLAGS or other flags, and calls doit

######################
# DEFAULT
export NAME=DEFAULT
export UCONFIGS=""
export CPPFLAGS=""
doit
USE_PREBUILT_DATA="ICUDATA_SOURCE_ARCHIVE=`echo ${BUILD_DIR}/DEFAULT/data/out/tmp/*.dat`"
######################

######################
# NO_MST
export NAME=NO_MST
export UCONFIGS="$NO_MST"
export CPPFLAGS=""
doit
######################

######################
# NO_RGX
export NAME=NO_RGX
export UCONFIGS="$NO_RGX"
export CPPFLAGS=""
doit ${USE_PREBUILT_DATA}
######################

######################
# NO_COL
export NAME=NO_COL
export UCONFIGS="$NO_COL"
export CPPFLAGS=""
doit
######################

######################
# NO_BRK
export NAME=NO_BRK
export UCONFIGS="$NO_BRK"
export CPPFLAGS=""
doit
######################

######################
# NO_FMT
export NAME=NO_FMT
export UCONFIGS="$NO_FMT"
export CPPFLAGS=""
doit
######################

######################
# NO_UCM
export NAME=NO_UCM
export UCONFIGS="$NO_UCM"
export CPPFLAGS=""
doit
######################

######################
# NO_FIO
export NAME=NO_FIO
export UCONFIGS="$NO_FIO"
export CPPFLAGS=""
doit ${USE_PREBUILT_DATA}
######################

######################
# NO_XLT
export NAME=NO_XLT
export UCONFIGS="$NO_XLT"
export CPPFLAGS=""
doit
######################

######################
# NO_IDN
export NAME=NO_IDN
export UCONFIGS="$NO_IDN"
export CPPFLAGS=""
doit
######################

######################
# NO_NRM
export NAME=NO_NRM
export UCONFIGS="$NO_NRM"
export CPPFLAGS=""
doit
######################

######################
# NO_SVC
export NAME=NO_SVC
export UCONFIGS="$NO_SVC"
export CPPFLAGS=""
doit
######################

######################
# JS_COL
export NAME=JS_COL
export UCONFIGS="$JS_COL"
export CPPFLAGS=""
doit
######################

######################
# NO_ALL
export NAME=NO_ALL
export UCONFIGS="$NO_ALL"
export CPPFLAGS=""
doit
######################


NAME=done
ban
echo "All builds finished! Times are in ${BUILD_DIR}/times"
echo "There were errors if the following grep finds anything."
echo "grep status ${BUILD_DIR}/times/*"
grep status ${BUILD_DIR}/times/*

if [ -s ${FAILS} ];
then
    echo "Failures: "
    cat ${FAILS}
fi

