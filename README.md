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
GitHub Actions | [![GHA CI](https://github.com/unicode-org/icu/workflows/GHA%20CI/badge.svg)](https://github.com/unicode-org/icu/actions?query=workflow%3A%22GHA+CI%22)
Azure Pipelines | [![Build Status](https://dev.azure.com/ms/icu/_apis/build/status/unicode-org/CI?branchName=main)](https://dev.azure.com/ms/icu/_build/latest?definitionId=360&branchName=main)
Azure Pipelines (Exhaustive Tests) | [![Build Status](https://dev.azure.com/ms/icu/_apis/build/status/unicode-org/CI-Exhaustive-Main?branchName=main)](https://dev.azure.com/ms/icu/_build/latest?definitionId=361&branchName=main)
Azure Pipelines (Valgrind ICU4C) | [![Build Status](https://dev.azure.com/ms/icu/_apis/build/status/unicode-org/CI-Valgrind?branchName=main)](https://dev.azure.com/ms/icu/_build/latest?definitionId=364&branchName=main)
Fuzzing | [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/icu.svg)](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:icu)
OpenSSF Scorecard | [![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/unicode-org/icu/badge)](https://securityscorecards.dev/viewer/?uri=github.com/unicode-org/icu)



### Subdirectories and Information

- [`icu4c/`](./icu4c/) [ICU for C/C++](./icu4c/readme.html)
- [`icu4j/`](./icu4j/) [ICU for Java](./icu4j/readme.html)
- [`tools/`](./tools/) Tools
- [`vendor/`](./vendor/) Vendor dependencies

### Copyright & Licenses

Copyright © 2016-2023 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

The project is released under [LICENSE](./LICENSE).

A CLA is required to contribute to this project - please refer to the [CONTRIBUTING.md](./CONTRIBUTING.md) file (or start a Pull Request) for more information.

