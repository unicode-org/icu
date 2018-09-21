#!/bin/sh
# © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html#License

BASE="$(pwd)/$(dirname $0)"
ICU4J_ROOT="${BASE}/../.."
ICU4C_ROOT="${ICU4J_ROOT}/../icu4c"
OUTDIR="${ICU4J_ROOT}/out"
ICU4C_BUILD="${OUTDIR}/icu_build"
# set this to your preferred make
MAKE=${MAKE:-make}
# set this for multi core builds
CORES=${CORES:-1}
mkdir -vp "${ICU4C_BUILD}" || exit 1
cd "${ICU4C_BUILD}" || exit 1
echo "Building in ${ICU4C_BUILD} - if this fails, try delete that directory and trying again"
( "${ICU4C_ROOT}/source/configure" ${CONFIG_OPTS} && ${MAKE} -j ${CORES} icu4j-data-install ) || exit 1
exec ls -lh ${ICU4J_ROOT}/main/shared/data/*.jar
