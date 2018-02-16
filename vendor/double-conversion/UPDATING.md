© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

## Updating double-conversion from Github to Vendor

Go to https://github.com/google/double-conversion/releases/latest/ to determine the latest version number; for the examples below, suppose it is `v3.0.0`.

Run `pull-from-upstream.sh` as below:

	./pull-from-upstream.sh v3.0.0

## Updating double-conversion from Vendor to ICU4C

The relevant files from double-conversion are copied (svn cp) from Vendor to icu4c/source/i18n.  In order to integrate changes from a new version of double-conversion, the changes in Vendor after updating from Github should be merged into their corresponding copies in ICU4C.

Instructions on how to do this should be written by the first person who performs such an update.

For reference, the original commit including all of the svn copies is here:

http://bugs.icu-project.org/trac/changeset/40929
