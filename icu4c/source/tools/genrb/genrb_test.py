#!/usr/bin/env python3
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Regression tests for the genrb command line tool.

Checks that resource bundle source files with an overlong file name in a
process(transliterator) or process(dependency) resource are handled without
overrunning the file name buffers. See ICU-23078.
"""

import argparse
from pathlib import Path
import subprocess
import sys
import tempfile


def run_genrb(genrb, arguments, expect_success):
    command = [str(genrb)] + [str(arg) for arg in arguments]
    print("Running:", " ".join(command), flush=True)
    completed = subprocess.run(command, capture_output=True, text=True)
    if expect_success and completed.returncode != 0:
        print(completed.stdout)
        print(completed.stderr)
        raise SystemExit(
            "genrb failed on a valid bundle, exit code %d" % completed.returncode
        )
    if not expect_success and completed.returncode == 0:
        print(completed.stdout)
        print(completed.stderr)
        raise SystemExit("genrb unexpectedly accepted a bundle with an unopenable file")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--genrb", required=True, help="path of the genrb binary")
    args = parser.parse_args()
    genrb = Path(args.genrb)

    with tempfile.TemporaryDirectory() as temp:
        work = Path(temp)
        source_dir = work / "src"
        output_dir = work / "out"
        source_dir.mkdir()
        output_dir.mkdir()

        (source_dir / "rules.txt").write_text("a > b;\n", encoding="utf-8")

        # A transliterator reference with a normal file name must work.
        (source_dir / "good.txt").write_text(
            'test:table {\n'
            '    resource:process(transliterator){"rules.txt"}\n'
            '}\n',
            encoding="utf-8",
        )
        run_genrb(
            genrb,
            ["-i", source_dir, "-d", output_dir, source_dir / "good.txt"],
            expect_success=True,
        )
        if not (output_dir / "test.res").is_file():
            raise SystemExit("genrb did not write test.res for a valid bundle")

        # A transliterator reference with an overlong file name used to
        # overrun the fixed size file name buffers. The file cannot be
        # opened, which must be reported as a clean error.
        (source_dir / "long.txt").write_text(
            'test:table {\n'
            '    resource:process(transliterator){"%s.txt"}\n'
            '}\n' % ("a" * 300),
            encoding="utf-8",
        )
        run_genrb(
            genrb,
            ["-i", source_dir, "-d", output_dir, source_dir / "long.txt"],
            expect_success=False,
        )

        # A dependency with an overlong file name only produces a warning;
        # it must not crash the tool.
        (source_dir / "longdep.txt").write_text(
            'test:table {\n'
            '    resource:process(dependency){"%s.txt"}\n'
            '}\n' % ("b" * 300),
            encoding="utf-8",
        )
        run_genrb(
            genrb,
            ["-i", source_dir, "-d", output_dir, source_dir / "longdep.txt"],
            expect_success=True,
        )
        if not (output_dir / "test.res").is_file():
            raise SystemExit("genrb did not write test.res for a dependency bundle")

    print("genrb tests passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
