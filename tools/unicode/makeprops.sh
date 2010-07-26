# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# Parses Unicode Character Database files and build ICU core properties files.
#
# Invoke as
#   ./makeprops.sh path/to/ICU/src/tree path/to/ICU/build/tree
#
# Prerequisite:
# In a Unicode version upgrade, pnames.icu must be generated first so that the
# tools here see new property names and property value names.
# See (ICU)/source/data/unidata/changes.txt.
ICU_SRC=$1
ICU_BLD=$2
source ./makedefs.sh

# uprops.icu
$UNITOOLS_BLD/c/genprops/genprops -d $SRC_DATA_IN      -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION
$UNITOOLS_BLD/c/genprops/genprops -d $COMMON --csource -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION

# ubidi.icu
$UNITOOLS_BLD/c/genbidi/genbidi -d $SRC_DATA_IN      -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION
$UNITOOLS_BLD/c/genbidi/genbidi -d $COMMON --csource -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION

# ucase.icu
$UNITOOLS_BLD/c/gencase/gencase -d $SRC_DATA_IN      -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION
$UNITOOLS_BLD/c/gencase/gencase -d $COMMON --csource -s $UNIDATA -i $BLD_DATA_FILES -u $UNICODE_VERSION

# unames.icu
$UNITOOLS_BLD/c/gennames/gennames -d $SRC_DATA_IN -1 -q $UNIDATA/UnicodeData.txt $UNIDATA/NameAliases.txt -u $UNICODE_VERSION

# unidata/norm2/*.txt
$UNITOOLS_BLD/c/gennorm/gennorm -d $UNIDATA/norm2 -s $UNIDATA -i $BLD_DATA_FILES

# *.nrm
export LD_LIBRARY_PATH=$ICU_BLD/lib
$ICU_BLD/bin/gennorm2 -o $SRC_DATA_IN/nfc.nrm     -s $UNIDATA/norm2 nfc.txt              -u $UNICODE_VERSION
$ICU_BLD/bin/gennorm2 -o $SRC_DATA_IN/nfkc.nrm    -s $UNIDATA/norm2 nfkc.txt             -u $UNICODE_VERSION
$ICU_BLD/bin/gennorm2 -o $SRC_DATA_IN/nfkc_cf.nrm -s $UNIDATA/norm2 nfkc.txt nfkc_cf.txt -u $UNICODE_VERSION
$ICU_BLD/bin/gennorm2 -o $SRC_DATA_IN/uts46.nrm   -s $UNIDATA/norm2 nfc.txt uts46.txt    -u $UNICODE_VERSION

# Let genuca see the latest normalization data.
cp $SRC_DATA_IN/nfc.nrm $BLD_DATA_FILES

# UCA
$UNITOOLS_BLD/c/genuca/genuca -d $SRC_DATA_IN/coll -s $UNIDATA -i $BLD_DATA_FILES
# If this is the first pass through rebuilding ICU data files,
# then genuca has not yet seen the new core properties (e.g., case mappings)
# from genprops --csource. Rebuild ICU and then the UCA data (makeuca.sh).
# See (ICU)/source/data/unidata/changes.txt.
