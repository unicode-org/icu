#!/usr/bin/env python3
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Regression tests for the makeconv command-line tool."""

import argparse
import filecmp
import os
from pathlib import Path
import subprocess
import sys
import tempfile


def run_tool(arguments):
    print("Running:", " ".join(str(arg) for arg in arguments), flush=True)
    subprocess.run([str(arg) for arg in arguments], check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--makeconv", required=True, type=Path)
    parser.add_argument("--testdata-dir", required=True, type=Path)
    args = parser.parse_args()

    if os.name == "nt" or sys.platform.startswith(("cygwin", "msys")):
        print("Skipping makeconv long-path test on Windows")
        return

    makeconv = args.makeconv.resolve()
    testdata_dir = args.testdata_dir.resolve()

    with tempfile.TemporaryDirectory(prefix="icu-makeconv-test-") as temp_name:
        temp_dir = Path(temp_name)
        short_output = temp_dir / "short-output"
        long_output = temp_dir / "long-output"
        short_output.mkdir()
        long_output.mkdir()

        # Keep the physical path short while exercising a lexical path longer
        # than the former 500-byte buffer. "filters/.." resolves back to the
        # testdata directory on POSIX filesystems.
        long_input_dir = str(testdata_dir)
        extension_name = os.sep + "test4x.ucm"
        while len(os.fsencode(long_input_dir + extension_name)) <= 550:
            long_input_dir += os.sep + "filters" + os.sep + ".."
        long_input = long_input_dir + extension_name

        run_tool([
            makeconv,
            "-d", short_output,
            testdata_dir / "test4x.ucm",
        ])
        run_tool([
            makeconv,
            "-d", long_output,
            long_input,
        ])

        short_cnv = short_output / "test4x.cnv"
        long_cnv = long_output / "test4x.cnv"
        if not filecmp.cmp(short_cnv, long_cnv, shallow=False):
            raise RuntimeError("makeconv produced different output for a long path")


if __name__ == "__main__":
    main()
