#  International Components for Unicode

This is the repository for the [International Components for Unicode](http://site.icu-project.org). 
The ICU project is under the stewardship of [The Unicode Consortium](https://www.unicode.org).

- Source: https://github.com/unicode-org/icu
- Bugs: https://unicode-org.atlassian.net/projects/ICU

![ICU Logo](./tools/images/iculogo_64.png)

### Build Status (`master` branch)

Build | Status
------|-------
TravisCI | [![Build Status](https://travis-ci.org/unicode-org/icu.svg?branch=master)](https://travis-ci.org/unicode-org/icu)
Azure Pipelines | [![Build Status](https://unicode-icu.visualstudio.com/ICU/_apis/build/status/CI?branchName=master)](https://unicode-icu.visualstudio.com/ICU/_build/latest?definitionId=21&branchName=master)
Azure Pipelines (Exhaustive Tests) | [![Build Status](https://unicode-icu.visualstudio.com/ICU/_apis/build/status/CI-Exhaustive-Master?branchName=master)](https://unicode-icu.visualstudio.com/ICU/_build/latest?definitionId=24&branchName=master)
Azure Pipelines (Valgrind ICU4C) | [![Build Status](https://unicode-icu.visualstudio.com/ICU/_apis/build/status/CI-Valgrind-Master?branchName=master)](https://unicode-icu.visualstudio.com/ICU/_build/latest?definitionId=30&branchName=master)
AppVeyor | [![Build status](https://ci.appveyor.com/api/projects/status/6ev1ssb6efahsvs2?svg=true)](https://ci.appveyor.com/project/unicode-org/icu)
Fuzzing | [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/icu.svg)](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:icu)


### Subdirectories and Information

- [`icu4c/`](./icu4c/) [ICU for C/C++](./icu4c/readme.html)
- [`icu4j/`](./icu4j/) [ICU for Java](./icu4j/readme.html)
- [`tools/`](./tools/) Tools
- [`vendor/`](./vendor/) Vendor dependencies

### License

Please see [./icu4c/LICENSE](./icu4c/LICENSE) (C and J are under an identical license file.)

> Copyright © 2016 and later Unicode, Inc. and others. All Rights Reserved.
Unicode and the Unicode Logo are registered trademarks 
of Unicode, Inc. in the U.S. and other countries.
[Terms of Use and License](http://www.unicode.org/copyright.html)
