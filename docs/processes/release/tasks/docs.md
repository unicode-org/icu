---
layout: default
title: APIs & Docs
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 20
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# APIs & Docs
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Update API status comments

### ICU4C

1.  **Note: for ICU4C 49m2 or later, will require Doxygen 1.7.5.1 or later (
    see** [ICU-8862](https://unicode-org.atlassian.net/browse/ICU-8862) **).**
    On Linux with debian style package management,

    `sudo apt-get install doxygen`

    Or, to install manually
    1.  To update Doxygen, first [download binary or source release
        here](http://doxygen.org/download.html#latestsrc)
    2.  Unpack the release, and run something like<br>
        "`configure --prefix /usr/local`" (to install into /usr/local/bin etc)<br>
        (Note for the binary release, 'configure' just builds an installer.)
    3.  Run "`make install`" with appropriate permission (perhaps
        "`sudo make install`".)
    4.  Verify that '`doxygen --version`' gives the correct number.

2.  Update the API documentation in all header files (.h file) to have correct
    @draft/@stable/@deprecated labels.
3.  Update docmain.h
4.  ./configure
5.  make doc
6.  Follow instructions in
    [tools/release/java/readme.txt](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/main/tools/release/java/readme.txt)
    to generate API status change report.
7.  Make sure that ICU headers work with U_HIDE_DRAFT_API and other such
    switches.
8.  Verify that U_DRAFT and U_STABLE match the @draft and @stable tags (same for
    other such pairs declaration macro vs. tag).
    1.  For example, on Linux:
    2.  grep --recursive --include="\*.h" --exclude-dir=".svn" -e "@deprecated"
        -A 5 . > at-deprecated.txt
    3.  grep --recursive --include="\*.h" --exclude-dir=".svn" -e "U_DEPRECATED"
        -B 5 -A 1 . > u_deprecated.txt
    4.  Do this for draft, deprecated, obsolete, internal.
    5.  For @stable, we either verify by cleaning up all others or we need to
        write a script.

### ICU4J

Update the API documentation to have correct @draft/@stable/@deprecated labels.
See the [User Guide, ICU Architectural Design, ICU API
compatibility](https://unicode-org.github.io/icu/userguide/icu/design#icu-api-compatibility).

On ICU4J, run
[com.ibm.icu.dev.tool.docs.CheckTags](https://github.com/unicode-org/icu/blob/main/icu4j/tools/build/src/com/ibm/icu/dev/tool/docs/CheckTags.java)
(see file for instructions). This requires a JDK with javadoc available. The
tool will need to change to reflect the release number to search for.

To check the API status changes, run the ant target "apireport" to generate the
report since the previous official release.

Make sure **@internal APIs** are also marked as @deprecated:

```
* @internal
* @deprecated This API is ICU internal only.
@Deprecated
```

---

## Promote draft APIs to stable

APIs previously introduced as @draft are reviewed for every new release. The
current policy is to keep @draft status at least in one release cycle. For
example, a new @draft API introduced in ICU 60 will be kept @draft in ICU 61.
Then the API will be reviewed by ICU technical committee before ICU 62 release
and the API can be promoted to @stable status.

### **ICU4C**

Andy's method (from email 2019-sep-05):

*   Creation of the promotion spreadsheet was done by hand. It's something of an
    iterative process.
*   Start by making a copy of the previous release spread sheet, to get the
    format, and probable entries for older @draft items that are perpetually
    being held back.
*   Do the API promotion - search the sources for @draft, and make a best guess
    at what to promote. Add entries to the spread sheet as you go. Note any
    items you're not sure about.
*   Run the API change report tool, see what looks inconsistent, fix up the
    sources and rerun as needed. Deleted functions will first show up here, and
    are added to the spread sheet.
*   Cross-check the API change report and the spread sheet.
*   The whole thing takes a bit of time to do.

### **ICU4J**

1.  Create a new GitHub branch.
2.  Run next ant target: `$ ant draftAPIsTSV`
3.  This **ant** target generates a tab-separated values file at
    icu4j/out/draftAPIs.tsv.
4.  Import the TSV file to Google spread sheet - for example, [ICU4J 61 Draft
    API
    Promotion](https://docs.google.com/spreadsheets/d/135RDyY6cWKBBvNuVE9ne8HfsItZu_CvLh47hKjr3UqM/edit#gid=1384666589).
    *   Create the spreadsheet in the shared ***ICU Projects*** folder.
5.  Review all APIs introduced in 2 releases ago or older. For example, 59 or
    older for ICU 61 release.
    *   Any API added 2 or more releases ago is a candidate for promotion.
    *   Newer APIs should be left in @draft state.
6.  Check each API entry has a corresponding approved API proposal. For example,
    [ICU 59 API proposal
    status](https://docs.google.com/document/d/12dHTG3e9WXb7C1Xdc2bcXE0BK8f6IoufoUAPcz4ZT2I/edit).
    Note: An API proposal might have been created before the API proposal doc
    version, so you may need to look at older version of API proposal status
    doc.
7.  Mark APIs proposed for promotion, and add note in the spread sheet.
8.  Update API comments in ICU4J source code. In this case, @draft is replaced
    with @stable (do not change ICU version, for example, "@draft ICU 59" will
    be changed to "@stable ICU 59").
9.  Run next ant target to generate an API change report html: `$ ant apireport`
10. Review the report, sent the report file and the link of the spread sheet to
    ICU technical committee members for review.
11. Once ICU TC approves the proposed change, create a pull request (PR), wait
    for review, and merge the branch into the main branch.

---

## Update the API Change Report

### ICU4C

This work is done in the root of icu4c:

1.  Update the API documentation in all header files (.h file) to have correct
    @draft/@stable/@deprecated/@obsolete labels.
2.  Update docmain.h with additions or removals as needed.
3.  `source/configure`
4.  `make doc`
5.  Follow instructions in
    [tools/release/java/readme.txt](https://github.com/unicode-org/icu/blob/main/tools/release/java/readme.txt).

### ICU4J

This work is done in the root of icu4j:

1.  Make sure JAVA_HOME is set to JDK 8. This report creation fails with JDK 11.
    For example, in Linux:
    *   `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`
2.  Then run ant task "clean" and "apireport" at `<icu4j_root>`:
    *   `ant clean apireport`
3.  Above will produce API change report file
    `<icu4j_root>/<b>out</b>/icu4j_compare_xxx_yyy.html`
4.  Make sure there are any new doc tag errors are reported. (As of ICU 4.4,
    ArabicShaping constants do not have proper tags - otherwise, clean)
5.  Copy generated report file to `<icu4j_root>/APIChangeReport.html` and check
    it in.

Once official release version is shipped, we need to keep API signature
information file for next iteration. This is not done for milestone releases,
only after the final official release.

---

## Check in API signature data file (ICU4J)

Once APIs are frozen for a reference release, we should check in the API
signature data file into the repository. The data file will be used for future
API change report.

1.  Run ant task "gatherapi" at `<icu4j_root>`
2.  Resolve any warnings before proceeding.
3.  The output file `icu4j<ver>.api3.gz` is created in `<icu4j_root>/out` directory.
4.  Copy the output .gz file to `<icu4j_root>/tools/build` directory and check in the
    file to the repository.
5.  You may delete very old versions of the API signature files. Keeping 10
    versions to the latest should be good enough.

Note: This task is only necessary for reference releases, because we won't
change public APIs in maintenance releases. The API signature file for an ICU4J
version is generated and checked into trunk just before ***maint-xx*** branch is
created for the major version, so we can keep track of API changes in
maintenance releases of the major version (A maintenance release should have no
API changes although).

---

## Verify that @draft is surrounded by #ifndef U_HIDE_DRAFT_API etc

In ICU4C, we want every (consecutive group of) `@draft` API to be surrounded by
`#ifndef U_HIDE_DRAFT_API`. This allows users to `-DU_HIDE_DRAFT_API` to make
sure they don't use unstable API.

```
#ifndef U_HIDE_DRAFT_API
/** @draft ICU 51 */
U_CAPI u_newFunction1();
/** @draft ICU 51 */
U_CAPI u_newFunction2();
#endif  // U_HIDE_DRAFT_API
```

Same for `@deprecated` & <code>#ifndef U_HIDE_<b>DEPRECATED</b>_API</code> ..
`#endif // U_HIDE_DEPRECATED_API`

Same for `@internal` & <code>#ifndef U_HIDE_<b>INTERNAL</b>_API</code> ..
`#endif // U_HIDE_INTERNAL_API`

Same for `@system` & <code>#ifndef U_HIDE_<b>SYSTEM</b>_API</code> ..
`#endif // U_HIDE_SYSTEM_API`

Same for `@obsolete` & <code>#ifndef U_HIDE_<b>OBSOLETE</b>_API</code> ..
`#endif // U_HIDE_OBSOLETE_API`

### Caution

*   We cannot #ifndef-guard virtual methods because that makes the vtable
    incompatible.
    *   // Cannot use #ifndef U_HIDE_DRAFT_API for the following draft method
        since it is virtual.
*   When you #ifndef-guard enum constants, normally the following unguarded ones
    (e.g., a _COUNT or _LIMIT) should retain the same numeric values as if the
    guard was absent.

<span style="background-color:yellow">For more details (and cautions) see the
Coding Guidelines section [C/C++ Hiding Un-@stable
APIs](https://unicode-org.github.io/icu/userguide/dev/codingguidelines#cc-hiding-un-stable-apis).</span>

### Task

a) For each of these API status tags, for each API that is tagged with it,
verify that the API is surrounded by the appropriate #ifndef..#endif.

Note: It is best to not use one single guard for APIs with different ICU
versions, since they will become stable and thus lose their guards at different
times. Use one #ifndef..#endif guard per API status and ICU version.

b) For each of these U_HIDE_..._API guards, verify that it only and exactly
surrounds APIs with the corresponding status tag. In particular, make sure that
U_HIDE_DRAFT_API does not surround (newly) @stable API.

We don't have tools for this. <span style="background-color:yellow">One approach
is to</span> use "grep" or similar on the public *common*, *i18n*, *io* header
files. Use grep options like -A 3, -B 2 and -C 3 for context After, Before, and
Around the matching line. <span style="background-color:yellow">A better
approach if you have the tools available is to use a programming-oriented text
editor that can do (a) powerful regex search across (b) multiple files in
several specified directories, and (c) display the matched lines in context such
that (d) they or their context can be edited in place; an example of such a tool
on macOS is BBEdit. This permits a comprehensive search using an expression such
as
"(U_HIDE_\[A-Z_\]+)|(@draft)|(@deprecated)|(@obsolete)|(@system)|(@internal)|(@preview)"
which permits relatively easy checking for correct enclosure of status tags in
conditionals.</span>

<span style="background-color:yellow">As part of this, you may need to run side
searches, for example to verify that no conditionalized type, function or value
is used by an unconditionalized portion of a header file.</span>

<span style="background-color:yellow">There is no magic bullet; however you
carry out this step, it will require several hours of going through the
grep/regex results and manually checking for correct enclosure, checking that
conditionalized values are not needed elsewhere, etc.</span>

---

## Update udraft.h, usystem.h, uintrnl.h, uobslete.h and udeprctd.h

*Ignore this step for ICU49 and later. In ICU 49 and above, these header files
and the gendraft/genheaders.pl tool are gone. (Ticket
[ICU-8571](https://unicode-org.atlassian.net/browse/ICU-8571))*

Instructions ***for ICU4C 4.8.x and earlier***:

1.  make doc
2.  `cd source/tools/gendraft ; make install-headers`
3.  Double check the modified files in <icu>/source/common/unicode folder and
    commit.

---

## Compare ICU4J APIs with JDK

Run the ICU4J versus JDK API comparison tool against the target JDK (anything
that will come out before our next release, basically) with the tool
[com.ibm.icu.dev.tool.docs.ICUJDKCompare](https://github.com/unicode-org/icu/blob/main/icu4j/tools/build/src/com/ibm/icu/dev/tool/docs/ICUJDKCompare.java)
and make sure ICU4J adequately covers the JDK API for the classes we replicate.

---

## Build API documentation

### **ICU4C**

Build the API documentation pages for the new release. Run Doxygen to create the
javadoc files. Create icu4c-X_X_X-docs.zip

**Note: for ICU4C 49m2 or later, requires Doxygen 1.7.5.1 or later ( see
[ICU-8862](https://unicode-org.atlassian.net/browse/ICU-8862) )**

#### Create the ICU4C docs zip file:

1.  Go to .../icu4c/source
2.  Generate the API document pages:<br>
    `make doc-searchengine`
3.  The generated API docs are in <path>/icu4c/source/doc/html/<br>
    `cd <path>/icu4c/source/doc/html/`
4.  Create a zip file, e.g.,<br>
    `tar cvfz /tmp/icu4c72rc.tar.gz * # Note: label the file apropriately for the release.


#### Create a PR for ICU4C docs using the docs zip file

1.  Follow directions in [How to update ICU docs](https://unicode-org.github.io/icu-docs/HOWTO-Update.html)

    a. First, bring the `main` branch of your icu-docs local copy up to date.

    b. Create and switch to a feature branch based of the latest `main`.  Ex: `git checkout -b ICU-<TICKET-NUMBER>`.

    c. Go to the directory for the version of interest, either
       `apidoc/dev/icu4c` (release candicate) or `apidoc/released/icu4c`
       (general release.)

    d. Be sure that you are in the right directory. Then, remove all the files
        in this directory, e.g., `rm -rf *`. (Remember "git restore" is a good friend!)
     
    e. Unzip the tar file from above, replacing all documentation. Ex: `tar xvfz /tmp/icu4c72rc.tar.gz`

    e. Remove the zip file if it's in the folder.

    f. `git add .`.  Note that this may remove some old files. That's correct behavior.

    h. `git commit -m "ICU-<TICKET-NUMBER> Update ICU4C API reference docs for
    XX.Y"`<br>
    Example: [ICU-21546 Update ICU4C API reference docs for 69.1](https://github.com/unicode-org/icu-docs/pull/25)

    i. `git push origin ICU-<TICKET-NUMBER>`

    j. Create Pull Request from your personal github fork of icu-docs from your newly-pushed branch `ICU-<TICKET-NUMBER>` with a destination of
    the `unicode-org/icu-docs` repo's `main` branch

    k. Request a review and submit the PR when approved.

Note: This is also referenced below [Upload API documentations](docs.md#upload-api-documentations) for how to make the API docs public.

### ICU4J

**Note:** JCite must be installed for building ICU4J documentation:
<https://icu.unicode.org/setup/java/ant#TOC-Building-ICU4J-API-Reference-Document-with-JCite>

Build the API documentation pages for the new release:

```
ant releaseDocs
```

#### Alternative method:

**Note:** JCite must be installed for building ICU4J documentation:
<https://icu.unicode.org/setup/java/ant#TOC-Building-ICU4J-API-Reference-Document-with-JCite>

Use the release target

```
ant releaseVer
```

which generate all release files.

*   Upload the output files including icu4j-docs.jar to the release page first,
*   Then update the live API docs from the generated docs.jar.

See [Upload API documentations](docs.md#upload-api-documentations) below for how to make the API docs public.

### Upload API documentations

See <https://unicode-org.github.io/icu-docs/HOWTO-Update.html> for instructions
to upload to <https://unicode-org.github.io/icu-docs/>

### Update the Readme.html for GA

If there are any last second changes:

*   Make sure to document especially items that affect software that is
    currently compiled with the previous version of ICU.
*   Update build/installation instructions as necessary.
