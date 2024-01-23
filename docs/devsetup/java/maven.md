---
layout: default
title: Maven Setup for Java
grand_parent: Setup for Contributors
parent: Java Setup
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Maven Setup for Java
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


Maven is a standard build tool in the Java ecosystem with a very well-defined
preferred way to organize the directory structure and execute builds.
Thus, many IDEs for Java will have some level of support for Maven.
The command line invocation is still, of course, the standard of truth for the build.

## Installation

Install Maven from your OS package manager.
The minimum version is 3.2.5 as of ICU 74,
although this may change.
See the `<ICU>/icu4j/pom.xml` root POM file's maven-enforcer-plugin
for the current minimum version.
The Maven page lists [system requirements](https://maven.apache.org/download.cgi),
which includes a minimum of JDK 8 as of ICU 74.

## Maven multi-module projects

ICU4J's codebase is structured as a "multi-module" Maven project.
In other words, the Maven project is divided into submodules that have dependencies on each other.
Multi-module projects are not uncommon in Maven over the several years,
yet the support for them by Maven is not complete.

Commonly-found instructions to use Maven at the command line
are usually designed for simple "single-module" Maven projects.
In such cases, the command names correspond intuitively to their function:
`mvn test` for running unit tests, `mvn package` for creating jar files, etc.
However, multi-module projects have the tradeoff between time and complexity:
running `mvn test` at the root recompiles all the sources
even if a module's sources are all unchanged,
which takes longer than it otherwise could.
The alternative is to always instruct Maven to compile and locally cache the artifact
for each submodule,
which speeds up tasks by avoiding duplicate compilation and testing effort,
but also changes the distinct Maven commands
`mvn {test, integration-test, ...}`
all into
`mvn install`
that are instead differentiated by appropriate options.

The instructions in this page for command line usage will choose,
from the 2 alternatives above,
the option that consistently uses `mvn install` for all Maven tasks.
Note that this choice should not affect IDEs with well working native Maven support like IntelliJ and Eclipse.
See the "IDE Setup" section for more details on how IDEs handle Maven projects differently than
the standard command line style usage of Maven.

## IDE Setup

Users of IDEs should familiarize themselves with the information about how to use Maven at
the command line,
which also includes information about Maven builds.

> :point_right: **Note**: In most IDEs, whenever the Maven configurations change (`pom.xml` files),
you will need to refresh your IDE project using the IDE's Maven plugin/functionality.

Most IDEs represent user settings for different codebases via an IDE-specific notion
usually called "project" or "workspace".
The IDE's project/workspace is a separate construct from the Maven configurations for the codebase.
Therefore, creating a new project for an existing codebase is done by "importing" the codebase,
in which the IDE creates the project/workspace settings files.
If you pull updates to the upstream ICU codebase codebase that result in changes to Maven settings,
then the IDE behavior may be behind until you update your IDE project accordingly.


### IntelliJ

IntelliJ does a good job of understanding multi-module Maven projects, 
including the non-standard configuration here.
It also recognizes the customized locations of source code files and test code files in the configuration here.

To import into IntelliJ:

1. In IntelliJ, open a new project. 
  a. Recent versions of IntelliJ provide a dialog box on startup to select a project. Click the "Open" button.)
2. Select the root `pom.xml` in ICU4J (ex: `<ICU>/icu4j/pom.xml`)
3. That's it. Note: IntelliJ will take a few minutes to do a one-time indexing of the new source code.

Navigating the source code files between main code and test code, and running tests individually or for an entire module,
work as they do normally in IntelliJ.

> :point_right: **Note**: Currently, Maven cannot build the entire project due to settings for `tools/utilities-for-cldr`. To work around this so that `Build > Build Project` works: in the "Project" toolbar, navigate to the `tools/utilities-for-cldr` folder, right click for the contextual menu, then `Maven > Ignore Projects`.
>
> When this workaround is no longer needed, the project can be reenabled by: `View > Tool Windows > Maven`, then expand "International Components for Unicode (ICU)", right click on `utilities-for-cldr`, then select `Unignore Projects`.

### Eclipse

