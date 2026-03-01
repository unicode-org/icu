<!--
Copyright (C) 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Updating ICU's built-in Break Iterator rules
============================================

Here are instructions for updating ICU's built-in break iterator rules, for Grapheme, Word, Line and Sentence breaks.

The ICU rules implement the boundary behavior from Unicode [UAX-14](https://www.unicode.org/reports/tr14/) and [UAX-29](https://www.unicode.org/reports/tr29/), with tailorings from CLDR and some ICU-specific enhancements. ICU rules updates are needed in response to changes from Unicode or CLDR, or for bug fixes. Often ideas for CLDR or UAX updates are prototyped in ICU first, before becoming official.

This is not a cook book process. Familiarity with ICU break iterator behavior and rules is needed. Sets of break rules often interact in subtle and difficult to understand ways. Expect some bumps.

### Have clear specifications for the change.

The changes will typically come from a proposed update to Unicode UAX 29 or UAX 14,
or from CLDR based tailorings to these specifications.

As an example, see [CLDR proposal for Extended Indic Grapheme Clusters](https://github.com/unicode-org/cldr/tree/main/common/properties/segments).

Often ICU will implement draft versions of proposed specification updates, to check that they are complete and consistent, and to identify any issues before they are released.

### Files that typically will need to be updated:


| File                                |  Contents |
|-------------------------------------|--------------------------
| icu/icu4c/source/...
| .../test/testdata/rbbitst.txt       | Data driven test file. Typically the first to be updated.
| .../data/brkitr/rules/*.txt         | Main break rule files.
| .../test/intltest/rbbitst.cpp       | Monkey Test Rules (as code; also called the 'original' or 'old' monkey test).
| .../test/testdata/break_rules/*.txt | Monkey Test Rules (as data; also called the 'new' or 'rule-based' monkey test).
| .../test/testdata/*BreakTest.txt    | Unicode Supplied Test Files.
|||
| icu/icu4j/...
| .../main/shared/data/icudata.jar        | Data jar, includes break rules. Derived from ICU4C.
| .../main/tests/core/src/com/ibm/icu/dev/test/rbbi/rbbitst.txt         | Test data, copied from ICU4C.
| .../main/tests/core/src/com/ibm/icu/dev/test/rbbi/break_rules/*       | Monkey test rules, copied from ICU4C.
| .../main/tests/core/src/com/ibm/icu/dev/test/rbbi/RBBITestMonkey.java | Monkey test w rules as code. Port from ICU4C.


### ICU4C

The rule updates are done first for ICU4C, and then ported (code changes) or moved (data changes) to ICU4J. This order is easiest because the the break rule source files are part of the ICU4C project, as is the rule builder.

1.  **Add basic tests**` to icu4c/source/test/testdata/rbbitst.txt`

    This file contains data driven tests, which are basically text strings marked up with their expected break positions. The test syntax is documented in the file itself.

    Add tests to to spot check the basics of the changes, to verify that some simple, straight forward cases work as expected. There is no need to thoroughly check corner cases; the goal at this step is a quick sanity check that will fail before the rule update and pass afterwards.

    The [Unicode Utilities](http://www.unicode.org/cldr/utility/) can be very helpful at this point, for showing what characters
    match a UnicodeSet expression, and for listing the properties of a particular character.

    Tests added for the above example:

         #
         #  ICU-13637 and CLDR-10994 - Indic Grapheme Cluster Boundary changes to support aksaras
         #      New rule: LinkingConsonant ExtCccZwj* Virama ExtCccZwj* × LinkingConsonant
         #      Sample Chars: LinkingConsonant: \u0915
         #                    Virama:           \u094d    [also Extend]
         #                    ExtCccZWJ:        \u0308
         #                    Extend but not ExtCCCZWJ   \u093A
         <char>
         <data>•\u0915\u094d\u0915•</data>
         <data>•\u0915\u0308\u0308\u094d\u0308\u0308\u0915•</data>
         <data>•\u0915\u0308\u0308\u094d\u0308\u0308•\u0041•</data>
         <data>•\u0915\u0308\u0308\u094d\u093A\u093A•\u0915•</data>

    Two copies of the test file exist in the ICU repository, one for C++ and one for Java. There are two because
    there is no common place that will always be present and that the two build systems can both access.

    The two should be identical. Verify this before starting to make changes. If they differ, it
    probably means that some earlier change to ICU4C was not fully ported to ICU4J, and this
    needs to be resolved before proceeding.

        diff icu4c/source/test/testdata/rbbitst.txt icu4j/main/tests/core/src/com/ibm/icu/dev/test/rbbi/rbbitst.txt

    Should show no differrence.


2.  **Run the ICU4C break iterator tests**, and verify that the newly added tests fail as expected.
    (We haven't updated the rules yet)

            cd icu/icu4c/source
            make -j6 check

    To run just the RBBI Tests (you will be doing this a lot)

            cd test/intltest
            LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$LD_LIBRARY_PATH  ./intltest rbbi

    A snippet from the test output, showing one of many expected failures:

            === Handling test: rbbi: ===
            rbbi {
            TestExtended {
            code    alpha extend alphanum type word sent line name
            ------------------------------------------------ 0
                915     1      0        1   Lo   LE   LE   AL DEVANAGARI LETTER KA
                94d     0      1        0   Mn Extend   EX   CM DEVANAGARI SIGN VIRAMA
            ------------------------------------------------ 2
                915     1      0        1   Lo   LE   LE   AL DEVANAGARI LETTER KA
                    Forward Iteration, break found, but not expected.  Pos=   2  File line,col=  175,  21
                    Reverse Itertion, break found, but not expected.  Pos=   2  File line,col=  175,  21

3.  **Update the main rule file for the break iterator type in question.**

    For this example, the rule file is `icu4c/source/data/brkitr/rules/char.txt`.
    (If the change is for word or line break, which have multiple rule files for tailorings, only update the root file at this time.)

    Start by looking at how existing similar rules are being handled, and also refer to the ICU user guide section on [Break Rules](../userguide/boundaryanalysis/break-rules.md) for an explanation of rule syntax and behavior.

    The transformation from UAX or CLDR style rules to ICU rules can be non-trivial. Sources of difficulties include:

    -  All ICU rules run in parallel, while UAX/CLDR rules are applied sequentially, stopping after the first match. The ICU rules sometimes require extra logic to prevent a later rule from preempting an earlier rule. This can be quite tricky to express.

    -  ICU rules match a run of text that does not have boundaries in its interior (unless the rule contains a "hard break", represented by a '/'. UAX and CLDR rules, on the other hand, tell whether a single text position is or is not a break, with the rule expressing pre and post context around that position. This transformation is generally not hard, and the ICU form  of the rules is often simpler.


4.  **Rebuild the ICU data with the updated rules.**

        cd icu4c/source/data
        make

5.  **Rerun the data-driven test**, `rbbi/TestExtended`. With luck, it may pass. Failures fall into two classes:

    - The newly added test failed. Either something is wrong with the test cases, or something is wrong with the rule updates.

    - A previously existing test started failing. Examine the test case; it probably conflicts with the new rules. Either change the expected boundaries, or change the test string to something that generates the previous boundaries. Or remove the test. Whatever seems most sensible.

    Fix any failures before proceeding. Don't try to run other tests yet; a large number of failures are very likely.

6.  **Run any relevant Unicode test data.** ICU's copies of the test files are here:

            icu4c/source/test/testdata/GraphemeBreakTest.txt
            icu4c/source/test/testdata/LineBreakTest.txt
            icu4c/source/test/testdata/SentenceBreakTest.txt
            icu4c/source/test/testdata/WordBreakTest.txt

    If the update includes new versions of any of these files, copy them to the above locations.

    To run the test:

            cd icu4c/source/test/intltest
            LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$LD_LIBRARY_PATH  ./intltest rbbi/RBBITest/TestUnicodeFiles

    The test files are from the Unicode Consortium. The official, released versions are at https://www.unicode.org/Public/UCD/latest/ucd/auxiliary/. The files are copied, unmodified, into the ICU source tree to make them accessible to the ICU tests.

    If the update is for a new Unicode version, or for a new CLDR tailoring of the root Unicode rules, it should include updated test data files. If they're missing, ask whoever is requesting or providing the updated rules for help. The test data is generated by CLDR tooling.

    Copy any new Unicode test data files to their location in icu, and rerun the test.

    Historically, failures have roughly equal chance of being problems with the test data or problems with the ICU rules. In either event, track down and fix any problems before proceeding.

    *Note:* Known issues with the test data file are accounted for in the test code, in the function `RBBITest::testCaseIsKnownIssue()` in the file `rbbitst.cpp`. Test cases are skipped when ICU behavior has been patched or enhanced for some reason, relative to standard Unicode behavior.

7.  **Other Break Iterator Tests, except for Monkey Tests**

            cd icu4c/source/test/intltest
            LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$LD_LIBRARY_PATH  ./intltest rbbi

    This runs all of the RBBI tests, including the Monkey tests. For this step, ignore Monkey failures, and track down and fix any others.

    There is a real mish-mash of old tests, checking random bits of hard coded data.

8.  **Monkey Tests**

    Monkey testing compares the breaking behavior of the main ICU RBBI implementation with that of a reference implementation, using random data.

    Monkey testing has proved to be by far the most effective way to check obscure edge and corner case behavior, to the point that it no longer seems worth while to hand-write more than fairly basic test cases.

    ICU has two independent RBBI monkey tests. The original one implements the break rules directly in code. The algorithm sticks pretty close to that of the Unicode UAX specifications. The original monkey test checks only the root break iterator behavior, not any tailorings.

    The newer, data-driven monkey test takes its reference rules from test data files, instead of hard coding them. It covers tailorings. Its algorithm differs somewhat from that of the UAX specifications, in that the rules match runs of text rather than testing pre or post context around a potential break. (This was intended as a first step in driving a revised algorithm back to the specifications, a task that hasn't yet happened.)

    The original plan was to retire the old monkey test in favor of the newer data-driven one, but each tends to uncover problems that the other misses, so they both remain.

9.  **Original Monkey Test**

    To run the test:

            LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$LD_LIBRARY_PATH  ./intltest rbbi/RBBITest/TestMonkey@"type=char loop=-1"

            Test parameters (following the '@'
            seed=nnnnn        Random number starting seed.
                              Setting the seed allows errors to be reproduced.
            loop=nnn          Looping count.  Controls running time.
                              -1:  run forever.
                               0 or greater:  run length.

            type = char | word | line | sent | title

    Updating the test with new or revised rules requires changing the test source code, in `icu4c/source/test/intltest/rbbitst.cpp`. Look for the classes RBBICharMonkey, RBBIWordMonkey, RBBISentMonkey and RBBILineMonkey. The body of each class tracks the corresponding UAX-14 or UAX-29 specifications in defining the character classes and break rules.

    After making changes, as a final check, let the test run for an extended period of time, on the order of several hours.
    Run it from a terminal, and just interrupt it (Ctrl-C) when it's gone long enough.

10. **New Monkey Test**

    To run the test:

            intltest rbbi/RBBIMonkeyTest/testMonkey@rules=grapheme.txt,loop=-1

    The @rules parameter is the test rules file to run; test rules files are located in the directory `icu4c/source/test/testdata/break_rules`

    The test should initially fail, because ICU's library rules have been updated (steps 3 and 4), but the reference rules used
    by this test have not yet been.

    Make the updates to the test rules and re-run. The rule syntax is described in
    [icu4c/source/test/testdata/break_rules/README.md](https://github.com/unicode-org/icu/blob/main/icu4c/source/test/testdata/break_rules/README.md)
    The test reference rules are in this same directory.

    Again, after everything appears to be working, let the test run for an extended length of time. Long runs are especially important with the more complex break rule sets, such as line break.

11. **Tailorings**

    If this is an update to word or line break root behavior, the rule changes must be propagated from from the root rule files to the tailored files, for both the main rules (source/data/brkitr/rules/*) and the monkey test rules (source/test/testdata/break_rules/*).

    The easiest and safest way to do this is to create a patch file of the diffs to the root rule file, typically using `git diff`.
    Apply it to the various tailorings of the break type being updated.

    Merge conflicts when applying the patch would indicate that the same rules modified by the new change were also modified in the tailoring. When this happens, you just have to dig in and understand the intent of the rules and the tailoring were, and figure out what makes sense. Fortunately, conflicts are not common.

    As with the main rules, after everything appears to be working, run the rule based monkey test for an extended period of time (with loop=-1).

### ICU4J

1.  **Copy the Data Driven Test File to ICU4J**

    Copy the file `rbbitst.txt` from ICU4C to ICU4J, and run the Java test. It should fail until the rules are updated.

            cd <top level icu directory>
            cp icu4c/source/test/testdata/rbbitst.txt icu4j/main/tests/core/src/com/ibm/icu/dev/test/rbbi/rbbitst.txt

    Run the test from Eclipse.

    Navigate to `/icu4j-core-tests/src/com/ibm/icu/dev/test/rbbi/RBBITestExtended.java`.

    Select the `TestExtended()` function in the source code, right-click it and choose "Run As Junit Test".

    Errors (expected because the break rules have not yet been updated) should show the failing line in `rbbitst.txt`. For example:

            java.lang.AssertionError: Forward Iteration, break found, but not expected.  Pos=2  File line,col= 175, 21

2.  **Refresh ICU4J data from ICU4C**.

    This will bring over the updated break rules, refreshing the file `main/shared/data/icudata.jar`. Follow the instructions from `icu4c/source/data/icu4j-readme.txt`.

    Rerun the ICU4J tests. `TestExtended` should now pass. Others may start failing.

3.  **Port the code-based Monkey Test changes from ICU4C**

    ICU4C file to port from: `source/test/intltest/rbbitst.cpp`

    ICU4J file to port to: `main/tests/core/src/com/ibm/icu/dev/test/rbbi/RBBITestMonkey.java`

    To conveniently run the individual tests, look for the test functions `TestCharMonkey()`, `TestWordMonkey()`, etc. in `RBBITestMonkey.java`.

    Test parameters are passed via the Eclipse Run Configuration settings, arguments tab, VM parameters.  For example,

            -ea -Dseed=554654 -Dloop=1

    When the test appears to be working, run for an extended time (with -Dloop=-1).

4.  **New (rule driven) Monkey Test**

    Copy the updated monkey test rules from ICU4C and run the test.**

    ICU4C directory, to copy from: `source/test/testdata/break_rules/`

    ICU4J directory, to copy to: `main/tests/core/src/com/ibm/icu/dev/test/rbbi/break_rules/`

    Then rerun the rule based monkey test, in the file `main/tests/core/src/com/ibm/icu/dev/test/rbbi/RBBIMonkeyTest.java`. Find the test function `TestMonkey()`; it include comments describing how to run it with parameters from Eclipse.

    Run the test(s) for the changed rules for an extended amount of time (with Dloop=-1).









