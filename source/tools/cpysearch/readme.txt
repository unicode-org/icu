Copyright (c) 2002, International Business Machines Corporation and others. All Rights Reserved.


cpysearch.pl is a perl script used to detect the files that might not have the copyright notice. Best when used on windows on a clean checkout. Edit $icuSource to your path. If you are working on other platform, you probably want to edit $ignore to reflect different temporary files that you don't want in the scan. The result will be the list of files that don't have word copyright (case ignored) in first 10 lines. Look at them and fix if needed. 

Have fun!
weiv


