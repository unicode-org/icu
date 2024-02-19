#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#   Copyright (C) 2006-2008, International Business Machines
#   Corporation and others.  All Rights Reserved.
#

These tests allow you to see how timezones, locales, charsets and other similar stuff are mapped from the OS values to ICU specific values.

Some tweaking may be required to get these tools to work.

If the charset maps to US-ASCII, it may be an indication that the alias or mapping table do not exist in ICU.
If the locale doesn't exist, the test will state that the locale is not available.
If the timezone doesn't match, it will be listed as a difference.