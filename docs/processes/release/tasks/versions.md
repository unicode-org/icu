---
layout: default
title: Version Numbers
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 140
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Version Numbers
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## ICU Version Number

For reference, see the section in the User Guide about Version numbers here:

<https://unicode-org.github.io/icu/userguide/icu/design#version-numbers-in-icu>

### ICU Version Number for Front load, RC and GA tasks

The process of releasing a new ICU version (E.G. ICU 70.1) is divided in three phases:
* Front loading tasks
* Release Candidate (RC) tasks
* General Availability (GA) tasks

As of ICU 70, the ICU Version number changes for each of these tasks.

When "front loading" tasks, the version number will consist of a Major number, Minor number, and a Patch number. 
For example: ICU version 70.0.1

For the RC and GA tasks, the ICU version number will consist of a Major number and a Minor number.
For example: ICU version 70.1

This means that when updating from the front load tasks to the RC tasks, files such as 
[icu4c/source/common/unicode/uvernum.h](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/unicode/uvernum.h)
need to be correspondingly updated. See below for more files to be updated and steps to be followed.


### ICU Data

[icu4c/source/data/misc/icuver.txt](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/misc/icuver.txt)
needs to be updated with the correct version number for ICU and its data.

#### Since ICU 68

In
[tools/cldr/cldr-to-icu/build-icu-data.xml](https://github.com/unicode-org/icu/blob/main/tools/cldr/cldr-to-icu/build-icu-data.xml)
edit `<property name="icuVersion" value="67.1.0.0"/>` and `<property
name="icuDataVersion" value="67.1.0.0"/>`.

The CLDR-to-ICU converter will then generate icuver.txt with the new version
numbers.

Run the converter, or coordinate with the person who will run it.

#### Until ICU 67 (inclusive)

Edit icuver.txt directly.

### ICU4C

1.  The instructions for updating the version number are in
    [icu4c/source/common/unicode/uvernum.h](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/unicode/uvernum.h).
2.  [icu4c/source/data/makedata.mak](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/makedata.mak)
    also needs to be updated with the correct version for `U_ICUDATA_NAME` (icudt).
3.  After `uvernum.h` is updated, the file [Build.Windows.IcuVersion.props](https://github.com/unicode-org/icu/blob/main/icu4c/source/allinone/Build.Windows.IcuVersion.props) should be updated.
    This can be done by hand, or by running the UNIX makefile target '**update-windows-makefiles**'
    in `icu4c/source`.
    *   You will need to rerun "`./configure`" first though before you can run the
        command "`make update-windows-makefiles`".
    *   Note: You can use MSYS+MinGW to run the UNIX makefile on Windows
        platforms as well.
4.  As well, the ICU4C "configure" script should be updated so that it reflects
    the latest version number.
    *   Note: When updating the version number in the configure files be careful
        not to just blindly search-and-replace the version number.
        For example, there are lines like this: "As of ICU 62, both C and C++
        files require them" which need to have the exact version number
        retained.

### ICU4J

Since ICU4J 4.6, you can quickly check the current version information by
running jar main in icu4j.jar. For example,

```sh
$ java -jar icu4j.jar
```

prints out -

<pre>
International Component for Unicode for Java 4.8
Implementation Version: 4.8
Unicode Data Version: 6.0
CLDR Data Version: 2.0
Time Zone Data Version: 2011g
</pre>

For updating ICU version numbers, follow the steps below.

1. [icu4j/main/shared/build/common.properties](https://github.com/unicode-org/icu/blob/main/icu4j/main/shared/build/common.properties)

    *   icu4j.spec.version: This is API spec version, therefore, 2-digit major
        version only. The version number won't be changed for maintenance
        releases. (e.g. "55")
    *   icu4j.impl.version: This version is used for the actual release version,
        including maintenance release numbers. (e.g. "55.1"). Note: We do not
        use .0 - For example, ICU4J 54.1 uses "54.1", instead of "54.1.0" For
        milestone 1, use <major>.0.1 (e.g. 60.0.1).
    *   icu4j.data.version: This version number is corresponding to the data
        structure, therefore, won't be changed in maintenance releases. Use
        2-digit number used by the data path. (e.g. "55")
    *   \[*Not applicable for ICU4J 60+*\] current.year: Some build script embed
        the year taken from this into a copyright template. Make sure this value
        is updated to the current year for a new release (also applicable to a
        maintenance release).

2. icu4j/build.properties (For API change report and release target)

    *   api.report.version: 2 digit release number. Note: If necessary, we may
        include maintenance release number. (e.g "54", "481")
    *   api.report.prev.version: The previous version compared against the current
        version. (e.g. "46")
    *   release.file.ver: This string is used for files names. For milestone
        releases, use <2-digit major version> + "m" + <milestone number>, e.g.
        "55m1". For release candidate, use <2-digit major version> + "rc", e.g.
        54rc. For official releases, use full version numbers using under bar as the
        separator, e.g. "54_1", "54_1_1".
    *   api.doc.version: The version displayed in API reference doc - use full
        version number such as "60.1" for official and RC releases, "60 Milestone 1" for
        milestone 1.
    *   maven.pom.ver: The version used in ICU pom.xml files. Use full version
        number such as "60.1" for official releases, "61.1-SNAPSHOT" until 61.1
        release, after 60.1.

3. [icu4j/main/core/src/main/java/com/ibm/icu/util/VersionInfo.java](https://github.com/unicode-org/icu/blob/main/icu4j/main/core/src/main/java/com/ibm/icu/util/VersionInfo.java)

    There is a static block starting at line 501 (as of 54.1) in the source file -

    <pre>
    /**
     * Initialize versions only after MAP_ has been created
     */
    static {
        UNICODE_1_0 = getInstance(1, 0, 0, 0);
        UNICODE_1_0_1 = getInstance(1, 0, 1, 0);
        UNICODE_1_1_0 = getInstance(1, 1, 0, 0);
        UNICODE_1_1_5 = getInstance(1, 1, 5, 0);
        UNICODE_2_0 = getInstance(2, 0, 0, 0);
        UNICODE_2_1_2 = getInstance(2, 1, 2, 0);
        UNICODE_2_1_5 = getInstance(2, 1, 5, 0);
        UNICODE_2_1_8 = getInstance(2, 1, 8, 0);
        UNICODE_2_1_9 = getInstance(2, 1, 9, 0);
        UNICODE_3_0 = getInstance(3, 0, 0, 0);
        UNICODE_3_0_1 = getInstance(3, 0, 1, 0);
        UNICODE_3_1_0 = getInstance(3, 1, 0, 0);
        UNICODE_3_1_1 = getInstance(3, 1, 1, 0);
        UNICODE_3_2 = getInstance(3, 2, 0, 0);
        UNICODE_4_0 = getInstance(4, 0, 0, 0);
        UNICODE_4_0_1 = getInstance(4, 0, 1, 0);
        UNICODE_4_1 = getInstance(4, 1, 0, 0);
        UNICODE_5_0 = getInstance(5, 0, 0, 0);
        UNICODE_5_1 = getInstance(5, 1, 0, 0);
        UNICODE_5_2 = getInstance(5, 2, 0, 0);
        UNICODE_6_0 = getInstance(6, 0, 0, 0);
        UNICODE_6_1 = getInstance(6, 1, 0, 0);
        UNICODE_6_2 = getInstance(6, 2, 0, 0);
        UNICODE_6_3 = getInstance(6, 3, 0, 0);
        UNICODE_7_0 = getInstance(7, 0, 0, 0);
    <b>
        ICU_VERSION = getInstance(54, 1, 0, 0);
        ICU_DATA_VERSION = ICU_VERSION;</b>
        UNICODE_VERSION = UNICODE_7_0;

        UCOL_RUNTIME_VERSION = getInstance(8);
        UCOL_BUILDER_VERSION = getInstance(9);
        UCOL_TAILORINGS_VERSION = getInstance(1);
    }
    </pre>

    In the same file, starting at line 164 (as of 54.1) -

    <pre>
    /**
     * Data version string for ICU's internal data.
     * Used for appending to data path (e.g. icudt43b)
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final String <b>ICU_DATA_VERSION_PATH = "54b";</b>
    </pre>

4. [icu4j/pom.xml](http://source.icu-project.org/repos/icu/trunk/icu4j/pom.xml)
    (before ICU4J 60 only)

    <pre>
    <groupId>com.ibm.icu</groupId>
    <artifactId>icu4j</artifactId>
    <version><b>55-SNAPSHOT</b></version>
    <name>ICU4J</name>
    </pre>

    Only for the final release (including maintenance release), update the <version>
    item to the actual release version (e.g. "54.1", "4.8.1") Otherwise, use (next
    ver)-SNAPSHOT. (e.g. "55-SNAPSHOT").

5. Time Bombs (before ICU4J 52)

    ***Note: We no longer use time bombs since ICU4J 52. In the trunk,
    logKnownIssue() is used for skipping known test failures. The new scheme no
    longer depends on the current ICU4J version, so there are no updates
    necessary in test codes when version number is updated. See [Skipping Known
    Test Failures](../../../setup/eclipse/time.md) for more details.***

    There might be some test cases intentionally skipped for the current ICU4J
    version. When ICU4J version is updated, these time bombed test cases may
    show up. In this case, you should:

    *   Inform the list of failing test cases because of the version change to
        icu-core ML - ask if someone has plan to fix them.
    *   File a ticket if a time bomb does not have any corresponding Jira ticket
        and put the ticket number as comment
    *   Move the time bomb version to the next milestone.

    The time bomb looks like below -

    <pre>
    if (<b>isICUVersionBefore(49, 1)</b>) { // ICU-6806
        logln(showOrderComparison(uLocale, style1, style2, order1, order2));
    } else {
        errln(showOrderComparison(uLocale, style1, style2, order1, order2));
    }
    </pre>

    Note: ICU4J time bomb - Before
    [ICU-7973](https://unicode-org.atlassian.net/browse/ICU-7973), we used to
    use skipIfBeforeICU(int,int,int).

    When a test case with time bomb still fails before a major release, the time
    bomb may be moved to the version before the first milestone of the next
    major release stream. For example, the time bomb (49,1) is not yet resolved
    before ICU4J 49 release, it should be updated to (50,0,1). This will prevent
    the error test case showing up during 49 maintenance releases, and appear
    before the first milestone of 50.0.1.

6. ICU4J Eclipse plug-in version (Eclipse release only)

   ICU4J Eclipse plug-in use the standard Eclipse versioning scheme -
   X.Y.Z.v<build date>, for example, com.ibm.icu_4.2.1.v20100412.jar. By
   default, the build script compose the version string from
   icu4j.plugin.impl.version.string property in
   [eclipse-build/build.properties](https://github.com/unicode-org/icu/blob/main/icu4j/eclipse-build/build.properties)
   with current date at the build time. However, when we tag a version, we want
   to freeze the build date part. To force a fixed version string, we add a
   property - icu4j.eclipse.build.version.string in the build.properties. For
   example, see
   [tags/release-4-4-2-eclipse37-20110208/eclipse-build/build.properties](http://source.icu-project.org/repos/icu/tags/icu4j/release-4-4-2-eclipse37-20110208/eclipse-build/build.properties).

7. [DebugUtilitiesData.java](https://github.com/unicode-org/icu/blob/main/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/util/DebugUtilities.java)

    This file is automatically generated when data is generated for ICU4J. The
    ICU4C version number string should be check to ensure that the correct
    version number is being used. public static final String
    ICU4C_VERSION="50.0.2"; Note: The ICU4C version number string in this JAVA
    file is not really used for anything except for a few logln method calls.
    Perhaps this member is not really needed.

## Data Versions

Make sure data file versions (for **data contents**) are properly assigned.

If any of the data files in the
[/icu/source/data/](https://github.com/unicode-org/icu/tree/main/icu4c/source/data)
directory has changed **MANUALLY**, upgrade the version number accordingly as
well. If the contents of a resource bundle has changed, then increase the
version number (at least the minor version field). The CLDR generated data
should have the correct number. **Note** from Markus (20090514, ICU 4.2
timeframe): Most data files automatically get their version numbers set by the
LDML2ICUConverter, from CLDR version numbers. It is not clear what, if anything,
needs to be done for this task.

The ICU4J data files depend on the ICU4C data files. Thus, in order to
regenerate the updated ICU4J data files, you will first need to update the ICU4C
data files version.

Once the ICU4C data files are updated, follow the instructions in the Readme
file here:
<https://github.com/unicode-org/icu/blob/main/icu4c/source/data/icu4j-readme.txt>

## Data File *Format* Versions

Make sure data file **format** versions are updated. See
<https://unicode-org.github.io/icu/userguide/icu_data/#icu-data-file-formats>

For Unicode data files, it is also useful to look at recent tools-tree changes:
[icu/commits/main/tools/unicode](https://github.com/unicode-org/icu/commits/main/tools/unicode)

If the format of a binary data file has changed, upgrade the format version in
the UDataInfo header for that file. Better: Change the format version
immediately when the format is changed. The change must be made in both the
producer/generator and consumer/runtime code.

It is desirable to maintain backward compatibility, but sometimes impractical.
Update the major version number for incompatible changes. Runtime code should
permit higher minor version numbers for supported major versions.

We rarely use the third and fourth version number fields, except for UTrie (only
version 1, not UTrie2) parameters that don't really change.
