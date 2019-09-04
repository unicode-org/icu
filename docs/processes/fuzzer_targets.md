<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Developing Fuzzer Targets for ICU APIs
======================================

This documents describes how to develop a [fuzzer](https://opensource.google.com/projects/oss-fuzz)
target for an ICU API and its integration into the ICU build process.

### Directory and naming conventions

Fuzzer targets are exclusively in directory
[`source/test/fuzzer/`](https://github.com/unicode-org/icu/tree/master/icu4c/source/test/fuzzer)
and end with `_fuzzer.cpp`. Only files with such ending are recognized and executed as fuzzer
targets by the OSS-Fuzz system.

### General structure of a fuzzer target

As a minimum, a fuzzer target contains the function


```
extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  ...
}
```

This function is expected and invoked by the fuzzer system. The `data` parameter contains the
fuzzer-controlled data of size `size` bytes. Part or all of this data is then passed into the
ICU API under test.

Fuzzer target
[`collator_rulebased_fuzzer.cpp`](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/fuzzer/collator_rulebased_fuzzer.cpp)
illustrates the basic elements.

```
// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cstring>

#include "fuzzer_utils.h"
#include "unicode/coll.h"
#include "unicode/localpointer.h"
#include "unicode/locid.h"
#include "unicode/tblcoll.h"

IcuEnvironment* env = new IcuEnvironment();

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;

  size_t unistr_size = size/2;
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), data, unistr_size * 2);
  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);

  icu::LocalPointer<icu::RuleBasedCollator> col1(
      new icu::RuleBasedCollator(fuzzstr, status));

  return 0;
}
```

The ICU API under test is the `RuleBasedCollator(const UnicodeString &rules, UErrorCode &status)`
constructor. The code interprets the fuzzer data as UnicodeString and passes it to the constructor.
And that is all. Specific error handling or return value verification is not required because the
fuzzer will detect all memory issues by means of memory/address sanitizer findings.

### Makefile.in changes

ICU fuzzer targets are built and executed by the OSS-Fuzz project. On side of ICU they are compiled
to assure that the code is syntactically correct and, as a sanity check, executed in the most basic
manner, i.e. with minimal testdata and without ASAN or MSAN analysis.

Add the new fuzzer target to the list of targets in the `FUZZER_TARGETS` variable in
[`Makefile.in`](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/fuzzer/Makefile.in).
The new fuzzer target will then be built and executed as part of a normal ICU4C unit test run. Note
that each fuzzer target becomes executable on its own. As such it is linked with the code in
`fuzzer_driver.cpp`, which contains the `main()` function.

### Fuzzer seed corpus

Any fuzzer seed data for a fuzzer target goes into a file with name `<fuzzer_target>_seed_corpus.txt`.
In many cases the input parameter of the ICU API under test is of type `UnicodeString`, in case
of which the seed data should be in UTF-16 format. As an example,see
[collator_rulebased_fuzzer_seed_corpus.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/fuzzer/collator_rulebased_fuzzer_seed_corpus.txt).

### Guidelines and tips

*   Leave all randomness to the fuzzer. If a random selection of any kind is needed (e.g., of a
    locale), then use bytes from the fuzzer data to make the selection
    ([example](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/fuzzer/break_iterator_fuzzer.cpp)).
*   In many cases ICU unit tests can provide seed data or at least ideas for seed data. If the API
    under test requires a Unicode string then make sure that the seed data is in UTF-16 encoding.
    This can be achieved with e.g. the 'iconv' command or using an editor that saves text in UTF-16.

### How to locally reproduce fuzzer findings

At this time reproduction of fuzzer findings requires Docker installed on the local machine and the
OSS-Fuzz project downloaded in a local git client.

1.  Install Docker (Ubuntu):

    ```
    sudo apt install docker
    ```
2.  Download OSS-Fuzz, switch into directory oss-fuzz/

    In a git client directory, download the fuzzer system.

    ```
    git clone https://github.com/google/oss-fuzz.git
    cd oss-fuzz/
    ```
3.  Build the Docker image for ICU.
    In some setups root permissions may be required to connect to the Docker.

    ```
    [sudo] python infra/helper.py build_image icu
    ```
    A prompt will appear: `Pull latest base images (compiler/runtime)? (y/N)`
    Respond: 'N'. If you are curious then respond with 'y' (won't hurt).
4.  Build the ICU fuzzers:

    ```
    [sudo] python infra/helper.py build_fuzzers --sanitizer [address | memory | undefined] icu
    ```
    Check that the fuzzer targets were built successfully: ```ls -l build/out/icu```

5.   Reproduce the fuzzer finding.
     First, get the testdata the fuzzer used when finding the issue. In the fuzzer bug report look
     for 'Reproducer Testcase', a click on the link will download the testdata. Then execute

     ```
     [sudo] python infra/helper.py reproduce icu <icu_fuzzer> <testdata>
     ```
     Concrete example:

     ```
     sudo python infra/helper.py reproduce icu uregex_open_fuzzer  ~/Downloads/clusterfuzz-testcase-minimized-uregex_open_fuzzer-5732067058384896
     ```

**Limitations:** When reproducing a fuzzer finding in the way outlined above the fuzzer environment
will use the current ICU trunk from https://github.com/unicode-org/icu.git. Thus it is not possible
to modify the code to try out a possible fix. What can be done is to redirect Docker to download ICU
from a forked ICU repository. Open the file oss-fuzz/projects/icu/Dockerfile and adjust the line
with `git clone --depth 1 https://github.com/unicode-org/icu.git icu` accordingly. Then modify
the code in the forked repository and follow the steps above beginning with step 3, create a Docker
image.

This of course is still a tedious way of reproducing and working on a fuzzer finding. Ticket
[ICU-20734](https://unicode-org.atlassian.net/browse/ICU-20734) aims to introduce a fuzzer driver
that can reproduce certain fuzzer findings in a local ICU workspace.
