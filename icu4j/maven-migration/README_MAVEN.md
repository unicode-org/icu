# Maven progress

## Running the migration scripts

Change the folder to `<icuroot>/icu4j/` and run `maven-migration/toMaven.sh`.

For convenience you can copy the `maven-migration` folder somewhere else and
run the script from there. \
But the current folder when running the script MUST be `<icuroot>/icu4j/`. \
This is convenient if you work in a feature branch and don't want to integrate
from `main` at this point. \
So you checkout `main`, copy the `maven-migration` folder somewhere else,
checkout `<feature_branch>`, and run script from where you copied it.

## TODO

- Packaging the many data files in a jar is slow (especially core)
- The data files (.res & Co.) are spread out in the various modules.
  So we would need a (non maven) step to distribute them.
  Probably modify the step that produces the .jar files (in the C/C++ world)
- done: The tests for localespi should become integration tests
- We might want some integration tests to begin with
- `LICENSE`, `security.policy`, anything else from `main/shared/` => LICENSE should be symlink.
- done: The `.lst` files. All tests pass without, we can probably delete them. But need more testing.
  If not, we need a way to generate them (right now I've copied them by hand)
- Double-check dependencies.
  Also see https://stackoverflow.com/questions/27726779/declare-maven-dependency-as-test-runtime-only
- BOM (Bill of Materials). See https://reflectoring.io/maven-bom/
- See the library shared with cldr tools
- Also to do:
  - promote this one folder up?
  - done: - build things in parent (demos, samples, tools)
  - done: - from the parent folder these will go away
    - `main/` ?
    - `maven/`
    - `maven-build/`
    - `manifest.stub`
    - `coverage-exclusion.txt`
    - `build.xml`
    - `ivy.xml`
- Change icu4j artifact description: \
  from "International Component for Unicode for Java" \
  to "International Components for Unicode for Java" (Components) \
  Fix all instances (found 6 instances, 4 in older pom.xml files, 2 in .md files)

> Copyright © 2023 and later Unicode, Inc. and others. All Rights Reserved.
Unicode and the Unicode Logo are registered trademarks
of Unicode, Inc. in the U.S. and other countries.
[Terms of Use and License](http://www.unicode.org/copyright.html)
