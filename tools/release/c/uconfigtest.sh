#!/bin/sh
# Exhaust(ive, ing)  (Mean, Multi)  (Test, Trouble)
# Copyright (c) 2002-2006 IBM All Rights Reserved
#

# Builds ICU a whole lotta times and with different options
# Set the options below and execute this script with the shell.

# This script is checked into icuhtml/emt. It assumes that the
# icu directory is at the same level as the icuhtml directory.


#------------------- Find full path names  -----------------------
# Build root - <icuhtml>/emt - the location of this script
B=$(pwd)

# the runConfigureICU platform name
ICUPLATFORM=LinuxRedHat

# Global Config options to use
export COPTS=" --with-data-packaging=archive"

# Global testing options to use
export INTLTESTOPTS=-w 
export CINTLTEST_OPTS=-w
# --- Probably will not need to modify the following variables ---

# ICU directory is $B/../../icu
ICU=$(dirname $(dirname $B))/icu

# Source directory
S=$ICU/source

# ------------ End of config variables

# Prepare uconfig.h
UCONFIG_H=$S/common/unicode/uconfig.h
if grep -q myconfig.h  $UCONFIG_H ;
then
    echo "# $UCONFIG_H already contains our patch, no change"
else
    mv $UCONFIG_H ${UCONFIG_H}.orig
    cat > $UCONFIG_H  <<EOF
#if defined(IN_UCONFIGTEST)
#include "myconfig.h"
#endif
/* for uconfigtest.sh - you may REMOVE above this line */
/* ----------------------------------------------------------- */
EOF
cat ${UCONFIG_H}.orig >> ${UCONFIG_H}
    echo "# $UCONFIG_H updated"
fi


# Start,  set a default name to start with in case something goes wrong

export NAME=foo
mkdir -p ${B} ${B}/times 2>/dev/null

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
    echo " build to ${B}/${NAME} and install in ${B}/I${NAME} "
    echo
}

# Clean up the old tree before building again
clean()
{
    echo cleaning ${B}/${NAME} and ${B}/I${NAME}
    rm -rf ${B}/I${NAME} ${B}/${NAME}
    mkdir -p ${B}/${NAME}
}

# Run configure with the appropriate options (out of source build)
config()
{
    mkdir -p ${B}/${NAME} 2>/dev/null
    cd ${B}/${NAME}
    mkdir emtinc 2>/dev/null

    # myconfig.h
    cat > emtinc/myconfig.h <<EOF
// NAME=${NAME}
// UCONFIGS=${UCONFIGS}
// CPPFLAGS=${CPPFLAGS}
#ifndef _MYCONFIG_H
#define _MYCONFIG_H

EOF
    for what in `echo $UCONFIGS`;
    do
        echo "#define UCONFIG_${what} 1" >> emtinc/myconfig.h
    done
    cat >> emtinc/myconfig.h <<EOF
#endif
EOF
    CPPFLAGS="${CPPFLAGS} -DIN_UCONFIGTEST -I${B}/${NAME}/emtinc"
    echo "CPPFLAGS=\"$CPPFLAGS\" Configure $COPTS --srcdir=$S"
    $S/runConfigureICU ${ICUPLATFORM} $COPTS --prefix=${B}/I${NAME} --srcdir=$S 2>&1 > ${B}/${NAME}/config.out
}

# Do an actual build
bld()
{
##*##  Stream filter to put 'NAME: ' in front of
##*##  every line:
##*##      . . .   2>&1 | tee -a ./bld.log | sed -e "s/^/${NAME}: /"
    cd ${B}/${NAME}
    /usr/bin/time -o ${B}/times/${NAME}.all     make -k all                      
    /usr/bin/time -o ${B}/times/${NAME}.install make -k install                  
    /usr/bin/time -o ${B}/times/${NAME}.il      make -k install-local            
    /usr/bin/time -o ${B}/times/${NAME}.chk     make -k check INTLTEST_OPTS=-w CINTLTST_OPTS=-w
    PATH=${B}/I${NAME}/bin:$PATH make -C ${B}/${NAME}/test/hdrtst/  check    
}

# Do a complete cycle for a run
doit()
{
ban ; clean ; config ; bld
}

# Set up the variables for convenience
NO_COL="NO_COLLATION"
NO_BRK="NO_BREAK_ITERATION"
NO_FMT="NO_FORMATTING"
NO_UCM="NO_LEGACY_CONVERSION"
NO_CNV="NO_CONVERSION"
NO_XLT="NO_TRANSLITERATION"
NO_RGX="NO_REGULAR_EXPRESSIONS"
JS_COL="ONLY_COLLATION"
NO_NRM="NO_NORMALIZATION"
NO_IDN="NO_IDNA"
NO_SVC="NO_SERVICE"
NO_MST="$NO_COL $NO_BRK $NO_FMT $NO_UCM $NO_RGX $NO_XLT $NO_NRM $NO_IDN $NO_SVC"
NO_ALL="$NO_MST $NO_SVC"

# Now, come the actual test runs
# Each one sets a NAME, and CPPFLAGS or other flags, and calls doit

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
doit
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
# Currently, ICU will not 
# build w/ UCONFIG_NO_CONVERSION
# so this test is disabled.
######################
# NO_CNV
export NAME=NO_CNV
export UCONFIGS="$NO_CNV"
export CPPFLAGS=""
#doit
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

######################
# DEFAULT
export NAME=DEFAULT
export UCONFIGS=""
export CPPFLAGS=""
doit
######################


NAME=done
ban
echo "All builds finished! Times are in ${B}/times"
echo "There were errors if the following grep finds anything."
echo "grep status $B/times/*"
grep status $B/times/*