[Eclipse's Maven plugin](https://eclipse.dev/m2e/)
works reasonably well and can support the import of a multi-module Maven project.
These instructions have not yet verified Eclipse's handling of the import of the ICU4J using a Maven build.

### VS Code

VS Code's support of Maven projects is not as robust as IntelliJ's when it comes to the non-standard file layout for sources and tests.
The Maven support comes from the standard Java extension (which depends on the standard Maven extension) from the extension marketplace.
Source and test code files are not recognized properly, and it is not clear how to execute the tests.

However, a workaround exists for those who want to use VS Code as their preferred editor and still execute commands to recompile or run tests.
The workaround relies on invoking the Maven commands in a shell, and using a VS Code extension to create shortcuts within the IDE to invoke those commands.

The extension is [Command Runner](https://marketplace.visualstudio.com/items?itemName=edonet.vscode-command-runner).
Next, create a VS Code workspace (File > Open Folder...) at the ICU4J root at `<ICU>/icu4j`.
Then edit your settings for your VS Code workspace for ICU4J (this is the file at `<ICU>/icu4j/.vscode/settings.json`)
by adding this section to the settings:

```jsonnet
{
    //...
    "command-runner.commands": {
        // The following commands assume your VS Code workspace is rooted at `<ICU_ROOT>/icu4j`. If not,
        // then adjust accordingly.
        "core > all > test": "cd ${workspaceFolder}; mvn -am -pl main/core install",
        "core > number > test": "cd ${workspaceFolder}; mvn -am -pl main/core test -Dtest=\"com/ibm/icu/dev/test/number/*,com/ibm/icu/dev/impl/number/*\" -Dsurefire.failIfNoSpecifiedTests=false",
        "core > text > test": "cd ${workspaceFolder}; mvn -am -pl main/core test -Dtest=\"com.ibm.icu.dev.test.text.*\" -Dsurefire.failIfNoSpecifiedTests=false",
        "charset > test": "cd ${workspaceFolder}; mvn -am -pl main/charset test",
        "common_tests > integration test": "cd ${workspaceFolder}; mvn -am -pl main/localespi test",
    }
    //...
}
```

As the extension's documentation describes, there are multiple ways to open up the palette of command shortcuts.
One way is to hit Ctrl/Cmd+Shift+P, then type "Run Command", then hit enter.
Another way is to right-click the background of any editor pane.

After the palette appears, you can choose which Maven build target to execute.

## Usage at the command line

Maven divides its concept of a build into a "lifecycle" of a linear sequence of steps, called "phases".
These phases correspond to common good practice tasks of a project, and they have a predefined order.
Each phase can only begin if all of the previous phases have finished successfully.
The sequence of phases include ... `compile` ... `test` ... `package` ... `integration-test` ... `verify` ... `install` ... `deploy`.

In a simple Maven project, the phases also serve as default build targets.
However, as mentioned above,
ICU4J is structured as a multi-module Maven project.
The instructions here for command line Maven usage will take the approach of using the `install` target
for all tasks.
Options can be used to control whether tests are executed, and which ones.

### Testing

To only execute a command within a submodule of the project, from the root, use the `-am -pl <projectlist>` syntax like this:

```
mvn install -am -pl main/core
```

where `<projectlist>` is a comma-separated list of names of the subfolders which contain the submodule configuration pom.xml files.

If you want to run only a specific test(s), use the `-Dtest="<test>"` option, where `<test>` can be a test name, a class name / package prefix, or a comma-separate list of them.


#### Run unit tests

The `test` target will only run unit tests (excludes integration tests). Ex:

```
mvn install -am -pl main/core -DskipITs
```

#### Run all tests (integration and unit tests)

All tests (unit tests and integration tests) will run by default.

```
mvn install -am -pl main/core
```

#### Run a single test
```
mvn install -Dtest="ULocaleTest" -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```
or
```
mvn install -Dtest="com.ibm.icu.dev.test.util.ULocaleTest" -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```

#### Run a single method in a single test

```
mvn install -Dtest="ULocaleTest#TestGetAvailableByType" -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```

#### Run multiple tests
You can use regular expression patterns and comma-separate lists,
such as:

```
mvn install -Dtest="RBBI*" -Dsurefire.failIfNoSpecifiedTests=false
mvn install -Dtest="*Locale*" -Dsurefire.failIfNoSpecifiedTests=false
```

or

```
mvn install -Dtest="*Locale*,RBBI*" -Dsurefire.failIfNoSpecifiedTests=false
```

If you want to run tests according to the package structure of the classes,
then you should use the filesystem notation for the test files in the regular expression expansion.
Therefore, this syntax will not work:
`mvn install -Dtest="com.ibm.icu.dev.test.util.*" -Dsurefire.failIfNoSpecifiedTests=false`.
Instead, you want to use this syntax:
```
mvn install -Dtest="com/ibm/icu/dev/test/util/*" -Dsurefire.failIfNoSpecifiedTests=false
```

#### Run in exhaustive mode

Some tests in ICU are configured to run only when "exhaustive mode" is enabled.
Exhaustive mode enables long running tests that would otherwise not run,
or would run far fewer iterations.
Exhaustive mode is configured through the system property `ICU.exhaustive`.
`ICU.exhaustive` takes an integer value from 0 to 10 such that, 
when greater than 5,
will trigger some tests to run in exhaustive mode.
See `TestFmwk.java` for more details,
and `ExhaustiveNumberTest.java` for an example of a test using it.

```
mvn install -DICU.exhaustive=10
```

#### Skip tests

If you want to skip tests, add the options:

```
-DskipTests -DskipITs
```

The first option specifies skipping unit tests,
and the second option specifies skipping integration tests.


## More info on Maven

To learn more about the details of Maven not covered above,
start by reading the [*Maven by Example* book](https://books.sonatype.com/mvnex-book/reference/index.html),
which gives an overview of Maven.
For more details on a specific topic,
refer to the [*Maven: Complete Reference* book](https://books.sonatype.com/mvnref-book/reference/index.html).