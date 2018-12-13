# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Python 2/3 Compatibility (ICU-20299)
# TODO(ICU-20301): Remove this.
from __future__ import print_function

from abc import abstractmethod
import copy
import sys


from . import utils


# TODO(ICU-20301): Remove arguments from all instances of super() in this file

# Note: for this to be a proper abstract class, it should extend abc.ABC.
# There is no nice way to do this that works in both Python 2 and 3.
# TODO(ICU-20301): Make this inherit from abc.ABC.
class AbstractRequest(object):
    def __init__(self, **kwargs):

        # Used for identification purposes
        self.name = None

        # The filter category that applies to this request
        self.category = None

        self._set_fields(kwargs)

    def _set_fields(self, kwargs):
        for key, value in list(kwargs.items()):
            if hasattr(self, key):
                if isinstance(value, list):
                    value = copy.copy(value)
                elif isinstance(value, dict):
                    value = copy.deepcopy(value)
                setattr(self, key, value)
            else:
                raise ValueError("Unknown argument: %s" % key)

    def apply_file_filter(self, filter):
        return True

    def flatten(self, config, all_requests, common_vars):
        return [self]

    def all_input_files(self):
        return []

    def all_output_files(self):
        return []


class AbstractExecutionRequest(AbstractRequest):
    def __init__(self, **kwargs):

        # Names of targets (requests) or files that this request depends on;
        # targets are of type DepTarget
        self.dep_targets = []

        self.dep_files = []

        # Primary input files
        self.input_files = []

        # Output files; for some subclasses, this must be the same length
        # as input_files
        self.output_files = []

        # What tool to execute
        self.tool = None

        # Argument string to pass to the tool with optional placeholders
        self.args = ""

        # Placeholders to substitute into the argument string; if any of these
        # have a list type, the list must be equal in length to input_files
        self.format_with = {}

        super(AbstractExecutionRequest, self).__init__(**kwargs)

    def apply_file_filter(self, filter):
        i = 0
        while i < len(self.input_files):
            if filter.match(self.input_files[i]):
                i += 1
                continue
            self._del_at(i)
        return i > 0

    def _del_at(self, i):
        del self.input_files[i]
        for _, v in self.format_with.items():
            if isinstance(v, list):
                del v[i]

    def flatten(self, config, all_requests, common_vars):
        self._dep_targets_to_files(all_requests)
        return super(AbstractExecutionRequest, self).flatten(config, all_requests, common_vars)

    def _dep_targets_to_files(self, all_requests):
        if not self.dep_targets:
            return
        for dep_target in self.dep_targets:
            for request in all_requests:
                if request.name == dep_target.name:
                    self.dep_files += request.all_output_files()
                    break
            else:
                print("Warning: Unable to find target %s, a dependency of %s" % (
                    dep_target.name,
                    self.name
                ), file=sys.stderr)

    def all_input_files(self):
        return self.dep_files + self.input_files

    def all_output_files(self):
        return self.output_files


class SingleExecutionRequest(AbstractExecutionRequest):
    def __init__(self, **kwargs):
        super(SingleExecutionRequest, self).__init__(**kwargs)


class RepeatedExecutionRequest(AbstractExecutionRequest):
    def __init__(self, **kwargs):

        # Placeholders to substitute into the argument string unique to each
        # iteration; all values must be lists equal in length to input_files
        self.repeat_with = {}

        super(RepeatedExecutionRequest, self).__init__(**kwargs)

    def _del_at(self, i):
        super(RepeatedExecutionRequest, self)._del_at(i)
        del self.output_files[i]
        for _, v in self.repeat_with.items():
            if isinstance(v, list):
                del v[i]


class RepeatedOrSingleExecutionRequest(AbstractExecutionRequest):
    def __init__(self, **kwargs):
        self.repeat_with = {}
        super(RepeatedOrSingleExecutionRequest, self).__init__(**kwargs)

    def flatten(self, config, all_requests, common_vars):
        if config.max_parallel:
            new_request = RepeatedExecutionRequest(
                name = self.name,
                category = self.category,
                dep_targets = self.dep_targets,
                input_files = self.input_files,
                output_files = self.output_files,
                tool = self.tool,
                args = self.args,
                format_with = self.format_with,
                repeat_with = self.repeat_with
            )
        else:
            new_request = SingleExecutionRequest(
                name = self.name,
                category = self.category,
                dep_targets = self.dep_targets,
                input_files = self.input_files,
                output_files = self.output_files,
                tool = self.tool,
                args = self.args,
                format_with = utils.concat_dicts(self.format_with, self.repeat_with)
            )
        return new_request.flatten(config, all_requests, common_vars)

    def _del_at(self, i):
        super(RepeatedOrSingleExecutionRequest, self)._del_at(i)
        del self.output_files[i]
        for _, v in self.repeat_with.items():
            if isinstance(v, list):
                del v[i]


class PrintFileRequest(AbstractRequest):
    def __init__(self, **kwargs):
        self.output_file = None
        self.content = None
        super(PrintFileRequest, self).__init__(**kwargs)

    def all_output_files(self):
        return [self.output_file]


class CopyRequest(AbstractRequest):
    def __init__(self, **kwargs):
        self.input_file = None
        self.output_file = None
        super(CopyRequest, self).__init__(**kwargs)

    def all_input_files(self):
        return [self.input_file]

    def all_output_files(self):
        return [self.output_file]


class VariableRequest(AbstractRequest):
    def __init__(self, **kwargs):
        self.input_files = []
        super(VariableRequest, self).__init__(**kwargs)

    def all_input_files(self):
        return self.input_files


class ListRequest(AbstractRequest):
    def __init__(self, **kwargs):
        self.variable_name = None
        self.output_file = None
        self.include_tmp = None
        super(ListRequest, self).__init__(**kwargs)

    def flatten(self, config, all_requests, common_vars):
        list_files = list(sorted(utils.get_all_output_files(all_requests)))
        if self.include_tmp:
            variable_files = list(sorted(utils.get_all_output_files(all_requests, include_tmp=True)))
        else:
            # Always include the list file itself
            variable_files = list_files + [self.output_file]
        return PrintFileRequest(
            name = self.name,
            output_file = self.output_file,
            content = "\n".join(file.filename for file in list_files)
        ).flatten(config, all_requests, common_vars) + VariableRequest(
            name = self.variable_name,
            input_files = variable_files
        ).flatten(config, all_requests, common_vars)

    def all_output_files(self):
        return [self.output_file]


class IndexTxtRequest(AbstractRequest):
    def __init__(self, **kwargs):
        self.input_files = []
        self.output_file = None
        self.cldr_version = ""
        super(IndexTxtRequest, self).__init__(**kwargs)

    def apply_file_filter(self, filter):
        i = 0
        while i < len(self.input_files):
            if filter.match(self.input_files[i]):
                i += 1
                continue
            del self.input_files[i]
        return i > 0

    def flatten(self, config, all_requests, common_vars):
        return PrintFileRequest(
            name = self.name,
            output_file = self.output_file,
            content = self._generate_index_file(common_vars)
        ).flatten(config, all_requests, common_vars)

    def _generate_index_file(self, common_vars):
        locales = [f.filename[f.filename.rfind("/")+1:-4] for f in self.input_files]
        formatted_version = "    CLDRVersion { \"%s\" }\n" % self.cldr_version if self.cldr_version else ""
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

    def all_input_files(self):
        return self.input_files

    def all_output_files(self):
        return [self.output_file]
