© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

## Updating double-conversion from Github to Vendor

IMPORTANT: Please start with a clean working directory before continuing (no uncommitted changes).

Go to https://github.com/google/double-conversion/releases/latest/ to determine the latest version number. You can also pull from a branch instead of a tag.

Run `pull-from-upstream.sh` as below:

	./pull-from-upstream.sh <tag/branch>

You will be prompted to download the tarball. If confirmed, the script will overwrite the contents of the upstream directory.

## Updating double-conversion from Vendor to ICU4C

After completing the first step, the script will stop again and ask you whether to copy the diffs into icu4c. If you say yes, the *diff between the git index and the working copy* (i.e., the output of `git diff`) will be applied to the corresponding files in icu4c.

Make note of the output of the command. If there are any merge conflicts, you will need to resolve them manually.

## Next Steps

Build and test icu4c, and send the PR for review.
