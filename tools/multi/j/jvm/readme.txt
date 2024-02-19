#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved

Put '.sh' files in this dir , locally, to be used for Java version bringup
See addjava below

ex:  '1_6.sh'
------
JAVA_HOME=/somewhere/1_6
#JAVA=java
#CLASSPATH=foo/bar.jar:/baz
#VM_OPTS=-Xmx265
-----


You can use the addjava.sh tool, like this - give it the full path to 'java':

 $    addjava.sh  /opt/IBM-JDK-1.5/bin/java
 Created  1_5.sh

IF 1_5.sh alreday exists, a random number will be added to the name of the new file.

