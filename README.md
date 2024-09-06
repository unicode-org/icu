#  International Components for Unicode

This is the repository for the [International Components for Unicode](https://icu.unicode.org/).
The ICU project is under the stewardship of [The Unicode Consortium](https://www.unicode.org).

- Source: https://github.com/unicode-org/icu
- Bugs: https://unicode-org.atlassian.net/projects/ICU
- API Docs: https://unicode-org.github.io/icu-docs/
- User Guide: https://unicode-org.github.io/icu/

![ICU Logo](./tools/images/iculogo_64.png)

### 🔴🔴🔴 Special Notice About Branch Renaming 🔴🔴🔴
Around March 24-25, 2021 we renamed the `master` branch to `main`. You may need to rename your branch in your local git repo and change your normal git command to reflect this change. See also https://github.com/github/renaming

### Build Status (`main` branch)

Build | Status
------|-------
GitHub Actions (ICU4C) | [![GHA ICU4C](https://github.com/unicode-org/icu/workflows/GHA%20ICU4C/badge.svg)](https://github.com/unicode-org/icu/actions?query=workflow%3A%22GHA+ICU4C%22+branch%3Amain)
GitHub Actions (ICU4J) | [![GHA ICU4J](https://github.com/unicode-org/icu/workflows/GHA%20ICU4J/badge.svg)](https://github.com/unicode-org/icu/actions?query=workflow%3A%22GHA+ICU4J%22+branch%3Amain)
GitHub Actions (Valgrind) | [![GHA CI Valgrind](https://github.com/unicode-org/icu/workflows/GHA%20CI%20Valgrind/badge.svg)](https://github.com/unicode-org/icu/actions/workflows/icu_valgrind.yml?query=workflow%3A%22GHA+CI%22+branch%3Amain)
Exhaustive Tests | [![Exhaustive Tests for ICU](https://github.com/unicode-org/icu/actions/workflows/icu_exhaustive_tests.yml/badge.svg?branch=main)](https://github.com/unicode-org/icu/actions/workflows/icu_exhaustive_tests.yml?query=branch%3Amain)
Fuzzing | [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/icu.svg)](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:icu)
OpenSSF Scorecard | [![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/unicode-org/icu/badge)](https://securityscorecards.dev/viewer/?uri=github.com/unicode-org/icu)



### Subdirectories and Information

- [`icu4c/`](./icu4c/) [ICU for C/C++](./icu4c/readme.html)
- [`icu4j/`](./icu4j/) [ICU for Java](./icu4j/readme.html)
- [`tools/`](./tools/) Tools
- [`vendor/`](./vendor/) Vendor dependencies

### Copyright & Licenses

Copyright © 2016-2024 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

A CLA is required to contribute to this project - please refer to the [CONTRIBUTING.md](./CONTRIBUTING.md) file (or start a Pull Request) for more information.

The contents of this repository are governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html) and are released under [LICENSE](./LICENSE).
