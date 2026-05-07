#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2006-2008 IBM All Rights Reserved
#
# This test script enumerates all locales and all timezones installed on a
# machine (usually Linux), and runs the existing ICU4C tests to make sure that
# the tests pass. Not everyone is using and testing ICU4C in the en_US locale
# with the Pacific timezone.
top_icu_dir=../../../icu4c
release_tools_dir=../../../../tools/release/c
cd $top_icu_dir/source/test/intltest
$release_tools_dir/allLocaleTest.sh intltest &
$release_tools_dir/allTimezoneTest.sh intltest &
cd ../iotest
$release_tools_dir/allLocaleTest.sh iotest IOTEST_OPTS=iotest-c-loc.txt &
$release_tools_dir/allTimezoneTest.sh iotest IOTEST_OPTS=iotest-c-tz.txt &
cd ../cintltst
$release_tools_dir/allLocaleTest.sh cintltst &
$release_tools_dir/allTimezoneTest.sh cintltst &

echo "All tests have been spawned."
echo "Please wait while the tests run. This may take a while."
