# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

import sys

from . import *


def dir_for(file):
    if isinstance(file, InFile):
        return "{IN_DIR}"
    if isinstance(file, TmpFile):
        return "{TMP_DIR}"
    if isinstance(file, OutFile):
        return "{OUT_DIR}"
    if isinstance(file, PkgFile):
        return "{PKG_DIR}"
    assert False


def concat_dicts(*dicts):
    # There is not a super great way to do this in Python:
    new_dict = {}
    for dict in dicts:
        new_dict.update(dict)
    return new_dict


def repeated_execution_request_looper(request):
    # dictionary of lists to list of dictionaries:
    ld = [
        dict(zip(request.repeat_with, t))
        for t in zip(*request.repeat_with.values())
    ]
    if not ld:
        # No special options given in repeat_with
        ld = [{} for _ in range(len(request.input_files))]
    return zip(ld, request.input_files, request.output_files)


def format_single_request_command(request, cmd_template, common_vars):
    return cmd_template.format(
        ARGS = request.args.format(
            INPUT_FILES = [file.filename for file in request.input_files],
            OUTPUT_FILES = [file.filename for file in request.output_files],
            **concat_dicts(common_vars, request.format_with)
        )
    )


def format_repeated_request_command(request, cmd_template, loop_vars, common_vars):
    (iter_vars, input_file, output_file) = loop_vars
    return cmd_template.format(
        ARGS = request.args.format(
            INPUT_FILE = input_file.filename,
            OUTPUT_FILE = output_file.filename,
            **concat_dicts(common_vars, request.format_with, iter_vars)
        )
    )


def dep_targets_to_files(this_request, all_requests):
    if not this_request.dep_files:
        return []
    dep_files = []
    for dep_target in this_request.dep_files:
        for request in all_requests:
            if request.name == dep_target.name:
                dep_files += get_output_files(request)
                break
        else:
            print("Warning: Unable to find target %s, a dependency of %s" % (
                dep_target.name,
                this_request.name
            ), file=sys.stderr)
    return dep_files


def flatten_requests(raw_requests, config, common_vars):
    """Post-processes "meta" requests into normal requests.

    Affected classes:
    - RepeatedOrSingleExecutionRequest becomes either 
      RepeatedExecutionRequest or SingleExecutionRequest
    - ListRequest becomes PrintFileRequest and VariableRequest
    - IndexTxtRequest becomes PrintFileRequest
    """
    flattened_requests = []
    for request in raw_requests:
        if isinstance(request, RepeatedOrSingleExecutionRequest):
            if config.max_parallel:
                flattened_requests.append(RepeatedExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = dep_targets_to_files(
                        request, raw_requests
                    ),
                    input_files = request.input_files,
                    output_files = request.output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = request.format_with,
                    repeat_with = request.repeat_with
                ))
            else:
                flattened_requests.append(SingleExecutionRequest(
                    name = request.name,
                    category = request.category,
                    input_files = request.input_files + dep_targets_to_files(
                        request, raw_requests
                    ),
                    output_files = request.output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = concat_dicts(request.format_with, request.repeat_with)
                ))
        elif isinstance(request, SingleExecutionRequest):
            flattened_requests += [
                SingleExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = dep_targets_to_files(
                        request, raw_requests
                    ),
                    input_files = request.input_files,
                    output_files = request.output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = request.format_with
                )
            ]
        elif isinstance(request, RepeatedExecutionRequest):
            flattened_requests += [
                RepeatedExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = dep_targets_to_files(
                        request, raw_requests
                    ),
                    input_files = request.input_files,
                    output_files = request.output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = request.format_with,
                    repeat_with = request.repeat_with
                )
            ]
        elif isinstance(request, ListRequest):
            list_files = list(sorted(get_all_output_files(raw_requests)))
            if request.include_tmp:
                variable_files = list(sorted(get_all_output_files(raw_requests, include_tmp=True)))
            else:
                # Always include the list file itself
                variable_files = list_files + [request.output_file]
            flattened_requests += [
                PrintFileRequest(
                    name = request.name,
                    output_file = request.output_file,
                    content = "\n".join(file.filename for file in list_files)
                ),
                VariableRequest(
                    name = request.variable_name,
                    input_files = variable_files
                )
            ]
        elif isinstance(request, IndexTxtRequest):
            flattened_requests += [
                PrintFileRequest(
                    name = request.name,
                    output_file = request.output_file,
                    content = generate_index_file(request.input_files, request.cldr_version, common_vars)
                )
            ]
        else:
            flattened_requests.append(request)
    return flattened_requests


def generate_index_file(input_files, cldr_version, common_vars):
    locales = [f.filename[f.filename.rfind("/")+1:-4] for f in input_files]
    formatted_version = "    CLDRVersion { \"%s\" }\n" % cldr_version if cldr_version else ""
    formatted_locales = "\n".join(["        %s {\"\"}" % v for v in locales])
    # TODO: CLDRVersion is required only in the base file
    return ("// Warning this file is automatically generated\n"
            "{INDEX_NAME}:table(nofallback) {{\n"
            "{FORMATTED_VERSION}"
            "    InstalledLocales {{\n"
            "{FORMATTED_LOCALES}\n"
            "    }}\n"
            "}}").format(
                FORMATTED_VERSION = formatted_version,
                FORMATTED_LOCALES = formatted_locales,
                **common_vars
            )


def get_input_files(request):
    if isinstance(request, SingleExecutionRequest):
        return request.dep_files + request.input_files
    elif isinstance(request, RepeatedExecutionRequest):
        return request.dep_files + request.input_files
    elif isinstance(request, RepeatedOrSingleExecutionRequest):
        return request.dep_files + request.input_files
    elif isinstance(request, PrintFileRequest):
        return []
    elif isinstance(request, CopyRequest):
        return [request.input_file]
    elif isinstance(request, VariableRequest):
        return []
    elif isinstance(request, ListRequest):
        return []
    elif isinstance(request, IndexTxtRequest):
        return request.input_files
    else:
        assert False


def get_output_files(request):
    if isinstance(request, SingleExecutionRequest):
        return request.output_files
    elif isinstance(request, RepeatedExecutionRequest):
        return request.output_files
    elif isinstance(request, RepeatedOrSingleExecutionRequest):
        return request.output_files
    elif isinstance(request, PrintFileRequest):
        return [request.output_file]
    elif isinstance(request, CopyRequest):
        return [request.output_file]
    elif isinstance(request, VariableRequest):
        return []
    elif isinstance(request, ListRequest):
        return [request.output_file]
    elif isinstance(request, IndexTxtRequest):
        return [request.output_file]
    else:
        assert False


def get_category(request):
    if isinstance(request, SingleExecutionRequest):
        return request.category
    elif isinstance(request, RepeatedExecutionRequest):
        return request.category
    elif isinstance(request, RepeatedOrSingleExecutionRequest):
        return request.category
    elif isinstance(request, IndexTxtRequest):
        return request.category
    else:
        return None


def get_all_output_files(requests, include_tmp=False):
    files = []
    for request in requests:
        files += get_output_files(request)

    # Filter out all files but those in OUT_DIR if necessary.
    # It is also easy to filter for uniqueness; do it right now and return.
    if not include_tmp:
        files = (file for file in files if isinstance(file, OutFile))
        return list(set(files))

    # Filter for unique values.  NOTE: Cannot use set() because we need to accept same filename as
    # OutFile and TmpFile as different, and by default they evaluate as equal.
    return [f for _, f in set((type(f), f) for f in files)]


class SpaceSeparatedList(list):
    """A list that joins itself with spaces when converted to a string."""
    def __str__(self):
        return " ".join(self)
