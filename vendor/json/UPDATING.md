<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# JSON Vendor Library

This library is used by the ICU4C test suite (`intltest`) to read JSON files. It is not used at runtime outside of test code.

## Updating json from Upstream to ICU

IMPORTANT: Please start with a clean working directory before continuing (no uncommitted changes).

Go to https://github.com/nlohmann/json/releases/latest/ to determine the latest version number. You can also pull from a branch instead of a tag.

Run `pull-from-upstream.sh` as below:

	./pull-from-upstream.sh <tag/branch>

The script runs in 4 steps.

### Step 1: Download Tarball

You will be prompted to download the tarball of the upstream tag/branch. If confirmed, the script will download the file to a temp directory and unpack it there.

At this point, the ICU source tree is still pristine.

### Step 2: Patch ICU4C

The script computes the ICU patches on json (diff between the ICU4C source tree and the vendor source tree), then copies in the new version of the files that it downloaded, and apply the ICU patches on top of those new files.

Look in the command output.  If you see a message like `Hunk #6 NOT MERGED`, it means that you have to open the file manually and resolve the merge conflict.

It is also possible that upstream added, deleted, or renamed files (unlikely, since we only use a single header file from the JSON library).  In this situation, you need to spend extra time touching up these changes in ICU4C.

At this point, the ICU4C source tree is changed, and the vendor directory is still pristine.

### Step 3: Copy to Vendor

Here, the script copies the pristine source from the temp directory to the vendor directory.  You should always do this unless you aborted one of the previous steps.

### Step 4: Cleanup

The script will ask whether to delete its temp directory.  If you want to keep the temp directory to refer to the ICU patches, for example, you can skip this step.

## Checking ICU Patches

Look over any ICU patches in the icu4c/source/test/intltest version of the code files; they should be marked clearly with "ICU PATCH" comments.  Make sure that the patches are still needed and remove them if possible.

## Next Steps

Build and test ICU4C, and send the PR for review.

Verify that `LICENSE.MIT` has not changed, may need Unicode legal review if it has.
