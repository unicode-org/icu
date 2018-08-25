# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

from distutils.sysconfig import parse_makefile

from buildtool import *


def generate(config, glob, common_vars):
    build_dirs = ["{OUT_DIR}", "{TMP_DIR}"]

    requests = []
    requests += generate_rb(config, glob, common_vars)
    requests += generate_sprep(config, glob, common_vars)
    requests += generate_conv(config, glob, common_vars)
    requests += generate_other(config, glob, common_vars)
    requests += generate_copy(config, glob, common_vars)

    all_output_files = list(sorted(utils.get_all_output_files(requests)))
    all_output_files_with_tmp = list(sorted(utils.get_all_output_files(requests, include_tmp=True)))
    testdata_list_file = TmpFile("testdata.lst")
    requests += [
        PrintFileRequest(
            name = "testdata_list",
            output_file = testdata_list_file,
            content = "\n".join(file.filename for file in all_output_files)
        ),
        VariableRequest(
            name = "testdata_all_output_files",
            input_files = all_output_files_with_tmp + [testdata_list_file]
        )
    ]

    return (build_dirs, requests)


def generate_rb(config, glob, common_vars):
    mk_vars = parse_makefile("{GLOB_DIR}/tstfiles.mk".format(**common_vars))
    basenames = [v[:-4] for v in mk_vars["TEST_RES_SOURCE"].split()]
    basenames += [
        "casing",
        "mc",
        "root",
        "sh",
        "sh_YU",
        "te",
        "te_IN",
        "te_IN_REVISED",
        "testtypes",
        "testaliases",
        "testempty",
        "structLocale",
        "idna_rules",
        "conversion",
        "icuio",
        # "metaZones",
        # "timezoneTypes",
        # "windowsZones",
    ]
    return [
        # Inference rule for creating resource bundles
        # Some test data resource bundles are known to have warnings and bad data.
        # The -q option is there on purpose, so we don't see it normally.
        # TODO: Use option -k?
        RepeatedExecutionRequest(
            name = "testrb",
            dep_files = [],
            input_files = [InFile("%s.txt" % bn) for bn in basenames],
            output_files = [OutFile("%s.res" % bn) for bn in basenames],
            tool = IcuTool("genrb"),
            args = "-q -s {IN_DIR} -d {OUT_DIR} {INPUT_FILE}",
            format_with = {},
            repeat_with = {}
        ),
        # Other standalone res files
        SingleExecutionRequest(
            name = "encoded",
            input_files = [InFile("encoded.utf16be")],
            output_files = [OutFile("encoded.res")],
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR} -eUTF-16BE -d {OUT_DIR} {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "idna_rules",
            input_files = [InFile("idna_rules.txt")],
            output_files = [OutFile("idna_rules.res")],
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR} -d {OUT_DIR} {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "zoneinfo64",
            input_files = [InFile("zoneinfo64.txt")],
            output_files = [TmpFile("zoneinfo64.res")],
            tool = IcuTool("genrb"),
            args = "-s {IN_DIR} -d {TMP_DIR} {INPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_sprep(config, glob, common_vars):
    return [
        SingleExecutionRequest(
            name = "nfscsi",
            input_files = [InFile("nfs4_cs_prep_ci.txt")],
            output_files = [OutFile("nfscsi.spp")],
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR} -d {OUT_DIR} -b nfscsi -u 3.2.0 {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "nfscss",
            input_files = [InFile("nfs4_cs_prep_cs.txt")],
            output_files = [OutFile("nfscss.spp")],
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR} -d {OUT_DIR} -b nfscss -u 3.2.0 {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "nfscis",
            input_files = [InFile("nfs4_cis_prep.txt")],
            output_files = [OutFile("nfscis.spp")],
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR} -d {OUT_DIR} -b nfscis -u 3.2.0 -k -n {IN_DIR}/../../data/unidata {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "nfsmxs",
            input_files = [InFile("nfs4_mixed_prep_s.txt")],
            output_files = [OutFile("nfsmxs.spp")],
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR} -d {OUT_DIR} -b nfsmxs -u 3.2.0 -k -n {IN_DIR}/../../data/unidata {INPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "nfsmxp",
            input_files = [InFile("nfs4_mixed_prep_p.txt")],
            output_files = [OutFile("nfsmxp.spp")],
            tool = IcuTool("gensprep"),
            args = "-s {IN_DIR} -d {OUT_DIR} -b nfsmxp -u 3.2.0 -k -n {IN_DIR}/../../data/unidata {INPUT_FILES[0]}",
            format_with = {}
        )
    ]


def generate_conv(config, glob, common_vars):
    basenames = [
        "test1",
        "test1bmp",
        "test2",
        "test3",
        "test4",
        "test4x",
        "test5",
        "ibm9027"
    ]
    return [
        RepeatedExecutionRequest(
            name = "test_conv",
            dep_files = [],
            input_files = [InFile("%s.ucm" % bn) for bn in basenames],
            output_files = [OutFile("%s.cnv" % bn) for bn in basenames],
            tool = IcuTool("makeconv"),
            args = "--small -d {OUT_DIR} {IN_DIR}/{INPUT_FILE}",
            format_with = {},
            repeat_with = {}
        )
    ]


def generate_copy(config, glob, common_vars):
    return [
        CopyRequest(
            name = "nam_typ",
            input_file = OutFile("te.res"),
            output_file = TmpFile("nam.typ")
        ),
        CopyRequest(
            name = "old_l_testtypes",
            input_file = InFile("old_l_testtypes.res"),
            output_file = OutFile("old_l_testtypes.res")
        ),
        CopyRequest(
            name = "old_e_testtypes",
            input_file = InFile("old_e_testtypes.res"),
            output_file = OutFile("old_e_testtypes.res")
        ),
    ]


def generate_other(config, glob, common_vars):
    return [
        SingleExecutionRequest(
            name = "testnorm",
            input_files = [InFile("testnorm.txt")],
            output_files = [OutFile("testnorm.nrm")],
            tool = IcuTool("gennorm2"),
            args = "-s {IN_DIR} {INPUT_FILES[0]} -o {OUT_DIR}/{OUTPUT_FILES[0]}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "test_icu",
            input_files = [],
            output_files = [OutFile("test.icu")],
            tool = IcuTool("gentest"),
            args = "-d {OUT_DIR}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "testtable32_txt",
            input_files = [],
            output_files = [TmpFile("testtable32.txt")],
            tool = IcuTool("gentest"),
            args = "-r -d {TMP_DIR}",
            format_with = {}
        ),
        SingleExecutionRequest(
            name = "testtable32_res",
            input_files = [TmpFile("testtable32.txt")],
            output_files = [OutFile("testtable32.res")],
            tool = IcuTool("genrb"),
            args = "-s {TMP_DIR} -d {OUT_DIR} {INPUT_FILES[0]}",
            format_with = {}
        )
    ]
