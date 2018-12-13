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
from .request_types import *


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

    def filter(self, request):
        if not request.apply_file_filter(self):
            return []
        for file in request.all_input_files():
            assert self.match(file)
        return [request]

    @abstractmethod
    def match(self, file):
        pass


class ExclusionFilter(Filter):
    def match(self, file):
        return False


class WhitelistBlacklistFilter(Filter):
    def __init__(self, json_data):
        if "whitelist" in json_data:
            self.is_whitelist = True
            self.whitelist = json_data["whitelist"]
        elif "blacklist" in json_data:
            self.is_whitelist = False
            self.blacklist = json_data["blacklist"]

    def match(self, file):
        file_stem = self._file_to_file_stem(file)
        return self._should_include(file_stem)

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


def apply_filters(requests, config):
    """Runs the filters and returns a new list of requests."""
    requests = _apply_file_filters(requests, config)
    requests = _apply_resource_filters(requests, config)
    return requests


def _apply_file_filters(old_requests, config):
    """Filters out entire files."""
    filters = _preprocess_file_filters(old_requests, config)
    new_requests = []
    for request in old_requests:
        category = request.category
        if category in filters:
            new_requests += filters[category].filter(request)
        else:
            new_requests.append(request)
    return new_requests


def _preprocess_file_filters(requests, config):
    all_categories = set(
        request.category
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


def _apply_resource_filters(old_requests, config):
    """Creates filters for looking within resource bundle files."""
    return old_requests
