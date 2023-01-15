<!--
© 2022 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4J Maven Build

This directory exists as part of work to build ICU4J with Maven.
Specifically, this directory was created to exist alongside the code
directory structure without changing it.
This preserves the existing behavior of Ant build targets and BRS tasks / artifact
deploy processes based on the output of those Ant builds.

If / when the Ant build is suitable to be replaced and remove by Maven
or other such build tool, this directory should be removed.

Note: This directory is the root of the directory structure that defines
the Maven build without changing the existing code structure and Ant build.

## Usage

Maven is a pretty standard build tool in the Java ecosystem with a very well-defined
preferred way to do file layout and build execution.
Thus, many IDEs for Java will have some level of support for Maven.
The command line invocation is still, of course, the standard of truth for the build.

In this current setup for the ICU4J Maven build, in which the existing file layout and
processes remain the same in order to keep the Ant build unchanged, 
the Maven build is configured in ways that are non-standard for a typical Maven setup.
Therefore, IDE support for this non-standard Maven build might be less than if ICUJ4's
directories and build process were modified to be more consistent with Maven expectations.
However, there still exist IDEs with good support, and the results of the command line 
invocation's test and artifact jar packaging are designed to be equivalent to the Ant build.

### Usage in IDEs

Users of IDEs should familiarize themselves with the information about how to use Maven at
the command line,
which also includes information about Maven builds.

#### IntelliJ

IntelliJ does a good job of understanding multi-module Maven projects, 
including the non-standard configuration here.
It also recognizes the customized locations of source code files and test code files in the configuration here.

To import into IntelliJ:

1. In IntelliJ, open a new project. 
  a. Recent versions of IntelliJ provide a dialog box on startup to select a project. Click the "Open" button.)
2. Select the `pom.xml` in this same directory (ex: `<ICU>/icu4j/maven-build/pom.xml`)
3. That's it. Note: IntelliJ will take a few minutes to do a one-time indexing of the new source code.

Navigating the source code files between main code and test code, and running tests individually or for an entire module,
work as they do normally in IntelliJ.

#### VS Code

VS Code's support of Maven projects is not as robust as IntelliJ's when it comes to the non-standard file layout for sources and tests.
The Maven support comes from the standard Java extension (which depends on the standard Maven extension) from the extension marketplace.
Source and test code files are not recognized properly, and it is not clear how to execute the tests.

However, a workaround exists for those who want to use VS Code as their preferred editor and still execute commands to recompile or run tests.
The workaround relies on invoking the Maven commands in a shell, and using a VS Code extension to create shortcuts within the IDE to invoke those commands.

The extension is [Command Runner](https://marketplace.visualstudio.com/items?itemName=edonet.vscode-command-runner).
Next, create a VS Code workspace (File > Open Folder...) at the ICU4J root at `<ICU>/icu4j`.
Then edit your settings for your VS Code workspace for ICU4J (this is the file at `<ICU>/icu4j/.vscode/settings.json`)
by adding this section to the settings:

```json
{
    ...
    "command-runner.commands": {
        // The following commands assume your VS Code workspace is rooted at `<ICU_ROOT>/icu4j`. If not,
        // then adjust accordingly.
        "core > all > compile": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j compile",
        "core > all > test": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j test -DfailIfNoTests=false",
        "core > number > test": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j test -Dtest=\"com.ibm.icu.dev.test.number.*,com.ibm.icu.dev.impl.number.*\" -DfailIfNoTests=false",
        "core > text > test": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j test -Dtest=\"com.ibm.icu.dev.test.text.*\" -DfailIfNoTests=false",
        "charset > compile": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j-charset compile",
        "charset > test": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j-charset test -DfailIfNoTests=false",
        "localespi > compile": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j-localespi compile",
        "localespi > test": "cd ${workspaceFolder}/maven-build; mvn -am -pl maven-icu4j-localespi test -DfailIfNoTests=false",
    }
    ...
}
```

As the extension's documentation describes, there are multiple ways to open up the palette of command shortcuts.
One way is to hit Ctrl/Cmd+Shift+P, then type "Run Command", then hit enter.
Another way is to right-click the background of any editor pane.

After the palette appears, you can choose which Maven build target to execute.

#### Eclipse

Although Eclipse's Maven plugin works reasonably well and can support the import of a multi-module Maven project,
the non-Maven-standard layout that it currently has seems to prevent an effective usage.
In particular, even though Eclipse can be configured to build the project, 
it does not dispay the source code files and test code files for editing within the IDE through any of the usual Views.

### Usage at the command line

Maven divides its concept of a build into a "lifecycle" of a linear sequence of steps, called "phases".
These phases have a predefined order, and each phase can only begin if all of the previous phases have finished successfully.
Phases also serve as default build targets.
The sequence of phases include ... `compile` ... `test` ... `package` ... `integration-test` ... `deploy`.

At the root of the project, you can run `mvn compile` to build/compile, and `mvn test` to run all of the tests (after first compiling successfully).

To only execute a command within a submodule of the project, from the root, use the `-am -pl <projectlist>` syntax like this:
```
mvn test -am -pl maven-icu4j
```
where `<projectlist>` is a comma-separated list of names of the subfolders which contain the submodule configuration pom.xml files.

If you want to run only a specific test(s), use the `-Dtest="<test>"` option, where `<test>` can be a test name, a class name / package prefix, or a comma-separate list of them.

If you want to skip tests, use the `-DskipTests=true` option.
