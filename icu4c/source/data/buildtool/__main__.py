# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Python 2/3 Compatibility (ICU-20299)
# TODO(ICU-20301): Remove this.
from __future__ import print_function

import argparse
import glob as pyglob
import json
import sys

from . import *
from .renderers import makefile, windirect
from . import filtration, utils
import BUILDRULES

flag_parser = argparse.ArgumentParser(
    description = """Generates rules for building ICU binary data files from text
and other input files in source control.

You can select features using either the --whitelist or --blacklist option.
Available features include:

{AVAILABLE_FEATURES}
""".format(AVAILABLE_FEATURES = "\n".join("    %s" % v for v in AVAILABLE_FEATURES)),
    formatter_class = argparse.RawDescriptionHelpFormatter
)
flag_parser.add_argument(
    "--format",
    help = "How to output the rules to run to build ICU data.",
    choices = ["gnumake", "windirect"],
    required = True
)
flag_parser.add_argument(
    "--glob_dir",
    help = "Path to data input folder (icu4c/source/data) when this script is being run.",
    default = "."
)
flag_parser.add_argument(
    "--in_dir",
    help = "Path to data input folder (icu4c/source/data) for file processing. Not used in gnumake format.",
    default = "."
)
flag_parser.add_argument(
    "--out_dir",
    help = "Path to where to save output data files. Not used in gnumake format.",
    default = "icudata"
)
flag_parser.add_argument(
    "--tmp_dir",
    help = "Path to where to save temporary files. Not used in gnumake format.",
    default = "icutmp"
)
flag_parser.add_argument(
    "--tool_dir",
    help = "Path to where to find binary tools (genrb, genbrk, etc). Used for 'windirect' format only.",
    default = "../tool"
)
flag_parser.add_argument(
    "--tool_cfg",
    help = "The build configuration of the tools. Used for 'windirect' format only.",
    default = "x86/Debug"
)
flag_parser.add_argument(
    "--seqmode",
    help = "Whether to optimize rules to be run sequentially (fewer threads) or in parallel (many threads).",
    choices = ["sequential", "parallel"],
    default = "parallel"
)
flag_parser.add_argument(
    "--collation_ucadata",
    help = "Which data set to use for ucadata in collation.",
    choices = ["unihan", "implicithan"],
    default = "unihan"
)
features_group = flag_parser.add_mutually_exclusive_group()
features_group.add_argument(
    "--blacklist",
    metavar = "FEATURE",
    help = "A list of one or more features to disable; all others will be enabled by default. New users should favor a blacklist to ensure important data is not left out.",
    nargs = "+",
    choices = AVAILABLE_FEATURES
)
features_group.add_argument(
    "--whitelist",
    metavar = "FEATURE",
    help = "A list of one or more features to enable; all others will be disabled by default.",
    nargs = "+",
    choices = AVAILABLE_FEATURES
)
features_group.add_argument(
    "--filter_file",
    help = "A path to a filter file.",
    default = None
)


class Config(object):

    def __init__(self, args):
        # Process arguments
        if args.whitelist:
            self._feature_set = set(args.whitelist)
        elif args.blacklist:
            self._feature_set = set(AVAILABLE_FEATURES) - set(args.blacklist)
        else:
            self._feature_set = set(AVAILABLE_FEATURES)
        self.max_parallel = (args.seqmode == "parallel")
        # Either "unihan" or "implicithan"
        self.coll_han_type = args.collation_ucadata

        # Default fields before processing filter file
        self.filters_json_data = {}

        # Process filter file
        if args.filter_file:
            try:
                with open(args.filter_file, "r") as f:
                    print("Note: Applying filters from %s." % args.filter_file, file=sys.stderr)
                    try:
                        import hjson
                        self.filters_json_data = hjson.load(f)
                    except ImportError:
                        self.filters_json_data = json.load(f)
            except IOError:
                print("Error: Could not read filter file %s." % args.filter_file, file=sys.stderr)
                exit(1)

    def has_feature(self, feature_name):
        assert feature_name in AVAILABLE_FEATURES
        return feature_name in self._feature_set


def main():
    args = flag_parser.parse_args()
    config = Config(args)

    if args.format == "gnumake":
        makefile_vars = {
            "IN_DIR": "$(srcdir)",
            "INDEX_NAME": "res_index"
        }
        makefile_env = ["ICUDATA_CHAR", "OUT_DIR", "TMP_DIR"]
        common = {
            key: "$(%s)" % key
            for key in list(makefile_vars.keys()) + makefile_env
        }
        common["GLOB_DIR"] = args.glob_dir
    else:
        common = {
            "GLOB_DIR": args.glob_dir,
            "IN_DIR": args.in_dir,
            "OUT_DIR": args.out_dir,
            "TMP_DIR": args.tmp_dir,
            "INDEX_NAME": "res_index",
            # TODO: Pull this from configure script:
            "ICUDATA_CHAR": "l"
        }

    def glob(pattern):
        result_paths = pyglob.glob("{IN_DIR}/{PATTERN}".format(
            IN_DIR = args.glob_dir,
            PATTERN = pattern
        ))
        # For the purposes of buildtool, force Unix-style directory separators.
        return [v.replace("\\", "/")[len(args.glob_dir)+1:] for v in sorted(result_paths)]

    build_dirs, requests = BUILDRULES.generate(config, glob, common)
    requests = filtration.apply_filters(requests, config)
    requests = utils.flatten_requests(requests, config, common)

    if args.format == "gnumake":
        print(makefile.get_gnumake_rules(
            build_dirs,
            requests,
            makefile_vars,
            common_vars = common
        ))
    elif args.format == "windirect":
        return windirect.run(
            build_dirs,
            requests,
            common_vars = common,
            tool_dir = args.tool_dir,
            tool_cfg = args.tool_cfg
        )
    else:
        print("Format not supported: %s" % args.format)
        return 1
    return 0

if __name__ == "__main__":
    exit(main())
