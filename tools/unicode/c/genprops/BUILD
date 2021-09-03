# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This Bazel build file defines a target representing the binary executable
# `genprops`, which is used for generating headers needed for bootstrapping
# the ICU4C build process in a way that integrates core Unicode properties data.

# Defining a binary executable (done in Bazel using `cc_binary`)
# enables the use of the output file from executing the binary as a part of
# other Bazel targets defined using `genrule`.

load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")

package(
    default_visibility = ["//visibility:public"],
)

cc_binary(
    name = "genprops",
    srcs = glob([
        "*.cpp",
        "*.h",   # cannot have hdrs section in cc_binary
    ]),
    deps = [
        "//icu4c/source/common:uhash",
        "//icu4c/source/common:umutablecptrie",
        "//icu4c/source/common:ucptrie",
        "//icu4c/source/common:utrie2",
        "//icu4c/source/common:utrie2_builder",
        "//icu4c/source/common:bytestrie",
        "//icu4c/source/common:bytestriebuilder",
        "//icu4c/source/common:propsvec",
        "//icu4c/source/common:errorcode",
        "//icu4c/source/common:ucharstriebuilder",
        "//icu4c/source/common:uniset",
        "//icu4c/source/common:uvector32",

        "//icu4c/source/common:platform",
        "//icu4c/source/common:headers",

        "//icu4c/source/tools/toolutil:ppucd",
        "//icu4c/source/tools/toolutil:unewdata",
        "//icu4c/source/tools/toolutil:writesrc",
        "//icu4c/source/tools/toolutil:uoptions",
        "//icu4c/source/tools/toolutil:uparse",
        "//icu4c/source/tools/toolutil:toolutil",
        "//icu4c/source/tools/toolutil:denseranges",
    ],
    linkopts = ["-pthread"],
)
