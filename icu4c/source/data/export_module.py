#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2022 L. E. Segovia <amy@amyspark.me>
# SPDX-License-Identifier: MIT

from argparse import ArgumentParser, FileType
from sys import stdout

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('symbol', type=str, help='Symbol to export')
    parser.add_argument('outfile', nargs='?', type=FileType('w', encoding='utf-8'),
        default=stdout, help='Module definition file')
    args = parser.parse_args()

    with args.outfile as f:
        f.write("EXPORTS\n")
        f.write(f"\t{args.symbol}\n")
