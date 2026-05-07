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

The International Components for Unicode (ICU) implement the Unicode Standard
and many of its Standard Annexes and Technical Standards,
and are updated to each new Unicode version.
Usually, the ICU team participates in the Unicode beta process by
updating to a beta snapshot of the new Unicode version and testing it thoroughly.
In the past, this has sometimes uncovered problems that could be
fixed before the release of the new Unicode version.

(Note that ICU does not provide any access to Unihan data,
mostly because of low demand and the large size of the Unihan data.)

## Update process

For the last several updates, there is a
[change log for Unicode updates](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/unidata/changes.txt).

For each new Unicode version, during the beta period,
*   Copy the change log for the previous version to the top of this file.
*   Adjust the versions, tickets, URLs, and paths.
*   Work through the steps listed in the log, top to bottom, adjusting the log as necessary.
*   Report problems to the UTC and/or CLDR and/or ICU.

Before the data is final, “turn the crank” several more times,
using appropriate subsets of the steps.

At the start of the process, most of the Unicode data files are copied into the ICU repository, either
without modification or, for some files, with comments removed and lines merged
to reduce their size.

Some of the data files are not part of the Unicode release but are output from
various Unicode Tools, as noted in the change log.
(See also https://github.com/unicode-org/unicodetools)

Note: We have looked at using the [UCD XML](https://www.unicode.org/ucd/#UCDinXML) files,
but decided against it and instead developed a simpler format for a combined Unicode data file.
See https://icu.unicode.org/design/props/ppucd#TOC-Why-not-UCD-XML-files-
(There was an outdated, experimental, partial UCD XML parser here:
<https://github.com/unicode-org/icu-docs/tree/main/design/properties/genudata>)

The ICU Unicode tools parse the text files, process the data somewhat, and write
binary data for runtime use. Most of these tools live in a
[source tree](https://github.com/unicode-org/icu/tree/main/tools/unicode) separate
from the ICU4C/ICU4J sources, and link with ICU4C.

The following steps are necessarily manual:

*   New property values and properties need to be reviewed.
*   For new property values, enum constants are added to the API.
*   For new properties, APIs are added and the tools are modified to write the
    additional data into new fields in the data structures; sometimes new data
    structures need to be developed for new properties.
*   Some properties are not exposed via simple, direct data access APIs but
    in more high-level APIs (like case mapping and normalization functions).
*   Sometimes changes in which property aliases are canonical vs. actual aliases
    require manual changes to helper files or tools.

New properties (whether they are supported via dedicated API or not) should be added to the
[Properties User Guide chapter](https://unicode-org.github.io/icu/userguide/strings/properties).

### Bazel build process

The tools for building ICU data for Unicode properties are in a separate subtree of the ICU repo.
They depend on parts of the ICU libraries and generate files that go back into the source tree
in order to make updated properties available to higher-level parts of the library and tools.

In the past, we boot-strapped this by doing a `make install` on ICU with the old data,
using cmake to build the tools, running some of the tools with their output
going back into the source tree, rebuilding ICU and the tools, running more tools, etc.

This was very manual and cumbersome.

Instead, starting with ICU 70 (2021),
we now use the [Bazel build system](https://bazel.build/) to build only small parts of the libraries,
just enough to build and run the initial tools.
We still need a layer outside of Bazel in order to copy the tool output into the source tree,
because Bazel on its own does not allow modifying the source tree.
We use a shell script to automate alternately building tools and copying files.

This simplifies the process.

It should also make it much easier to customize Unicode properties,
for example by patching ppucd.txt with real properties for PUA (private use) characters.

Finally, it should make it easier to modify the binary data file format for a property
because we build the library code that depends on the data only after generating that data.

For the initial setup of this Bazel build system for ICU see
https://unicode-org.atlassian.net/browse/ICU-21117 “sane build system for Unicode data”

This was completed while working on
https://unicode-org.atlassian.net/browse/ICU-21635 “Unicode 14”

#### Bazel setup

It should be possible to run the `bazel` command directly,
but the Bazel team recommends using the `bazelisk` wrapper.
It downloads and runs the latest version of Bazel, or,
if the root folder contains a .bazelisk file with an entry like
```
USE_BAZEL_VERSION=3.7.1
```
then it downloads that specific version. If there are any incompatible changes in Bazel behavior,
then this insulates us from those.

We do have an $ICU_SRC/.bazeliskrc file with such a line.
Consider running `bazelisk --version` outside of the $ICU_SRC folder
to find out the latest `bazel` version, and copying that version number into the config file.
(Revert if you find incompatibilities, or, better, update our build & config files.)

Right in $ICU_SRC we also have a file called WORKSPACE which tells Bazel that
our repo root is also the root of its build system.
We build library “targets” relative to that. For example,
`//icu4c/source/common:normalizer2` refers to the cc_library named `normalizer2` in
$ICU_SRC/icu4c/source/common/BUILD .

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
