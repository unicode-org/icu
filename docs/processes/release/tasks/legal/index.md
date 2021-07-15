---
layout: default
title: Legalities
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 70
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Legalities

## 1. Scan for Copyright notices

Check source and data files, especially newly contributed ones, to make sure the
proper copyright notice is in place. For example,

```
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html#License
```

Scan the source code to make sure that every file that was touched recently has
the current year in the copyright statement. See the [ICU Copyright
Scanner](../../../copyright-scan.md) page and follow the link to the scripts and
the readme.

Scan the source code to include third-party company names in copyright notices
if necessary.

~~Scan for text files that do not contain the word "Copyright": find_textfiles
-nosvn -novc -noeclipse | xargs grep -i -L Copyright (See the find_textfiles
Perl script attached to this page.) There are files without word "Copyright" in
ICU source repository including some test data files (no comment syntax defined
for these test data files), Unicode data files, tzcode source files and others.
Review the output file list and determine if each of them should have ICU
copyright statement or not.~~ ***The script [find_textfiles](find_textfiles)
associated with this document is not maintained. Use the ICU Copyright Scanner
above instead.***

## 2. Update license files

Check ICU, Unicode and other license terms. Make sure these files are up to
date. The [Unicode data and software license
term](http://www.unicode.org/copyright.html) is updated annually (usually year
number only). The easiest way to get the updated license is to do View Source on
unicode.org/copyright.html and scroll down to the plaintext version of the
software license ("Exhibit A").

See [svn changeset r39632](https://github.com/unicode-org/icu/commit/0001f6c5e92f6f3a8d66c7dbc47cc24df7633a71)
for an example; there should be only two files to update.
