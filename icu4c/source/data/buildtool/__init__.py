# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

from collections import namedtuple

AVAILABLE_FEATURES = [
    "confusables",
    "cnvalias",
    "uconv",
    "brkitr",
    "stringprep",
    "dictionaries",
    "normalization",
    "coll",
    "unames",
    "misc",
    "locales",
    "curr",
    "lang",
    "region",
    "zone",
    "unit",
    "rbnf",
    "translit"
]

InFile = namedtuple("InFile", ["filename"])
TmpFile = namedtuple("TmpFile", ["filename"])
OutFile = namedtuple("OutFile", ["filename"])
PkgFile = namedtuple("PkgFile", ["filename"])

IcuTool = namedtuple("IcuTool", ["name"])
SystemTool = namedtuple("SystemTool", ["name"])

DepTarget = namedtuple("DepTarget", ["name"])
