#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
There are several tools in this directory that should make it easier to generate collation data:
extractCollationData.pl - perl script that reads ICU resource bundle files and outputs a locale_collation.html file if collation elements are present in the locale. Arguments are the list of locale source files (*.txt) that need to be processed.
createComparisonTables.pl - takes a locale name. Looks in directories that should contain the html data produced by colprobe or extractCollationData.
tableStarter.pl - invokes createComparisonTables.pl with a list of locales. 
