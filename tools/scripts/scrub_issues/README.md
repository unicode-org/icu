<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

 The file *scrub_issues.py* is a python program that accepts a path to the
directory '/icu/" of the ICU version being processed.

The program then executes a `grep` command on code in ICU4C and ICU4J branches,
finding lines with 'knownIssue' and 'TODO'.

Next, it uses the JIRA Python module to check the status of each of items 
found in the `grep` command.

The following Types of results are printed:
* non-numeric issue ids that are not short forms of a JIRA id
* issues with a JIRA number but that don't start with 'ICU-' or 'CLDR-'
* JIRA issues that are resolved
* unresolved JIRA issues found in code


Important: the "jira" module must be available to run this.
