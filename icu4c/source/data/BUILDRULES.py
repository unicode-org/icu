# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Python 2/3 Compatibility (ICU-20299)
# TODO(ICU-20301): Remove this.
from __future__ import print_function

from distutils.sysconfig import parse_makefile

from buildtool import *
from buildtool import utils
from buildtool.request_types import *

import sys


def generate(config, glob, common_vars):
    requests = []

    if len(glob("misc/*")) == 0:
        print("Error: Cannot find data directory; please specify --glob_dir", file=sys.stderr)
        exit(1)

    # DIRECTORIES
    build_dirs = [
        "{OUT_DIR}",
        "{OUT_DIR}/curr",
        "{OUT_DIR}/lang",
        "{OUT_DIR}/region",
        "{OUT_DIR}/zone",
        "{OUT_DIR}/unit",
        "{OUT_DIR}/brkitr",
        "{OUT_DIR}/coll",
        "{OUT_DIR}/rbnf",
        "{OUT_DIR}/translit",
        "{TMP_DIR}",
        "{TMP_DIR}/curr",
        "{TMP_DIR}/lang",
        "{TMP_DIR}/locales",
        "{TMP_DIR}/region",
        "{TMP_DIR}/zone",
        "{TMP_DIR}/unit",
        "{TMP_DIR}/coll",
        "{TMP_DIR}/rbnf",
        "{TMP_DIR}/translit",
        "{TMP_DIR}/brkitr"
    ]

    requests += generate_cnvalias(config, glob, common_vars)
    requests += generate_confusables(config, glob, common_vars)
    requests += generate_conversion_mappings(config, glob, common_vars)
    requests += generate_brkitr_brk(config, glob, common_vars)
    requests += generate_stringprep(config, glob, common_vars)
    requests += generate_brkitr_dictionaries(config, glob, common_vars)
    requests += generate_normalization(config, glob, common_vars)
    requests += generate_coll_ucadata(config, glob, common_vars)
    requests += generate_unames(config, glob, common_vars)
    requests += generate_misc(config, glob, common_vars)
    requests += generate_curr_supplemental(config, glob, common_vars)
    requests += generate_translit(config, glob, common_vars)

    # Res Tree Files
    # (input dirname, output dirname, resfiles.mk path, mk version var, mk source var, use pool file, dep files)
    requests += generate_tree(config, glob, common_vars,
        "locales",
        None,
        "resfiles.mk",
        "GENRB_CLDR_VERSION",
        "GENRB_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "curr",
        "curr",
        "resfiles.mk",
        "CURR_CLDR_VERSION",
        "CURR_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "lang",
        "lang",
        "resfiles.mk",
        "LANG_CLDR_VERSION",
        "LANG_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "region",
        "region",
        "resfiles.mk",
        "REGION_CLDR_VERSION",
        "REGION_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "zone",
        "zone",
        "resfiles.mk",
        "ZONE_CLDR_VERSION",
        "ZONE_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "unit",
        "unit",
        "resfiles.mk",
        "UNIT_CLDR_VERSION",
        "UNIT_SOURCE",
        True,
        [])

    requests += generate_tree(config, glob, common_vars,
        "coll",
        "coll",
        "colfiles.mk",
        "COLLATION_CLDR_VERSION",
        "COLLATION_SOURCE",
        False,
        # Depends on timezoneTypes.res and keyTypeData.res.
        # TODO: We should not need this dependency to build collation.
        # TODO: Bake keyTypeData.res into the common library?
        [DepTarget("coll_ucadata"), DepTarget("misc_res")])

    requests += generate_tree(config, glob, common_vars,
        "brkitr",
        "brkitr",
        "brkfiles.mk",
        "BRK_RES_CLDR_VERSION",
        "BRK_RES_SOURCE",
        False,
        [DepTarget("brkitr_brk"), DepTarget("dictionaries")])

    requests += generate_tree(config, glob, common_vars,
        "rbnf",
        "rbnf",
        "rbnffiles.mk",
        "RBNF_CLDR_VERSION",
        "RBNF_SOURCE",
        False,
        [])

    requests += [
        ListRequest(
            name = "icudata_list",
            variable_name = "icudata_all_output_files",
            output_file = TmpFile("icudata.lst"),
            include_tmp = False
        )
    ]

    return (build_dirs, requests)


