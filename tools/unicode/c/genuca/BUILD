# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This Bazel build file defines a target representing the binary executable
# `genuca`, which is used for generating ICU root collation data files.

load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")

package(
    default_visibility = ["//visibility:public"],
)

cc_binary(
    name = "genuca",
    srcs = glob([
        "*.cpp",
        "*.h",   # cannot have hdrs section in cc_binary
    ]),
    deps = [
        "//icu4c/source/common:headers",
        "//icu4c/source/common:platform",
        "//icu4c/source/i18n:collation_builder",
        "//icu4c/source/i18n:headers",
        "//icu4c/source/tools/toolutil:collationinfo",
        "//icu4c/source/tools/toolutil:toolutil",
        "//icu4c/source/tools/toolutil:unewdata",
        "//icu4c/source/tools/toolutil:uoptions",
        "//icu4c/source/tools/toolutil:uparse",
        "//icu4c/source/tools/toolutil:writesrc",
    ],
    # Markus 2021-06-16:
    # The pthread library is not linked in automatically.
    # See https://docs.bazel.build/versions/main/cpp-use-cases.html
    # When pthread is absent, then we get runtime errors instead of compile/link errors.
    # See https://stackoverflow.com/questions/51584960/stdcall-once-throws-stdsystem-error-unknown-error-1
    #
    # My first genuca build crashed with
    #   terminate called after throwing an instance of 'std::system_error'
    #   what():  Unknown error -1
    #
    #   Program received signal SIGABRT, Aborted.
    #   ...
    #   #4  0x00007ffff7e809d1 in std::terminate() () from /lib/x86_64-linux-gnu/libstdc++.so.6
    #   #5  0x00007ffff7e80c65 in __cxa_throw () from /lib/x86_64-linux-gnu/libstdc++.so.6
    #   #6  0x00007ffff7e78458 in std::__throw_system_error(int) () from /lib/x86_64-linux-gnu/libstdc++.so.6
    #   #7  0x0000555555601c75 in std::call_once<void (&)()> (__once=..., __f=@0x55555560156c: {void (void)} 0x55555560156c <icu_70::umtx_init()>)
    #       at /usr/include/c++/10/mutex:743
    #   #8  0x00005555556017ca in icu_70::umtx_initImplPreInit (uio=...) at icu4c/source/common/umutex.cpp:146
    #   #9  0x0000555555592236 in icu_70::umtx_initOnce (uio=..., fp=0x5555555e0716 <icu_70::initNFCSingleton(UErrorCode&)>,
    #   errCode=@0x7fffffffd738: U_ZERO_ERROR) at icu4c/source/common/umutex.h:143
    linkopts = ["-pthread"],
)
