# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This file defines Bazel targets for a subset of ICU4C "common" library header and source files.
# The configuration of dependencies among targets is strongly assisted by the
# file in depstest that maintains such information, at
# icu4c/source/test/depstest/dependencies.txt .

load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")

package(
    default_visibility = ["//visibility:public"],
)

# When compiling code in the `common` dir, the constant
# `U_COMMON_IMPLEMENTATION` needs to be defined. See 
# https://unicode-org.github.io/icu/userguide/howtouseicu#c-with-your-own-build-system .

# If linker errors occur, then this may be a sign that the dependencies were 
# not specified correctly. Use dependencies.txt in depstest for assistance. See
# https://stackoverflow.com/q/66111709/2077918 .

cc_library(
    name = "headers",
    hdrs = glob([
        "unicode/*.h", # public
        "*.h",         # internal
        ],
        # Instead of using this checked-in file, our Bazel build process
        # regenerates this file and then uses the new version.
        exclude = ["norm2_nfc_data.h"],
    ),
    # We need to add includes in order to preserve existing source files'
    # include directives that use traditional paths, not paths relative to
    # Bazel workspace:
    # https://stackoverflow.com/a/65635893/2077918
    includes = ["."],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "platform",
    srcs = [
        "cmemory.cpp", 
        "uobject.cpp",
        "cstring.cpp",
        "cwchar.cpp",
        "uinvchar.cpp",
        "charstr.cpp",
        "unistr.cpp",
        "appendable.cpp",
        "stringpiece.cpp",
        "ustrtrns.cpp",
        "ustring.cpp",  
        "ustrfmt.cpp",  
        "utf_impl.cpp",
        "putil.cpp",
        "ucln_cmn.cpp",  
        "udataswp.cpp",  
        "umath.cpp",
        "umutex.cpp",
        "sharedobject.cpp",
        "utrace.cpp",
    ],
    deps = [
        ":headers",
        # omit other deps b/c they are sys symbols
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
    linkopts = ["-ldl"],
)

cc_library(
    name = "utrie",
    srcs = ["utrie.cpp"],
    deps = [":platform"],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "utrie2",
    srcs = ["utrie2.cpp"],
    deps = [":platform"],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "utrie2_builder",
    srcs = ["utrie2_builder.cpp"],
    deps = [
        ":utrie",
        ":utrie2",
        ":platform",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "ucptrie",
    srcs = ["ucptrie.cpp"],
    deps = [":platform"],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "umutablecptrie",
    srcs = ["umutablecptrie.cpp"],
    deps = [":ucptrie"],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "bytestrie",
    srcs = ["bytestrie.cpp"],
    deps = [":platform"],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "bytestriebuilder",
    srcs = ["bytestriebuilder.cpp"],
    deps = [
        ":bytestrie",
        ":stringtriebuilder",
        ":sort",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "stringtriebuilder",
    srcs = ["stringtriebuilder.cpp"],
    deps = [
        ":uhash",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "uhash",
    hdrs = [
        "uhash.h",
    ],
    srcs = [
        "uhash.cpp",
    ],
    deps = [
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "errorcode",
    hdrs = [
    ],
    srcs = [
        "errorcode.cpp",
    ],
    includes = ["."],
    deps = [
        ":platform",
        ":utypes",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "utypes",
    srcs = [
        "utypes.cpp",
    ],
    includes = ["."],
    deps = [
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "uniset",
    srcs = [
        "uniset.cpp",
        "unifilt.cpp",
        "unisetspan.cpp",
        "bmpset.cpp",
        "util.cpp",
        "unifunct.cpp",
        "usetiter.cpp",
    ],
    includes = ["."],
    deps = [
        ":patternprops",
        ":uvector",
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "patternprops",
    srcs = [
        "patternprops.cpp",
    ],
    includes = ["."],
    deps = [
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "propsvec",
    srcs = [
        "propsvec.cpp",
    ],
    includes = ["."],
    deps = [
        ":sort",
        ":utrie2_builder",
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "propname",
    srcs = [
        "propname.cpp",
    ],
    includes = ["."],
    deps = [
        ":bytestrie",
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

# Note: cc_library target name "uvector32" matches dependencies.txt group name,
# but filename "uvectr32.*" differs in spelling.
cc_library(
    name = "uvector32",
    srcs = [
        "uvectr32.cpp",
    ],
    includes = ["."],
    deps = [
        ":platform",
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "sort",
    srcs = [
        "uarrsort.cpp",
    ],
    includes = ["."],
    deps = [
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)

cc_library(
    name = "uvector",
    srcs = [
        "uvector.cpp",
    ],
    includes = ["."],
    deps = [
        ":platform",
        ":sort",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",
    ],
)


# Building this target will indirectly cause the creation of a header file that
# integrates normalization data. This header file is generated by a genrule
# target that invokes the gennorm2 binary.
cc_library(
    name = "normalizer2",
    srcs = [
        "normalizer2.cpp",
        "normalizer2impl.cpp",
    ],
    includes = ["."],
    hdrs = [
        "normalizer2impl.h",
        "norm2_nfc_data.h",  # generated by gennorm2
    ],
    deps = [
        ":headers",
    ],
    local_defines = [
        "U_COMMON_IMPLEMENTATION",  
    ],
)



# Note: Bazel has chosen specifically to disallow any official support for breaking
# through constraints that would allow copying to or generating to source
# directories: https://stackoverflow.com/questions/51283183/how-to-generate-files-in-source-folder-using-bazel

genrule(
    name = "gen_norm2_nfc_data_h",
    srcs = [
        "//icu4c/source/data/unidata/norm2:nfc.txt",
    ],
    outs = ["norm2_nfc_data.h"],
    cmd = "./$(location //icu4c/source/tools/gennorm2) -o $@ $(location //icu4c/source/data/unidata/norm2:nfc.txt) --csource && cp $@ /tmp",
    tools = ["//icu4c/source/tools/gennorm2"],
)
