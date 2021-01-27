---
layout: default
title: Unicode Update
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 130
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Unicode Update
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

*[This page's last thorough update was on [5 August
2010](https://sites.google.com/site/icusite/system/app/pages/admin/revisions?wuid=wuid:gx:6c0edddbaea77d12).]*
<!-- TODO: move into release/tasks/ if that's where it belongs? Else update the nav info. -->

The International Components for Unicode (ICU) implement the Unicode Standard
and many of its Standard Annexes faithfully and are updated to a new Unicode
version soon after its release. Usually, the ICU team participates in the
Unicode beta process by updating to a new Unicode version in a branch and
testing it thoroughly. In the past, this has uncovered problems that could be
fixed before the release of the new Unicode version.

One notable exception to ICU implementing all of Unicode is that it does not
provide any access to Unihan data (mostly because of low demand and the large
size of the Unihan data).

## Update process

For the last several updates, there is a [change log
here](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/unidata/changes.txt).
In short, most of the ucd .txt files are copied into the ICU repository, either
without modification or, for some files, with comments removed and lines merged
to reduce their size.

Some of the data files are not part of the Unicode release but are output from
Mark's Unicode tools. See <http://sites.google.com/site/unicodetools/>

Note: We have looked at using the [UCD
XML](http://www.unicode.org/ucd/#UCDinXML) files, but did not want to rely on
them alone until there was a way to verify that they contain precisely the same
data as the .txt files. Also, using the XML files would require a partial
rewrite of the existing tools. (There was an outdated, experimental, partial UCD
XML parser here:
<https://github.com/unicode-org/icu-docs/tree/main/design/properties/genudata>)

The ICU Unicode tools parse the text files, process the data somewhat, and write
binary data for runtime use. Most of these tools live in a [source
tree](https://github.com/unicode-org/icu/tree/main/tools/unicode) separate
from the ICU4C/ICU4J sources, and link with ICU4C.

The following steps are necessarily manual:

*   New property values and properties need to be reviewed.
*   For new property values, enum constants are added to the API.
*   For new properties, APIs are added and the tools are modified to write the
    additional data into new fields in the data structures; sometimes new data
    structures need to be developed for new properties.
*   Some new properties are not exposed via simple, direct data access APIs but
    in more high-level APIs (like case mapping and normalization functions).
*   Sometimes changes in which property aliases are canonical vs. actual aliases
    require manual changes to helper files or tools.

New properties (whether they are supported via dedicated API or not) should be
added to the [Properties User Guide
chapter](http://userguide.icu-project.org/strings/properties).

## Testing

The ICU test suites include some tests for Unicode data. Some just check the
data from the API against the original .txt files. Some tests simply check for
certain hardcoded values, which have to be updated when those values change
deliberately. Other tests perform consistency checks between some properties, or
between different implementations.

There is a program as a part of CLDR that uses regular expressions to test the
segmentation rules and properties (LineBreak, WordBreak, etc). That is, there is
a regular expression corresponding to each of the rules, and a brute force
evaluation of them. That is used to generate the tables and test data. The
segmentation rules in ICU are later modified by hand to match the
specifications. That has to be done by hand, because there are some areas where
the rules don't correspond 1:1 with the spec. There are a series of ICU
consistency tests for those rules. ICU also includes regression tests with
"golden files" that are used to detect unanticipated side effects of revisions
to the rules.
