# Copyright (C) 2022 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Author: srloomis@unicode.org

from re import compile
from typing import Tuple
import typing

commit_line = compile('^\s*-\s+([0-9a-f]+)\s+([^\s]+)\s+(.*)$') # '- <sha> <hypen-or-ticketid> <message>'
verb_line = compile('^\s*#\s+([A-Z]+)\s+(.*)$') # '# ACTION any other content…'

class CommitMetadata():
    def __init__(self, metadata_file=None) -> None:
        skipset = {}
        fixset = []  # sha, id, message
        current_skip = None
        if metadata_file:
            with open(metadata_file, 'r') as f:
                for line in f.readlines():
                    line = line.strip()
                    m = commit_line.match(line)
                    if m:
                        sha = m.group(1)
                        id = m.group(2)
                        message = m.group(3)
                        fixset.append((sha, id, message))
                        if current_skip:
                            skipset[current_skip].append((sha, id, message))
                        continue
                    m = verb_line.match(line)
                    if m:
                        action = m.group(1)
                        content = m.group(2)
                        # for now we only support SKIP
                        assert action == "SKIP", "Unknown action %s in %s (expected 'SKIP')" % (action, line)
                        current_skip = content
                        assert content not in skipset, "Error: two sections like %s" % line
                        skipset[content] = []  # initialize this
                        continue
        self.fixset = fixset
        self.skipset = skipset
        pass

    def get_commit_info(self, commit, skip=None) -> typing.Tuple[str, str, str]:
        """Return an override line given a commit

        Args:
            commit (str): hash or short hash of a commit
            skip (str): If set, a version to skip such as 'v41' (this will match a section - SKIP v41)

        Returns:
            sha (str): original sha from line
            id (str): ticket id or hash
            message (str): override message
        """
        if skip:
            if not skip in self.skipset:
                return None
            return CommitMetadata.match_list(commit, self.skipset[skip])
        else:
            return CommitMetadata.match_list(commit, self.fixset)

    @staticmethod
    def match_list(commit, l) -> any:
        """Find the first match in a list of commits

        Args:
            commit (str): short or long commit string
            l (list): list of tuples, where item 0 is the commit

        Returns:
            any: matching list member, or None
        """
        for i in l:
            if CommitMetadata.match_commit(commit, i[0]):
                return i
        return None

    @staticmethod
    def match_commit(h1, h2) -> bool:
        """return true if the prefix of the hashes are the same"""
        comm = min(len(h1), len(h2))
        return h1[0:comm] == h2[0:comm]
