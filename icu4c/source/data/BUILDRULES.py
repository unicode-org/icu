# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

from __future__ import print_function
from distutils.sysconfig import parse_makefile

from buildtool import *
from buildtool import utils

import sys


def generate(config, glob, common_vars):
    requests = []
    pkg_exclusions = set()

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

    # UConv Name Aliases
    if config.has_feature("cnvalias"):
        input_file = InFile("mappings/convrtrs.txt")
        output_file = OutFile("cnvalias.icu")
        requests += [
            SingleExecutionRequest(
                name = "cnvalias",
                input_files = [input_file],
                output_files = [output_file],
                tool = IcuTool("gencnval"),
                args = "-s {IN_DIR} -d {OUT_DIR} "
                    "{INPUT_FILES[0]}",
                format_with = {}
            )
        ]

    # CONFUSABLES
    if config.has_feature("confusables"):
        txt1 = InFile("unidata/confusables.txt")
        txt2 = InFile("unidata/confusablesWholeScript.txt")
        cfu = OutFile("confusables.cfu")
        requests += [
            SingleExecutionRequest(
                name = "confusables",
                input_files = [txt1, txt2, OutFile("cnvalias.icu")],
                output_files = [cfu],
                tool = IcuTool("gencfu"),
                args = "-d {OUT_DIR} -i {OUT_DIR} "
                    "-c -r {IN_DIR}/{INPUT_FILES[0]} -w {IN_DIR}/{INPUT_FILES[1]} "
                    "-o {OUTPUT_FILES[0]}",
                format_with = {}
            )
        ]

    # UConv Conversion Table Files
    if config.has_feature("uconv"):
        input_files = [InFile(filename) for filename in glob("mappings/*.ucm")]
        output_files = [OutFile("%s.cnv" % v.filename[9:-4]) for v in input_files]
        # TODO: handle BUILD_SPECIAL_CNV_FILES? Means to add --ignore-siso-check flag to makeconv
        requests += [
            RepeatedOrSingleExecutionRequest(
                name = "uconv",
                dep_files = [],
                input_files = input_files,
                output_files = output_files,
                tool = IcuTool("makeconv"),
                args = "-s {IN_DIR} -d {OUT_DIR} -c {INPUT_FILE_PLACEHOLDER}",
                format_with = {},
                repeat_with = {
                    "INPUT_FILE_PLACEHOLDER": [file.filename for file in input_files]
                },
                flatten_with = {
                    "INPUT_FILE_PLACEHOLDER": " ".join(file.filename for file in input_files)
                }
            )
        ]

    # BRK Files
    brkitr_brk_files = []
    if config.has_feature("brkitr"):
        input_files = [InFile(filename) for filename in glob("brkitr/rules/*.txt")]
        output_files = [OutFile("brkitr/%s.brk" % v.filename[13:-4]) for v in input_files]
        brkitr_brk_files += output_files
        requests += [
            RepeatedExecutionRequest(
                name = "brkitr_brk",
                dep_files = [OutFile("cnvalias.icu")],
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

    # SPP FILES
    if config.has_feature("stringprep"):
        input_files = [InFile(filename) for filename in glob("sprep/*.txt")]
        output_files = [OutFile("%s.spp" % v.filename[6:-4]) for v in input_files]
        bundle_names = [v.filename[6:-4] for v in input_files]
        requests += [
            RepeatedExecutionRequest(
                name = "stringprep",
                dep_files = [],
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

    # Dict Files
    dict_files = []
    if config.has_feature("dictionaries"):
        input_files = [InFile(filename) for filename in glob("brkitr/dictionaries/*.txt")]
        output_files = [OutFile("brkitr/%s.dict" % v.filename[20:-4]) for v in input_files]
        dict_files += output_files
        extra_options_map = {
            "brkitr/dictionaries/burmesedict.txt": "--bytes --transform offset-0x1000",
            "brkitr/dictionaries/cjdict.txt": "--uchars",
            "brkitr/dictionaries/khmerdict.txt": "--bytes --transform offset-0x1780",
            "brkitr/dictionaries/laodict.txt": "--bytes --transform offset-0x0e80",
            "brkitr/dictionaries/thaidict.txt": "--bytes --transform offset-0x0e00"
        }
        extra_optionses = [extra_options_map[v.filename] for v in input_files]
        requests += [
            RepeatedExecutionRequest(
                name = "dictionaries",
                dep_files = [],
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

    # NRM Files
    if config.has_feature("normalization"):
        input_files = [InFile(filename) for filename in glob("in/*.nrm")]
        input_files.remove(InFile("in/nfc.nrm"))  # nfc.nrm is pre-compiled into C++
        output_files = [OutFile(v.filename[3:]) for v in input_files]
        requests += [
            RepeatedExecutionRequest(
                name = "normalization",
                dep_files = [],
                input_files = input_files,
                output_files = output_files,
                tool = IcuTool("icupkg"),
                args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILE} {OUT_DIR}/{OUTPUT_FILE}",
                format_with = {},
                repeat_with = {}
            )
        ]

    # Collation Dependency File (ucadata.icu)
    if config.has_feature("coll"):
        input_file = InFile("in/coll/ucadata-%s.icu" % config.coll_han_type())
        output_file = OutFile("coll/ucadata.icu")
        requests += [
            SingleExecutionRequest(
                name = "coll_ucadata",
                input_files = [input_file],
                output_files = [output_file],
                tool = IcuTool("icupkg"),
                args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILES[0]} {OUT_DIR}/{OUTPUT_FILES[0]}",
                format_with = {}
            )
        ]

    # Unicode Character Names
    if config.has_feature("unames"):
        input_file = InFile("in/unames.icu")
        output_file = OutFile("unames.icu")
        requests += [
            SingleExecutionRequest(
                name = "unames",
                input_files = [input_file],
                output_files = [output_file],
                tool = IcuTool("icupkg"),
                args = "-t{ICUDATA_CHAR} {IN_DIR}/{INPUT_FILES[0]} {OUT_DIR}/{OUTPUT_FILES[0]}",
                format_with = {}
            )
        ]

    # Misc Data Res Files
    if config.has_feature("misc"):
        # TODO: Treat each misc file separately
        input_files = [InFile(filename) for filename in glob("misc/*.txt")]
        input_basenames = [v.filename[5:] for v in input_files]
        output_files = [OutFile("%s.res" % v[:-4]) for v in input_basenames]
        requests += [
            RepeatedExecutionRequest(
                name = "misc",
                dep_files = [],
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

    # Specialized Locale Data Res Files
    specialized_sub_dirs = [
        # (input dirname, output dirname, resfiles.mk path, mk version var, mk source var, use pool file, dep files)
        ("locales",  None,       "resfiles.mk",  "GENRB_CLDR_VERSION",     "GENRB_SOURCE",     True,
            []),
        ("curr",     "curr",     "resfiles.mk",  "CURR_CLDR_VERSION",      "CURR_SOURCE",      True,
            []),
        ("lang",     "lang",     "resfiles.mk",  "LANG_CLDR_VERSION",      "LANG_SOURCE",      True,
            []),
        ("region",   "region",   "resfiles.mk",  "REGION_CLDR_VERSION",    "REGION_SOURCE",    True,
            []),
        ("zone",     "zone",     "resfiles.mk",  "ZONE_CLDR_VERSION",      "ZONE_SOURCE",      True,
            []),
        ("unit",     "unit",     "resfiles.mk",  "UNIT_CLDR_VERSION",      "UNIT_SOURCE",      True,
            []),
        # TODO: We should not need timezoneTypes.res to build collation resource bundles.
        # TODO: Maybe keyTypeData.res should be baked into the common library.
        ("coll",     "coll",     "colfiles.mk",  "COLLATION_CLDR_VERSION", "COLLATION_SOURCE", False,
            [OutFile("coll/ucadata.icu"), OutFile("timezoneTypes.res"), OutFile("keyTypeData.res")]),
        ("brkitr",   "brkitr",   "brkfiles.mk",  "BRK_RES_CLDR_VERSION",   "BRK_RES_SOURCE",   False,
            brkitr_brk_files + dict_files),
        ("rbnf",     "rbnf",     "rbnffiles.mk", "RBNF_CLDR_VERSION",      "RBNF_SOURCE",      False,
            []),
        ("translit", "translit", "trnsfiles.mk", None,                     "TRANSLIT_SOURCE",  False,
            [])
    ]

    for sub_dir, out_sub_dir, resfile_name, version_var, source_var, use_pool_bundle, dep_files in specialized_sub_dirs:
        out_prefix = "%s/" % out_sub_dir if out_sub_dir else ""
        if config.has_feature(sub_dir):
            # TODO: Clean this up for translit
            if sub_dir == "translit":
                input_files = [
                    InFile("translit/root.txt"),
                    InFile("translit/en.txt"),
                    InFile("translit/el.txt")
                ]
            else:
                input_files = [InFile(filename) for filename in glob("%s/*.txt" % sub_dir)]
            input_basenames = [v.filename[len(sub_dir)+1:] for v in input_files]
            output_files = [
                OutFile("%s%s.res" % (out_prefix, v[:-4]))
                for v in input_basenames
            ]
            if use_pool_bundle:
                input_pool_files = [OutFile("%spool.res" % out_prefix)]
                use_pool_bundle_option = "--usePoolBundle {OUT_DIR}/{OUT_PREFIX}".format(
                    OUT_PREFIX = out_prefix,
                    **common_vars
                )
                requests += [
                    SingleExecutionRequest(
                        name = "%s_pool_write" % sub_dir,
                        input_files = dep_files + input_files,
                        output_files = input_pool_files,
                        tool = IcuTool("genrb"),
                        args = "-s {IN_DIR}/{IN_SUB_DIR} -d {OUT_DIR}/{OUT_PREFIX} -i {OUT_DIR} "
                            "--writePoolBundle -k "
                            "{INPUT_BASENAMES_SPACED}",
                        format_with = {
                            "IN_SUB_DIR": sub_dir,
                            "OUT_PREFIX": out_prefix,
                            "INPUT_BASENAMES_SPACED": " ".join(input_basenames)
                        }
                    ),
                ]
            else:
                input_pool_files = []
                use_pool_bundle_option = ""
            requests += [
                RepeatedOrSingleExecutionRequest(
                    name = "%s_res" % sub_dir,
                    dep_files = dep_files + input_pool_files,
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
                        "INPUT_BASENAME": input_basenames,
                    },
                    flatten_with = {
                        "INPUT_BASENAME": " ".join(input_basenames)
                    }
                )
            ]
            # Generate index txt file
            if sub_dir != "translit":
                # TODO: Change .mk files to .py files so they can be loaded directly.
                # Alternatively, figure out a way to require reading this file altogether.
                # Right now, it is required for the index list file.
                # Reading these files as .py will be required for Bazel.
                mk_values = parse_makefile("{GLOB_DIR}/{IN_SUB_DIR}/{RESFILE_NAME}".format(
                    IN_SUB_DIR = sub_dir,
                    RESFILE_NAME = resfile_name,
                    **common_vars
                ))
                cldr_version = mk_values[version_var] if version_var and sub_dir == "locales" else None
                locales = [v[:-4] for v in mk_values[source_var].split()]
                pkg_exclusions |= set(output_files) - set(OutFile("%s%s.res" % (out_prefix, locale)) for locale in locales)
                index_file_txt = TmpFile("{IN_SUB_DIR}/{INDEX_NAME}.txt".format(
                    IN_SUB_DIR = sub_dir,
                    **common_vars
                ))
                requests += [
                    PrintFileRequest(
                        name = "%s_index_txt" % sub_dir,
                        output_file = index_file_txt,
                        content = utils.generate_index_file(locales, cldr_version, common_vars)
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

    # Finally, make the package.
    all_output_files = list(sorted(utils.get_all_output_files(requests)))
    icudata_list_file = TmpFile("icudata.lst")
    requests += [
        PrintFileRequest(
            name = "icudata_list",
            output_file = icudata_list_file,
            content = "\n".join(file.filename for file in all_output_files)
        ),
        VariableRequest(
            name = "icudata_all_output_files",
            input_files = all_output_files + [icudata_list_file]
        )
    ]

    return (build_dirs, requests)
