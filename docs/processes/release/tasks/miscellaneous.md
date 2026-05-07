---
layout: default
title: Miscellaneous
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 80
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Miscellaneous
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Complete code reviews

Nag all the reviewers to finish reviewing the code and change the status of
bugs.

---

## Check in serialization compatibility test data

ICU4J unit test contains serialization compatibility test cases. When a new
reference version is released, we build serialized object data with the version
and use it for future testing.

1.  Run "ant serialTestData" at ICU4J root directory
2.  The target generates test data and runs some serialization test cases.
3.  Once you confirm the test runs clean, copy
    `out/serialTestData/ICU_<version>` to
    `main/core/src/test/resources/com/ibm/icu/dev/test/serializable/data/ICU_<version>`.

---

## Release ticket

After every milestone (GA / RC / Milestone-N) is completed, create a new release
ticket in ICU Jira.
The release ticket is used for BRS tasks, such as version update, tagging new version,
merging post RC fixes from trunk and others.

---

## Check integrity of Jira issues in commit messages

Every commit being shipped in the next ICU release should be labeled with a Jira
ticket that is marked as fixed with the correct fix version. Further, there
should be no Jira tickets marked as fixed with the current fixVersion that do
not have commits. 

### Run locally

To check this, run the following tool:

<https://github.com/unicode-org/icu/tree/main/tools/commit-checker>

Follow the instructions in the README file to generate the report locally and send it
for review.

### Run via CI

Alternatively, you can run the "BRS Commit Checker Report" workflow directly from the project page CI
by:

