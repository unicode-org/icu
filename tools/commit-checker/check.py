#!/usr/bin/env python3
# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html 
# Author: shane@unicode.org

import argparse
import itertools
import os
import re
import sys

from enum import Enum
from collections import namedtuple
from git import Repo
from jira import JIRA


ICUCommit = namedtuple("ICUCommit", ["issue_id", "commit"])

class CommitWanted(Enum):
    REQUIRED = 1
    OPTIONAL = 2
    FORBIDDEN = 3
    ERROR = 4

ICUIssue = namedtuple("ICUIssue", ["issue_id", "is_closed", "commit_wanted", "issue"])


flag_parser = argparse.ArgumentParser(
    description = "Generates a Markdown report for commits on master since the 'latest' tag.",
    formatter_class = argparse.ArgumentDefaultsHelpFormatter
)
flag_parser.add_argument(
    "--rev-range",
    help = "A git revision range; see https://git-scm.com/docs/gitrevisions. Should be the two-dot range between the previous release and the current tip.",
    required = True
)
flag_parser.add_argument(
    "--repo-root",
    help = "Path to the repository to check",
    default = os.path.join(os.path.dirname(__file__), "..", "..")
)
flag_parser.add_argument(
    "--jira-hostname",
    help = "Hostname of the Jira instance",
    default = "unicode-org.atlassian.net"
)
flag_parser.add_argument(
    "--jira-username",
    help = "Username to use for authenticating to Jira",
    default = os.environ.get("JIRA_USERNAME", None)
)
flag_parser.add_argument(
    "--jira-password",
    help = "Password to use for authenticating to Jira. Authentication is necessary to process sensitive tickets. Leave empty to skip authentication. Instead of passing your password on the command line, you can save your password in the JIRA_PASSWORD environment variable. You can also create a file in this directory named \".env\" with the contents \"JIRA_PASSWORD=xxxxx\".",
    default = os.environ.get("JIRA_PASSWORD", None)
)
flag_parser.add_argument(
    "--jira-query",
    help = "JQL query load tickets; this should match tickets expected to correspond to the commits being checked. Example: 'project=ICU and fixVersion=63.1'; set fixVersion to the upcoming version.",
    required = True
)
flag_parser.add_argument(
    "--github-url",
    help = "Base URL of the GitHub repo",
    default = "https://github.com/unicode-org/icu"
)


def issue_id_to_url(issue_id, jira_hostname, **kwargs):
    return "https://%s/browse/%s" % (jira_hostname, issue_id)


def pretty_print_commit(commit, github_url, **kwargs):
    print("- %s `%s`" % (commit.commit.hexsha[:7], commit.commit.summary))
    print("\t- Authored by %s <%s>" % (commit.commit.author.name, commit.commit.author.email))
    print("\t- Committed at %s" % commit.commit.committed_datetime.isoformat())
    print("\t- GitHub Link: %s" % "%s/commit/%s" % (github_url, commit.commit.hexsha))


def pretty_print_issue(issue, **kwargs):
    print("- %s: `%s`" % (issue.issue_id, issue.issue.fields.summary))
    if issue.issue.fields.assignee:
        print("\t- Assigned to %s" % issue.issue.fields.assignee.displayName)
    else:
        print("\t- No assignee!")
    print("\t- Jira Link: %s" % issue_id_to_url(issue.issue_id, **kwargs))


def get_commits(repo_root, rev_range, **kwargs):
    """
    Yields an ICUCommit for each commit in the user-specified rev-range.
    """
    repo = Repo(repo_root)
    for commit in repo.iter_commits(rev_range):
        match = re.search(r"^(\w+-\d+) ", commit.message)
        if match:
            yield ICUCommit(match.group(1), commit)
        else:
            yield ICUCommit(None, commit)


def get_jira_instance(jira_hostname, jira_username, jira_password, **kwargs):
    jira_url = "https://%s" % jira_hostname
    if jira_username and jira_password:
        jira = JIRA(jira_url, basic_auth=(jira_username, jira_password))
    else:
        jira = JIRA(jira_url)
    return (jira_url, jira)


def make_icu_issue(jira_issue):
    # Resolution ID 10004 is "Fixed"
    # Resolution ID 10015 is "Fixed by Other Ticket"
    if not jira_issue.fields.resolution:
        commit_wanted = CommitWanted["OPTIONAL"]
    elif jira_issue.fields.resolution.id == "10015":
        commit_wanted = CommitWanted["FORBIDDEN"]
    elif jira_issue.fields.resolution.id != "10004":
        commit_wanted = CommitWanted["ERROR"]
    # Issue Type ID 10010 is User Guide
    # Issue Type ID 10003 is Task
    elif jira_issue.fields.issuetype.id == "10010" or jira_issue.fields.issuetype.id == "10003":
        commit_wanted = CommitWanted["OPTIONAL"]
    else:
        commit_wanted = CommitWanted["REQUIRED"]
    # Status ID 10002 is "Done"
    return ICUIssue(jira_issue.key, jira_issue.fields.status.id == "10002", commit_wanted, jira_issue)


def get_jira_issues(jira_query, **kwargs):
    """
    Yields an ICUIssue for each issue in the user-specified query.
    """
    jira_url, jira = get_jira_instance(**kwargs)
    # Jira limits us to query the API using a limited batch size.
    start = 0
    batch_size = 50
    while True:
        issues = jira.search_issues(jira_query, startAt=start, maxResults=batch_size)
        print("Loaded issues %d-%d" % (start, start + len(issues)), file=sys.stderr)
        for jira_issue in issues:
            yield make_icu_issue(jira_issue)
        if len(issues) < batch_size:
            break
        start += batch_size


