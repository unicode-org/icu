# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Python 2/3 Compatibility (ICU-20299)
# TODO(ICU-20301): Remove this.
from __future__ import print_function

from abc import abstractmethod
from collections import defaultdict
import re
import sys

from . import *
from . import utils


# Note: for this to be a proper abstract class, it should extend abc.ABC.
# There is no nice way to do this that works in both Python 2 and 3.
# TODO(ICU-20301): Make this inherit from abc.ABC.
class Filter(object):
    @staticmethod
    def create_from_json(json_data):
        if "filterType" in json_data:
            filter_type = json_data["filterType"]
        else:
            filter_type = "file-stem"

        if filter_type == "file-stem":
            return FileStemFilter(json_data)
        elif filter_type == "language":
            return LanguageFilter(json_data)
        elif filter_type == "regex":
            return RegexFilter(json_data)
        elif filter_type == "exclude":
            return ExclusionFilter()
        else:
            print("Error: Unknown filterType option: %s" % filter_type, file=sys.stderr)
            return None

    @abstractmethod
    def filter(self, request):
        pass


class ExclusionFilter(Filter):
    def filter(self, request):
        return []


class WhitelistBlacklistFilter(Filter):
    def __init__(self, json_data):
        if "whitelist" in json_data:
            self.is_whitelist = True
            self.whitelist = json_data["whitelist"]
        elif "blacklist" in json_data:
            self.is_whitelist = False
            self.blacklist = json_data["blacklist"]

    def filter(self, request):
        if isinstance(request, SingleExecutionRequest):
            return self._filter_single(request)
        elif isinstance(request, RepeatedExecutionRequest):
            return self._filter_repeated(request)
        elif isinstance(request, RepeatedOrSingleExecutionRequest):
            return self._filter_repeated_or_single(request)
        elif isinstance(request, IndexTxtRequest):
            return self._filter_index_txt(request)
        else:
            # Assert that no other types are needed
            for file in utils.get_input_files(request):
                file_stem = self._file_to_file_stem(file)
                assert self._should_include(file_stem), request
            return [request]

    def _filter_single(self, request):
        new_input_files = []
        new_format_with = defaultdict(utils.SpaceSeparatedList)
        for i in range(len(request.input_files)):
            file_stem = self._file_to_file_stem(request.input_files[i])
            if self._should_include(file_stem):
                new_input_files.append(request.input_files[i])
                for k,v in request.format_with.items():
                    if isinstance(v, list):
                        new_format_with[k].append(v[i])

        # Return a new request if there are still >= 1 input files.
        if new_input_files:
            return [
                SingleExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = request.dep_files,
                    input_files = new_input_files,
                    output_files = request.output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = utils.concat_dicts(request.format_with, new_format_with)
                )
            ]
        return []

    def _filter_repeated(self, request):
        new_input_files = []
        new_output_files = []
        new_format_with = defaultdict(utils.SpaceSeparatedList)
        new_repeat_with = defaultdict(utils.SpaceSeparatedList)
        for i in range(len(request.input_files)):
            file_stem = self._file_to_file_stem(request.input_files[i])
            if self._should_include(file_stem):
                new_input_files.append(request.input_files[i])
                new_output_files.append(request.output_files[i])
                for k,v in request.format_with.items():
                    if isinstance(v, list):
                        new_format_with[k].append(v[i])
                for k,v in request.repeat_with.items():
                    assert isinstance(v, list)
                    new_repeat_with[k].append(v[i])

        # Return a new request if there are still >= 1 input files.
        if new_input_files:
            return [
                RepeatedExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = request.dep_files,
                    input_files = new_input_files,
                    output_files = new_output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = utils.concat_dicts(request.format_with, new_format_with),
                    repeat_with = utils.concat_dicts(request.repeat_with, new_repeat_with)
                )
            ]
        else:
            return []

    def _filter_repeated_or_single(self, request):
        new_input_files = []
        new_output_files = []
        new_format_with = defaultdict(utils.SpaceSeparatedList)
        new_repeat_with = defaultdict(utils.SpaceSeparatedList)
        for i in range(len(request.input_files)):
            file_stem = self._file_to_file_stem(request.input_files[i])
            if self._should_include(file_stem):
                new_input_files.append(request.input_files[i])
                new_output_files.append(request.output_files[i])
                for k,v in request.format_with.items():
                    if isinstance(v, list):
                        new_format_with[k].append(v[i])
                for k,v in request.repeat_with.items():
                    assert isinstance(v, list)
                    new_repeat_with[k].append(v[i])

        # Return a new request if there are still >= 1 input files.
        if new_input_files:
            return [
                RepeatedOrSingleExecutionRequest(
                    name = request.name,
                    category = request.category,
                    dep_files = request.dep_files,
                    input_files = new_input_files,
                    output_files = new_output_files,
                    tool = request.tool,
                    args = request.args,
                    format_with = utils.concat_dicts(request.format_with, new_format_with),
                    repeat_with = utils.concat_dicts(request.repeat_with, new_repeat_with)
                )
            ]
        else:
            return []

    def _filter_index_txt(self, request):
        new_input_files = []
        for file in request.input_files:
            file_stem = self._file_to_file_stem(file)
            if self._should_include(file_stem):
                new_input_files.append(file)

        # Return a new request if there are still >= 1 input files.
        if new_input_files:
            return [
                IndexTxtRequest(
                    name = request.name,
                    category = request.category,
                    input_files = new_input_files,
                    output_file = request.output_file,
                    cldr_version = request.cldr_version
                )
            ]
        else:
            return []

    @classmethod
    def _file_to_file_stem(cls, file):
        start = file.filename.rfind("/")
        limit = file.filename.rfind(".")
        return file.filename[start+1:limit]

    @abstractmethod
    def _should_include(self, file_stem):
        pass