def generate_cnvalias(config, glob, common_vars):
    # UConv Name Aliases
    input_file = InFile("mappings/convrtrs.txt")
    output_file = OutFile("cnvalias.icu")
    return [
        SingleExecutionRequest(
            name = "cnvalias",
            category = "cnvalias",
            dep_targets = [],
            input_files = [input_file],
            output_files = [output_file],
            tool = IcuTool("gencnval"),
            args = "-s {IN_DIR} -d {OUT_DIR} "
                "{INPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_confusables(config, glob, common_vars):
    # CONFUSABLES
    txt1 = InFile("unidata/confusables.txt")
    txt2 = InFile("unidata/confusablesWholeScript.txt")
    cfu = OutFile("confusables.cfu")
    return [
        SingleExecutionRequest(
            name = "confusables",
            category = "confusables",
            dep_targets = [DepTarget("cnvalias")],
            input_files = [txt1, txt2],
            output_files = [cfu],
            tool = IcuTool("gencfu"),
            args = "-d {OUT_DIR} -i {OUT_DIR} "
                "-c -r {IN_DIR}/{INPUT_FILES[0]} -w {IN_DIR}/{INPUT_FILES[1]} "
                "-o {OUTPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_conversion_mappings(config, glob, common_vars):
    # UConv Conversion Table Files
    input_files = [InFile(filename) for filename in glob("mappings/*.ucm")]
    output_files = [OutFile("%s.cnv" % v.filename[9:-4]) for v in input_files]
    # TODO: handle BUILD_SPECIAL_CNV_FILES? Means to add --ignore-siso-check flag to makeconv
    return [
        RepeatedOrSingleExecutionRequest(
            name = "conversion_mappings",
            category = "conversion_mappings",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("makeconv"),
            args = "-s {IN_DIR} -d {OUT_DIR} -c {INPUT_FILE_PLACEHOLDER}",
            format_with = {},
            repeat_with = {
                "INPUT_FILE_PLACEHOLDER": utils.SpaceSeparatedList(file.filename for file in input_files)
            }
        )
    ]


def generate_brkitr_brk(config, glob, common_vars):
    # BRK Files
    input_files = [InFile(filename) for filename in glob("brkitr/rules/*.txt")]
    output_files = [OutFile("brkitr/%s.brk" % v.filename[13:-4]) for v in input_files]
    return [
        RepeatedExecutionRequest(
            name = "brkitr_brk",
            category = "brkitr_rules",
            dep_targets = [DepTarget("cnvalias")],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("genbrk"),
            args = "-d {OUT_DIR} -i {OUT_DIR} "
                "-c -r {IN_DIR}/{INPUT_FILE} "
                "-o {OUTPUT_FILE}",
            format_with = {},
            repeat_with = {}
        )
    ]


def generate_stringprep(config, glob, common_vars):
    # SPP FILES
    input_files = [InFile(filename) for filename in glob("sprep/*.txt")]
    output_files = [OutFile("%s.spp" % v.filename[6:-4]) for v in input_files]
    bundle_names = [v.filename[6:-4] for v in input_files]
    return [
        RepeatedExecutionRequest(
            name = "stringprep",
            category = "stringprep",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR}/sprep -d {OUT_DIR} -i {OUT_DIR} "
                "-b {BUNDLE_NAME} -m {IN_DIR}/unidata -u 3.2.0 {BUNDLE_NAME}.txt",
            format_with = {},
            repeat_with = {
                "BUNDLE_NAME": bundle_names
            }
        )
    ]


def generate_brkitr_dictionaries(config, glob, common_vars):
    # Dict Files
    input_files = [InFile(filename) for filename in glob("brkitr/dictionaries/*.txt")]
    output_files = [OutFile("brkitr/%s.dict" % v.filename[20:-4]) for v in input_files]
    extra_options_map = {
        "brkitr/dictionaries/burmesedict.txt": "--bytes --transform offset-0x1000",
        "brkitr/dictionaries/cjdict.txt": "--uchars",
        "brkitr/dictionaries/khmerdict.txt": "--bytes --transform offset-0x1780",
        "brkitr/dictionaries/laodict.txt": "--bytes --transform offset-0x0e80",
        "brkitr/dictionaries/thaidict.txt": "--bytes --transform offset-0x0e00"
    }
    extra_optionses = [extra_options_map[v.filename] for v in input_files]
    return [
        RepeatedExecutionRequest(
            name = "dictionaries",
            category = "brkitr_dictionaries",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("gendict"),
            args = "-i {OUT_DIR} "
                "-c {EXTRA_OPTIONS} "
                "{IN_DIR}/{INPUT_FILE} {OUT_DIR}/{OUTPUT_FILE}",
            format_with = {},
            repeat_with = {
                "EXTRA_OPTIONS": extra_optionses
            }
        )
    ]


def generate_normalization(config, glob, common_vars):
    # NRM Files
    input_files = [InFile(filename) for filename in glob("in/*.nrm")]
    input_files.remove(InFile("in/nfc.nrm"))  # nfc.nrm is pre-compiled into C++
    output_files = [OutFile(v.filename[3:]) for v in input_files]
    return [
        RepeatedExecutionRequest(
            name = "normalization",
            category = "normalization",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("icupkg"),
            args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILE} {OUT_DIR}/{OUTPUT_FILE}",
            format_with = {},
            repeat_with = {}
        )
    ]


def generate_coll_ucadata(config, glob, common_vars):
    # Collation Dependency File (ucadata.icu)
    input_file = InFile("in/coll/ucadata-%s.icu" % config.coll_han_type)
    output_file = OutFile("coll/ucadata.icu")
    return [
        SingleExecutionRequest(
            name = "coll_ucadata",
            category = "coll_ucadata",
            dep_targets = [],
            input_files = [input_file],
            output_files = [output_file],
            tool = IcuTool("icupkg"),
            args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILES[0]} {OUT_DIR}/{OUTPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_unames(config, glob, common_vars):
    # Unicode Character Names
    input_file = InFile("in/unames.icu")
    output_file = OutFile("unames.icu")
    return [
        SingleExecutionRequest(
            name = "unames",
            category = "unames",
            dep_targets = [],
            input_files = [input_file],
            output_files = [output_file],
            tool = IcuTool("icupkg"),
            args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILES[0]} {OUT_DIR}/{OUTPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_misc(config, glob, common_vars):
    # Misc Data Res Files
    input_files = [InFile(filename) for filename in glob("misc/*.txt")]
    input_basenames = [v.filename[5:] for v in input_files]
    output_files = [OutFile("%s.res" % v[:-4]) for v in input_basenames]
    return [
        RepeatedExecutionRequest(
            name = "misc_res",
            category = "misc",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR}/misc -d {OUT_DIR} -i {OUT_DIR} "
                "-k -q "
                "{INPUT_BASENAME}",
            format_with = {},
            repeat_with = {
                "INPUT_BASENAME": input_basenames
            }
        )
    ]


def generate_curr_supplemental(config, glob, common_vars):
    # Currency Supplemental Res File
    input_file = InFile("curr/supplementalData.txt")
    input_basename = "supplementalData.txt"
    output_file = OutFile("curr/supplementalData.res")
    return [
        SingleExecutionRequest(
            name = "curr_supplemental_res",
            category = "curr_supplemental",
            dep_targets = [],
            input_files = [input_file],
            output_files = [output_file],
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR}/curr -d {OUT_DIR}/curr -i {OUT_DIR} "
                "-k "
                "{INPUT_BASENAME}",
            format_with = {
                "INPUT_BASENAME": input_basename
            }
        )
    ]


def generate_translit(config, glob, common_vars):
    input_files = [
        InFile("translit/root.txt"),
        InFile("translit/en.txt"),
        InFile("translit/el.txt")
    ]
    input_basenames = [v.filename[9:] for v in input_files]
    output_files = [
        OutFile("translit/%s.res" % v[:-4])
        for v in input_basenames
    ]
    return [
        RepeatedOrSingleExecutionRequest(
            name = "translit_res",
            category = "translit",
            dep_targets = [],
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR}/translit -d {OUT_DIR}/translit -i {OUT_DIR} "
                "-k "
                "{INPUT_BASENAME}",
            format_with = {
            },
            repeat_with = {
                "INPUT_BASENAME": utils.SpaceSeparatedList(input_basenames)
            }
        )
    ]


def generate_tree(
        config,
        glob,
        common_vars,
        sub_dir,
        out_sub_dir,
        resfile_name,
        version_var,
        source_var,
        use_pool_bundle,
        dep_targets):
    requests = []
    category = "%s_tree" % sub_dir
    out_prefix = "%s/" % out_sub_dir if out_sub_dir else ""
    # TODO: Clean this up for curr
    input_files = [InFile(filename) for filename in glob("%s/*.txt" % sub_dir)]
    if sub_dir == "curr":
        input_files.remove(InFile("curr/supplementalData.txt"))
    input_basenames = [v.filename[len(sub_dir)+1:] for v in input_files]
    output_files = [
        OutFile("%s%s.res" % (out_prefix, v[:-4]))
        for v in input_basenames
    ]

    # Generate Pool Bundle
    if use_pool_bundle:
        input_pool_files = [OutFile("%spool.res" % out_prefix)]
        pool_target_name = "%s_pool_write" % sub_dir
        use_pool_bundle_option = "--usePoolBundle {OUT_DIR}/{OUT_PREFIX}".format(
            OUT_PREFIX = out_prefix,
            **common_vars
        )
        requests += [
            SingleExecutionRequest(
                name = pool_target_name,
                category = category,
                dep_targets = dep_targets,
                input_files = input_files,
                output_files = input_pool_files,
                tool = IcuTool("genrb"),
                args = "-s {IN_DIR}/{IN_SUB_DIR} -d {OUT_DIR}/{OUT_PREFIX} -i {OUT_DIR} "
                    "--writePoolBundle -k "
                    "{INPUT_BASENAMES_SPACED}",
                format_with = {
                    "IN_SUB_DIR": sub_dir,
                    "OUT_PREFIX": out_prefix,
                    "INPUT_BASENAMES_SPACED": utils.SpaceSeparatedList(input_basenames)
                }
            ),
        ]
        dep_targets = dep_targets + [DepTarget(pool_target_name)]
    else:
        use_pool_bundle_option = ""

    # Generate Res File Tree
    requests += [
        RepeatedOrSingleExecutionRequest(
            name = "%s_res" % sub_dir,
            category = category,
            dep_targets = dep_targets,
            input_files = input_files,
            output_files = output_files,
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR}/{IN_SUB_DIR} -d {OUT_DIR}/{OUT_PREFIX} -i {OUT_DIR} "
                "{EXTRA_OPTION} -k "
                "{INPUT_BASENAME}",
            format_with = {
                "IN_SUB_DIR": sub_dir,
                "OUT_PREFIX": out_prefix,
                "EXTRA_OPTION": use_pool_bundle_option
            },
            repeat_with = {
                "INPUT_BASENAME": utils.SpaceSeparatedList(input_basenames)
            }
        )
    ]

    # Generate index txt file
    # TODO: Change .mk files to .py files so they can be loaded directly.
    # Alternatively, figure out a way to not require reading this file altogether.
    # Right now, it is required for the index list file.
    # Reading these files as .py will be required for Bazel.
    mk_values = parse_makefile("{GLOB_DIR}/{IN_SUB_DIR}/{RESFILE_NAME}".format(
        IN_SUB_DIR = sub_dir,
        RESFILE_NAME = resfile_name,
        **common_vars
    ))
    cldr_version = mk_values[version_var] if version_var and sub_dir == "locales" else None
    index_input_files = [
        InFile("%s/%s" % (sub_dir, basename))
        for basename in mk_values[source_var].split()
    ]
    index_file_txt = TmpFile("{IN_SUB_DIR}/{INDEX_NAME}.txt".format(
        IN_SUB_DIR = sub_dir,
        **common_vars
    ))
    requests += [
        IndexTxtRequest(
            name = "%s_index_txt" % sub_dir,
            category = category,
            input_files = index_input_files,
            output_file = index_file_txt,
            cldr_version = cldr_version
        )
    ]

    # Generate index res file
    index_res_file = OutFile("{OUT_PREFIX}{INDEX_NAME}.res".format(
        OUT_PREFIX = out_prefix,
        **common_vars
    ))
    requests += [
        SingleExecutionRequest(
            name = "%s_index_res" % sub_dir,
            category = "%s_index" % sub_dir,
            dep_targets = [],
            input_files = [index_file_txt],
            output_files = [index_res_file],
            tool = IcuTool("genrb"),
            args = "-s {TMP_DIR}/{IN_SUB_DIR} -d {OUT_DIR}/{OUT_PREFIX} -i {OUT_DIR} "
                "-k "
                "{INDEX_NAME}.txt",
            format_with = {
                "IN_SUB_DIR": sub_dir,
                "OUT_PREFIX": out_prefix
            }
        )
    ]

    return requests
