# ICU4C unity compilation check

This standalone CMake project compiles the full ICU4C `common` and `i18n`
source lists in unity batches. It does not link ICU, generate data, or replace
the supported ICU build system.

From the repository root, for example:

```sh
cmake -S icu4c/source/test/unity -B /tmp/icu4c-unity -G Ninja \
  -DCMAKE_UNITY_BUILD_BATCH_SIZE=50
cmake --build /tmp/icu4c-unity --target icu_unity_common icu_unity_i18n
```

Set `CMAKE_UNITY_BUILD_BATCH_SIZE` from 2 to 254 to vary the batch boundaries.
The `common` list has 202 sources and the `i18n` list has 254 sources. All
sources are included in unity batches, including when a library fits in one
batch. The i18n target sets both `UNISTR_FROM_*_EXPLICIT` macros consistently
before any headers are included.
