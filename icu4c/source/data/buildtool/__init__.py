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

SingleExecutionRequest = namedtuple("SingleExecutionRequest", [
    "name",
    "input_files",
    "output_files",
    "tool",
    "args",
    "format_with"
])

RepeatedExecutionRequest = namedtuple("RepeatedExecutionRequest", [
    "name",
    "dep_files",
    "input_files",
    "output_files",
    "tool",
    "args",
    "format_with",
    "repeat_with"
])

RepeatedOrSingleExecutionRequest = namedtuple("RepeatedOrSingleExecutionRequest", [
    "name",
    "dep_files",
    "input_files",
    "output_files",
    "tool",
    "args",
    "format_with",
    "repeat_with",
    "flatten_with"
])

PrintFileRequest = namedtuple("PrintFileRequest", [
    "name",
    "output_file",
    "content"
])

CopyRequest = namedtuple("CopyRequest", [
    "name",
    "input_file",
    "output_file"
])

VariableRequest = namedtuple("VariableRequest", [
    "name",
    "input_files"
])
