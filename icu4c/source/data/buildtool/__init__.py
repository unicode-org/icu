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
    # Used for identification purposes
    "name",

    # The filter category that applies to this request
    "category",

    # Dependency files; usually generated
    "dep_files",

    # Primary input files
    "input_files",

    # Output files
    "output_files",

    # What tool to use
    "tool",

    # Argument string to pass to the tool with optional placeholders
    "args",

    # Placeholders to substitute into the argument string; if any of these
    # have a list type, the list must be equal in length to input_files
    "format_with"
])

RepeatedExecutionRequest = namedtuple("RepeatedExecutionRequest", [
    # Used for identification purposes
    "name",

    # The filter category that applies to this request
    "category",

    # Dependency files; usually generated
    "dep_files",

    # Primary input files
    "input_files",

    # Output files
    "output_files",

    # What tool to use
    "tool",

    # Argument string to pass to the tool with optional placeholders
    "args",

    # Placeholders to substitute into the argument string for all executions;
    # if any of these have a list type, the list must be equal in length to
    # input_files
    "format_with",

    # Placeholders to substitute into the argument string unique to each
    # iteration; all values must be lists equal in length to input_files
    "repeat_with"
])

RepeatedOrSingleExecutionRequest = namedtuple("RepeatedOrSingleExecutionRequest", [
    "name",
    "category",
    "dep_files",
    "input_files",
    "output_files",
    "tool",
    "args",
    "format_with",
    "repeat_with"
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

ListRequest = namedtuple("ListRequest", [
    "name",
    "variable_name",
    "output_file",
    "include_tmp"
])

IndexTxtRequest = namedtuple("IndexTxtRequest", [
    "name",
    "category",
    "input_files",
    "output_file",
    "cldr_version"
])
