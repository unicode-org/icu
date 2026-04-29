---
layout: default
title: Errorprone for Java
grand_parent: Setup for Contributors
parent: Java Setup
---

<!--
© 2026 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Errorprone for Java
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

Error Prone is a static analysis tool for Java that catches common programming mistakes at compile-time. See https://errorprone.info/

ICU4J runs it in CI for each pull request to prevent problems from being
introduced, and to periodically generate a report, posted at
https://github.com/unicode-org/icu, under the “Quality Reports (main branch)”
→ “Java static analysis” section).

## Integration with IDEs

See https://errorprone.info/docs/installation

## Running the checks

There are currently two CI checks for errorprone.
One is executed for each PR, and fails if a problem is detected.
The other one generates a full report, and does not fail.
The result is posted on the project GitHub page, under “Quality Reports.”

Here is how you run the test that runs in CI:

```sh
mvn clean test -DskipTests -DskipITs -P errorprone
```

And here is how to run the full test that produces reports in `html`, `tsv`,
and `md` formats.

```sh
# Run the checks
mvn clean test -DskipTests -DskipITs \
    -l /tmp/errorprone_all.log -P errorprone-all
# Generate reports in html, tsv, and md formats
mvn exec:java -f icu4j/tools/errorprone_report/ \
    -DlogFile=/tmp/errorprone_all.log -P errorprone_report
# Make sure it says "BUILD SUCCESS"
tail /tmp/errorprone_all.log
```

## Patching

See https://errorprone.info/docs/patching. \
But we will summarize here how to use it from Maven, for ICU4J.

> :point_right: **Note**: This is the patching mechanism provided by errorprone itself.
But there are (of course) other ways, for example an IDE. \
For example I had good results with Eclipse, enabling a certain check,
then asking Eclipse to fix all instances.

> :point_right: **Note**: this patching is not necessarily perfect.
Always check, visually and by running all tests.

### Preparation

1. Edit the `pom.xml` in the root of the ICU repo.
1. Search for the `errorprone-all` profile, and in the `compilerArgs` section of
  that profile find the `<arg>` element that contains `-Xplugin:ErrorProne`.
1. After `-Xplugin:ErrorProne` add `-XepPatchChecks:<listOfIssueTypes> -XepPatchLocation:IN_PLACE`.
  For example `-XepPatchChecks:UnusedMethod`. \
  You can also specify more than one kind of issues to fix, using comma as a separator,
  for example `-XepPatchChecks:UnusedMethod,UnusedVariable`.

### Apply the fixes

Run the errorprone report generation step:

```sh
mvn clean test -DskipTests -DskipITs -l /tmp/errorprone_all.log -P errorprone-all
```

If errorprone knows how to patch the kind of issue you specified, it will update the files.

> :point_right: **Note**: the patches will not always apply cleanly.
>
> Keep an eye on warnings, and `git status` see if there
> are any files with an `.orig` or `.rej` extension.
> Those are patches that didn't apply cleanly, and will need to be checked
> more carefully, and possibly fixed by hand.

### Test, cleanup

**Re-format:**
```sh
mvn spotless:apply
```

**Test:**
```sh
mvn package
```

**Visually inspect the changed files** (`git diff` or a GUI git client are handy here). Compare with the initial errorprone report.

### Prepare to submit

Update `errorprone.cfg` to declare the issue you just fix as an error. \
That way it will prevent it from coming back in the future.
In our example add `-Xep:UnusedMethod:ERROR`.

Revert the `pom.xml` changes (`git checkout -- pom.xml`).

Run all the errorprone checks (see [“Running the checks”](#running-the-checks))
to see that the all issues of that type are fixed.

Run a regular build with testing:
```sh
mvn clean package
```

You might have to go back to [“Test, cleanup”](#test-cleanup) if something fails.

Finally, create a PR, commit, push, send for review.
