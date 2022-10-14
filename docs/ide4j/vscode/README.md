# Configuring VSCode for ICU4J

To get ICU4J running in VSCode, including JUnit support:

1. Install the Extension Pack for Java (`vscjava.vscode-java-pack`)
2. In settings.json, remove the contents of `"java.project.resourceFilters"`. This will prevent the extension from dirtying your .project files in later steps:

```
    "java.project.resourceFilters": [
    ]
```

3. Run `ant init` in the `icu4j` directory
4. Run `sudo cp -R lib /external-libraries && chmod a+x /external-libraries`.
    - *This is a big hack! It allows VSCode to find the required jar files.*
    - The same outcome can also be achieved by modifying the paths in the `.classpath` files, as described below.
5. Create a new workspace and add the following directories to it:
    - `icu4j/main/classes/*` (all directories at that path)
    - `icu4j/main/tests/*` (all directories at that path)
    - `icu4j/tools/misc`
6. ICU4J should now be able to _build_ successfully. However, when you try to run tests, you will get failures because it cannot find the data. To fix this, add the following line to the `.classpath` file of the package you are testing (e.g. `icu4j/main/tests/core/.classpath`):

```xml
	<classpathentry kind="lib" path="../../shared/data/icudata.jar"/>
	<classpathentry kind="lib" path="../../shared/data/icutzdata.jar"/>
	<classpathentry kind="lib" path="../../shared/data/testdata.jar"/>
```

7. To prevent your changes to `.classpath` from accidentally being committed, you can run:

```bash
$ git update-index --assume-unchanged icu4j/main/tests/core/.classpath
```

8. If VSCode also tries changing your `org.eclipse.jdt.core.prefs` files, you can ignore those, too:

```bash
$ git update-index --assume-unchanged main/classes/*/.settings/org.eclipse.jdt.core.prefs
$ git update-index --assume-unchanged main/tests/*/.settings/org.eclipse.jdt.core.prefs
$ git update-index --assume-unchanged tools/misc/.settings/org.eclipse.jdt.core.prefs
```
