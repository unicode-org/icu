# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# Parses Unicode Character Database files and build ICU UCA data files.
#
# Requires: 1. run makeprops.sh  2. rebuild ICU & Unicode tools
# See (ICU)/source/data/unidata/changes.txt
#
# Invoke as
#   ./makeuca.sh path/to/ICU/src/tree path/to/ICU/build/tree
ICU_SRC=$1
ICU_BLD=$2
source ./makedefs.sh

$UNITOOLS_BLD/c/genuca/genuca -d $SRC_DATA_IN/coll -s $UNIDATA -i $BLD_DATA_FILES
