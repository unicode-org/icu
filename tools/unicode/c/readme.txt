#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun04
# created by: Markus W. Scherer

These tools parse Unicode Character Database files and generate
data files (text, source code and binary) for use in ICU.
They are used during the Unicode beta period and after a Unicode release.

For a log of actions and changes for recent Unicode version upgrades of ICU, see
http://bugs.icu-project.org/trac/browser/icu/trunk/source/data/unidata/changes.txt

Since the Unicode 5.2 upgrade, these tools have been moved out of the ICU source
tree to here. They have not been fully tested and probably need some more work
and setup. They might benefit from additional shell or Python scripts.

There are autoconf makefiles (Makefile.in) and Visual C++ project files (.vcproj)
in the subfolders. They are copied over from the ICU source tree and will not
work without modifications. However, I started to use CMake (CMakeLists.txt)
which is much simpler, and if it works well enough then I plan to just
delete the old makefiles and project files. The CMake files should
work on Linux and MacOS X.
I should use more variables to make the CMake files more portable, and should
use ICU's installed icu-config or Makefile.inc to get the values for these
variables.
(If and when ICU itself uses CMake, we should be able to point to its modules.)

Things will improve as I work on Unicode 6...

markus
