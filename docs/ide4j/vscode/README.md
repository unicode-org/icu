<!--- © 2020 and later: Unicode, Inc. and others. ---> 
<!--- License & terms of use: http://www.unicode.org/copyright.html --->

# Configuring VSCode for ICU4J

To get ICU4J running in VSCode, including JUnit support:

1. Install the Extension Pack for Java (`vscjava.vscode-java-pack`)
2. In settings.json, remove the contents of `"java.project.resourceFilters"`. This will prevent the extension from dirtying your .project files in later steps:

```
    "java.project.resourceFilters": [
    ]
```

3. Run `ant init` in the `icu4j` directory
4. Create a new workspace and add each of the following directories to it:
    - `icu4j/main/classes/*` (all directories at that path)
    - `icu4j/main/tests/*` (all directories at that path)
    - `icu4j/tools/misc`
5. Modify all of the `icu4j/main/tests/.classpath` files as follows:
    1. Change all occurrences of `/external-libraries/` to `../../../lib/`.
    2. Add `classpathentry`s for the data jar files.
    
    For example:

```xml
<!-- Before -->

	<classpathentry kind="lib" path="/external-libraries/hamcrest-core-1.3.jar"/>
	<classpathentry kind="lib" path="/external-libraries/junit-4.12.jar" sourcepath="/external-libraries/junit-4.12-sources.jar">
		<attributes>
			<attribute name="javadoc_location" value="jar:platform:/resource/external-libraries/junit-4.12-javadoc.jar!/"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="lib" path="/external-libraries/JUnitParams-1.0.5.jar" sourcepath="/external-libraries/JUnitParams-1.0.5-sources.jar">
		<attributes>
			<attribute name="javadoc_location" value="jar:platform:/resource/external-libraries/JUnitParams-1.0.5-javadoc.jar!/"/>
		</attributes>
	</classpathentry>

<!-- After -->

	<classpathentry kind="lib" path="../../../lib/hamcrest-core-1.3.jar"/>
	<classpathentry kind="lib" path="../../../lib/junit-4.12.jar" sourcepath="../../../lib/junit-4.12-sources.jar">
		<attributes>
			<attribute name="javadoc_location" value="jar:platform:/resource/../../../lib/junit-4.12-javadoc.jar!/"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="lib" path="../../../lib/JUnitParams-1.0.5.jar" sourcepath="../../../lib/JUnitParams-1.0.5-sources.jar">
		<attributes>
			<attribute name="javadoc_location" value="jar:platform:/resource/../../../lib/JUnitParams-1.0.5-javadoc.jar!/"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="lib" path="../../shared/data/icudata.jar"/>
	<classpathentry kind="lib" path="../../shared/data/icutzdata.jar"/>
	<classpathentry kind="lib" path="../../shared/data/testdata.jar"/>
```

6. To prevent your changes to `.classpath` from accidentally being committed, you can run:

```bash
$ git update-index --assume-unchanged main/tests/*/.classpath
```

7. If VSCode also tries changing your `org.eclipse.jdt.core.prefs` files, you can ignore those, too:

```bash
$ git update-index --assume-unchanged main/classes/*/.settings/org.eclipse.jdt.core.prefs
$ git update-index --assume-unchanged main/tests/*/.settings/org.eclipse.jdt.core.prefs
$ git update-index --assume-unchanged tools/misc/.settings/org.eclipse.jdt.core.prefs
```

8. To verify that everything is working, use the VSCode UI to run your choice of JUnit test. Open any `*.java` file, and you should see a double-right triangle, which you can click to run the test.

Other tips:

- View the project outline under "Java Projects" on the left side under the file explorer; from here, you can rebuild projects, run all tests, add new classes, etc.
- To rebuild: open the command palette (`Shift + Ctrl + P` on Linux) and search for "Java: Rebuild Projects"
- To debug: right-click the icon where you run your test, and click "Debug Test"