1. Go to the [Actions tab](https://github.com/unicode-org/icu/actions)
1. Click on the "BRS Commit Checker Report" workflow name on the left hand list of workflows
1. Click the "Run workflow" dropdown.

    The dropdown should reveal an input form in which to provide inputs

1. Provide the same inputs in the form as you would for a local run of the tool,
as described in the [tool's Readme](https://github.com/unicode-org/icu/tree/main/tools/commit-checker) in the instructions above.

    The only difference from the local run instructions is that git branch names in the 
Actions workflow input form should be prefixed with `origin/`. Ex:

    ```
    The ICU Jira "Fix Version" semver -> 78.1
    The git ref start of comparison range. Prefix branches with `origin/`. -> release-77-1
    The git ref end of comparison range. Must be descendant of `from_git_ref`. Prefix branches with `origin/`. -> origin/main
    ```

1. If the job fails with an error such as this:

    ```
    jira.exceptions.JIRAError: JiraError HTTP 404 url: https://unicode-org.atlassian.net/rest/api/2/issue/ICU-XXXXX
        text: Issue does not exist or you do not have permission to see it.
    ```

    then you likely can solve this by recreating the Jira API token as described in the [tool's Readme](https://github.com/unicode-org/icu/tree/main/tools/commit-checker) in the instructions above.
    Since 2024, the [Jira API tokens are not allowed to last longer than 365 days](https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/),
    but may expire more quickly than 365 days depending on what duration was set, etc.
    The account corresponding to the email should be allowed to view sensitive tickets, or else any
    existence of sensitive tickets in the git commit range will cause a failure.
---

## Fix Mis-ticketted commits

If the commit checker tool above reports any malformed commit messages, it might
mean that a bad commit made its way onto ICU main branch. To fix this, a rebase
is required. Since rebases can be disruptive, confirm with ICU-TC before
performing one. If ICU-TC agrees to perform a rebase, initiate the process as
follows:

$ git checkout main; git pull --ff-only upstream main

$ git rebase -i --preserve-merges latest

Note: although tempting, please do not use --committer-date-is--author-date. It
changes the order of commits in ways we don't want to do.

In the interactive rebase window, choose commit messages to rewrite using the
"reword" option. Save and exit the interactive screen, and then edit each commit
message.

When ready, force-push the main branch to your fork and give ICU-TC a day or two
to review. Before force-pushing to upstream, create a new branch on upstream
with the latest commit on the old main branch; name it something like
"pre63-old-main".  When ready, disable branch protection on main, force-push,
and then reapply branch protection. Create a new branch named something like
"pre63-new-main" to allow users to easily switch between the two heads.

Send an email to icu-support explaining the change and how to deal with it. You
can use [this email](https://groups.google.com/a/unicode.org/g/icu-support/c/DC_pX9kPEoc) as
a model.

---

## Scrub closed issues in code

ICU and CLDR issues are tracked with the [Unicode JIRA
tool](https://unicode-org.atlassian.net/jira/dashboards/last-visited). Each open
issue should be indicated in code with either "TODO" or "knownIssue" as a
comment that includes the JIRA identifier.

A "TODO" or "knownIssue" usually prevents a test from executing, often with
specific conditions. Instead, the output will include a message indicating that
the issue is present but that it does not prevent the test suite from passing.

Note that sometimes an issue has been marked as "done" without updating the
"TODO" or "knownIssue" item in the code or test routines. This step is used to
synchronize the issue database and the code.

The idea is simple:

(1) Search for "TODO(12345)" to detect TODO items for closed issues.

(2) Do the same for logKnownIssue. (the data related logKnownIssues are often
addressed during CLDR data integration)

(3) For each TODO or logKnownIssue that is marked as "done", check the status in
JIRA. Check if the problem is actually fixed by removing the TODO/logKnownIssue
protection from the code so the test will be executed.

(4) Verify that the test now passes. First run local tests, i.e., in icu4c or
icu4j.

If any test does not pass, either reopen the issue or create a new issue and
update the reference in the code. Then communicate the problems with the Unicode
tech team.

(5) If all tests have passed so far, run the exhaustive tests with the update.
[See Exhaustive Tests](https://unicode-org.github.io/icu/userguide/dev/ci.html#:~:text=Exhaustive%20tests%20run%20tests%20on,and%20click%20'Run%20workflow') for details.

If any test does not pass, the JIRA status is incorrect. Raise this problem with
the Unicode tech team as noted above.

If the exhaustive tests all pass, the TODO or knownIssue can be
scrubbed. Createa pull request and request a review. Submit when approved and
all conditions pass.


> [!NOTE]
> New in ICU78: Finding issues is automated with a python script in
icu/tools/scripts/scrub_issues. Here's how:

1. Install the python jira module, e.g., `pip install jira`. See [pypi.org.project/jira](https://pypi.org/project/jira/).
2. Get the latest version of ICU code in local directory, e.g., ~/newICU.
3. Execute the script from the scrub_issues directory with the ICU code path:
```
# In the new ICU directory
cd icu/tools/scripts/scrub_issues/scrub_issues.py
python scrub_issues.py  --icu_base ~/newICU/icu |& tee new_scrub_results.txt
```
4. Examine each line of the "Closed ids" section of the output file:
 * Try removing the code that prevents tests from failing in the lines for each of the "knownIssue" and "TODO" items.
 * If a modified test passes, update the code in the Github repository.
 * After merging changes, synch with the repository. Then rerun the python script to verify that the status is updated.

By default, the script reports all instances of each TODO or knownIssue by file and line number in that file.

### Possible closed issues: update the comments
```
INFO:scrub issues:----------------------------------------------------------------
INFO:scrub issues:2 Closed ids: ['ICU-23185', 'CLDR-18905']
INFO:scrub issues:       ['CLDR-18905', 'logKnownIssue', '/icu/icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/MeasureUnitTest.java', 529]
INFO:scrub issues:       ['CLDR-18905', 'logKnownIssue', '/icu/icu4c/source/test/intltest/measfmttest.cpp', 5529]
INFO:scrub issues:       ['CLDR-18905', 'logKnownIssue', '/icu/icu4c/source/test/intltest/measfmttest.cpp', 5956]
INFO:scrub issues:       ['ICU-23185', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/format/DateIntervalFormatTest.java', 895]
INFO:scrub issues:       ['ICU-23185', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/format/DateIntervalFormatTest.java', 900]
INFO:scrub issues:       ['ICU-23185', 'logKnownIssue', '/icu/icu4c/source/test/intltest/dtifmtts.cpp', 1255]
INFO:scrub issues:       ['ICU-23185', 'logKnownIssue', '/icu/icu4c/source/test/intltest/dtifmtts.cpp', 1262]
```

### Non-standard identifiers. Issues not in JIRA

This script also finds commented lines that are not named with either "ICU-" or "CLDR-" plus a number. Often these are very old issues that may have been resolved without updating the code.

Sometimes just an indentifier that is simple a number is found. In these cases, the script checks if there is a JIRA issue with either prefix and that number. 
Consider updating the test code with the full identifier. In that case "REPLACEMENT" is shown.

Examples:

```
INFO:root:Base Directory: /usr/local/google/home/ccornelius/ICU78-new/icu
INFO:scrub issues:----------------------------------------------------------------
INFO:scrub issues:       ['knownIssue', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/format/PersonNameConsistencyTest.java', 89]
INFO:scrub issues:       ['knownIssue', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/format/PersonNameConsistencyTest.java', 99]
INFO:scrub issues:REPLACEMENT 22099 --> ICU-22099 (Accepted)
INFO:scrub issues:       ['22099', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/number/ExhaustiveNumberTest.java', 61]
INFO:scrub issues:       ['a/b/c', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/TestUnicodeKnownIssues.java', 25]
INFO:scrub issues:       ['a/b/c', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/TestUnicodeKnownIssues.java', 37]
...
INFO:scrub issues:       ['String ticket', 'logKnownIssue', '/icu/icu4j/main/framework/src/test/java/com/ibm/icu/dev/test/TestFmwk.java', 198]
WARNING:scrub issues:Not in JIRA : "Tom" (1 instances)
INFO:scrub issues:       ['Tom', 'TODO', '/icu/icu4j/main/core/src/main/java/com/ibm/icu/impl/SimpleFilteredSentenceBreakIterator.java', 32]
WARNING:scrub issues:Not in JIRA : "a/b/c" (7 instances)
INFO:scrub issues:       ['a/b/c', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/TestUnicodeKnownIssues.java', 25]
INFO:scrub issues:       ['a/b/c', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/TestUnicodeKnownIssues.java', 37]
INFO:scrub issues:       ['a/b/c', 'logKnownIssue', '/icu/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/TestUnicodeKnownIssues.java', 54]
...
```

Finally, the script reports all issues that have JIRA entries with status 'Accepted' or 'Design', showing the instances in code:

```
INFO:scrub issues:Unresolved IDS: 57 still not "Done" in Jira
...
INFO:scrub issues:id: ['13034', 'Accepted']
INFO:scrub issues:       ['13034', 'TODO', '/icu/icu4c/source/test/intltest/numfmtst.cpp', 8613]
INFO:scrub issues:       ['13034', 'TODO', '/icu/icu4c/source/i18n/number_padding.cpp', 32]
```

It is recommended to review such unresolved issues periodically.