def get_single_jira_issue(issue_id, **kwargs):
    """
    Returns a single ICUIssue for the given issue ID.
    """
    jira_url, jira = get_jira_instance(**kwargs)
    jira_issue = jira.issue(issue_id)
    print("Loaded single issue %s" % issue_id, file=sys.stderr)
    if jira_issue:
        return make_icu_issue(jira_issue)
    else:
        return None


def main():
    args = flag_parser.parse_args()
    print("TIP: Have you pulled the latest master? This script only looks at local commits.", file=sys.stderr)
    if not args.jira_username or not args.jira_password:
        print("WARNING: Jira credentials not supplied. Sensitive tickets will not be found.", file=sys.stderr)
        authenticated = False
    else:
        authenticated = True

    commits = list(get_commits(**vars(args)))
    issues = list(get_jira_issues(**vars(args)))

    commit_issue_ids = set(commit.issue_id for commit in commits if commit.issue_id is not None)
    grouped_commits = [
        (issue_id, [commit for commit in commits if commit.issue_id == issue_id])
        for issue_id in sorted(commit_issue_ids)
    ]
    jira_issue_map = {issue.issue_id: issue for issue in issues}
    jira_issue_ids = set(issue.issue_id for issue in issues)
    closed_jira_issue_ids = set(issue.issue_id for issue in issues if issue.is_closed)

    total_problems = 0
    print("<!---")
    print("Copyright (C) 2018 and later: Unicode, Inc. and others.")
    print("License & terms of use: http://www.unicode.org/copyright.html")
    print("-->")
    print()
    print("Commit Report")
    print("=============")
    print()
    print("Environment:")
    print("- Latest Commit: %s" % commits[0].commit.hexsha)
    print("- Jira Query: %s" % args.jira_query)
    print("- Rev Range: %s" % args.rev_range)
    print("- Authenticated: %s" % "Yes" if authenticated else "No (sensitive tickets not shown)")
    print()
    print("## Problem Categories")
    print("### Closed Issues with No Commit")
    print("Tip: Tickets with type 'Task' or 'User Guide' or resolution 'Fixed by Other Ticket' are ignored.")
    print()
    found = False
    for issue in issues:
        if not issue.is_closed:
            continue
        if issue.issue_id in commit_issue_ids:
            continue
        if issue.commit_wanted == CommitWanted["OPTIONAL"] or issue.commit_wanted == CommitWanted["FORBIDDEN"]:
            continue
        found = True
        total_problems += 1
        pretty_print_issue(issue, **vars(args))
        print()
    if not found:
        print("*Success: No problems in this category!*")

    print("### Closed Issues with Illegal Resolution or Commit")
    print("Tip: Fixed tickets should have resolution 'Fixed by Other Ticket' or 'Fixed'.")
    print("Duplicate tickets should have their fixVersion tag removed.")
    print("Tickets with resolution 'Fixed by Other Ticket' are not allowed to have commits.")
    print()
    found = False
    for issue in issues:
        if not issue.is_closed:
            continue
        if issue.commit_wanted == CommitWanted["OPTIONAL"]:
            continue
        if issue.issue_id in commit_issue_ids and issue.commit_wanted == CommitWanted["REQUIRED"]:
            continue
        if issue.issue_id not in commit_issue_ids and issue.commit_wanted == CommitWanted["FORBIDDEN"]:
            continue
        found = True
        total_problems += 1
        pretty_print_issue(issue, **vars(args))
        print()
    if not found:
        print("*Success: No problems in this category!*")

    print()
    print("### Commits without Jira Issue Tag")
    print("Tip: If you see your name here, make sure to label your commits correctly in the future.")
    print()
    found = False
    for commit in commits:
        if commit.issue_id is not None:
            continue
        found = True
        total_problems += 1
        pretty_print_commit(commit, **vars(args))
        print()
    if not found:
        print("*Success: No problems in this category!*")

    print()
    print("### Commits with Jira Issue Not Found")
    print("Tip: Check that these tickets have the correct fixVersion tag.")
    print()
    found = False
    for issue_id, commits in grouped_commits:
        if issue_id in jira_issue_ids:
            continue
        found = True
        total_problems += 1
        print("#### Issue %s" % issue_id)
        print()
        jira_issue = get_single_jira_issue(issue_id, **vars(args))
        if jira_issue:
            pretty_print_issue(jira_issue, **vars(args))
        else:
            print("*Jira issue does not seem to exist*")
        print()
        print("##### Commits with Issue %s" % issue_id)
        print()
        for commit in commits:
            pretty_print_commit(commit, **vars(args))
            print()
    if not found:
        print("*Success: No problems in this category!*")

    print()
    print("### Commits with Open Jira Issue")
    print("Tip: Consider closing the ticket if it is fixed.")
    print()
    found = False
    for issue_id, commits in grouped_commits:
        if issue_id in closed_jira_issue_ids:
            continue
        print("#### Issue %s" % issue_id)
        print()
        if issue_id in jira_issue_map:
            jira_issue = jira_issue_map[issue_id]
        else:
            jira_issue = get_single_jira_issue(issue_id, **vars(args))
        if jira_issue:
            pretty_print_issue(jira_issue, **vars(args))
        else:
            print("*Jira issue does not seem to exist*")
        print()
        print("##### Commits with Issue %s" % issue_id)
        print()
        found = True
        total_problems += 1
        for commit in commits:
            pretty_print_commit(commit, **vars(args))
            print()
    if not found:
        print("*Success: No problems in this category!*")

    print()
    print("## Total Problems: %s" % total_problems)


if __name__ == "__main__":
    main()