class FileStemFilter(WhitelistBlacklistFilter):
    def _should_include(self, file_stem):
        if self.is_whitelist:
            return file_stem in self.whitelist
        else:
            return file_stem not in self.blacklist


class LanguageFilter(WhitelistBlacklistFilter):
    def _should_include(self, file_stem):
        language = file_stem.split("_")[0]
        if language == "root":
            # Always include root.txt
            return True
        if self.is_whitelist:
            return language in self.whitelist
        else:
            return language not in self.blacklist


class RegexFilter(WhitelistBlacklistFilter):
    def __init__(self, *args):
        # TODO(ICU-20301): Change this to: super().__init__(*args)
        super(RegexFilter, self).__init__(*args)
        if self.is_whitelist:
            self.whitelist = [re.compile(pat) for pat in self.whitelist]
        else:
            self.blacklist = [re.compile(pat) for pat in self.blacklist]

    def _should_include(self, file_stem):
        if self.is_whitelist:
            for pattern in self.whitelist:
                if pattern.match(file_stem):
                    return True
            return False
        else:
            for pattern in self.blacklist:
                if pattern.match(file_stem):
                    return False
            return True


def apply_filters(old_requests, config):
    """Runs the filters and returns a new list of requests."""
    filters = _preprocess_filters(old_requests, config)
    new_requests = []
    for request in old_requests:
        category = utils.get_category(request)
        if category in filters:
            new_requests += filters[category].filter(request)
        else:
            new_requests.append(request)
    return new_requests


def _preprocess_filters(requests, config):
    all_categories = set(
        utils.get_category(request)
        for request in requests
    )
    all_categories.remove(None)
    all_categories = list(sorted(all_categories))
    json_data = config.filters_json_data
    filters = {}
    for category in all_categories:
        if "featureFilters" in json_data and category in json_data["featureFilters"]:
            filters[category] = Filter.create_from_json(
                json_data["featureFilters"][category]
            )
        elif "localeFilter" in json_data and category[-5:] == "_tree":
            filters[category] = Filter.create_from_json(
                json_data["localeFilter"]
            )
    if "featureFilters" in json_data:
        for category in json_data["featureFilters"]:
            if category not in all_categories:
                print("Warning: category %s is not known" % category, file=sys.stderr)
    return filters
