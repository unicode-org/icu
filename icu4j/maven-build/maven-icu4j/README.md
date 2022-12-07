<!--
© 2022 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# maven-icu4j

This directory exists as part of work to build ICU4J with Maven.
Specifically, this directory was created to exist alongside the code
directory structure without changing it.
This preserves the existing behavior of Ant build targets and BRS tasks / artifact
deploy processes based on the output of those Ant builds.

If / when the Ant build is suitable to be replaced and remove by Maven
or other such build tool, this directory should be removed.