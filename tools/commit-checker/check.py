#!/usr/bin/env python3
# Copyright (C) 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Author: shane@unicode.org

import argparse
import itertools
import os
import io
import re
import sys
import datetime
from hashlib import sha256
import pickle

from enum import Enum
from collections import namedtuple
from git import Repo
from jira import JIRA
from commit_metadata import CommitMetadata

# singleCount = 0

ICUCommit = namedtuple("ICUCommit", ["issue_id", "commit"])

class CommitWanted(Enum):
    REQUIRED = 1
    OPTIONAL = 2
    FORBIDDEN = 3
    ERROR = 4

ICUIssue = namedtuple("ICUIssue", ["issue_id", "is_closed", "commit_wanted", "issue"])

# JIRA constants.

# TODO: clearly these should move into a config file of some sort.
# NB: you can fetch the resolution IDs by authenticating to JIRA and then viewing
# the URL given.

# constants for jira_issue.fields.resolution.id
# <https://unicode-org.atlassian.net/rest/api/2/resolution>
R_NEEDS_MOREINFO        = "10003"
R_FIXED                 = "10004"
R_NO_TIME_TO_DO_THIS    = "10005"
R_DUPLICATE             = "10006"
R_OUTOFSCOPE            = "10008"
R_ASDESIGNED            = "10009"
R_WONTFIX               = "10010"  # deprecated
R_INVALID               = "10012"
R_FIXED_BY_OTHER_TICKET = "10015"
R_NOTREPRO              = "10024"
R_FIXED_NON_REPO        = "10025"
R_FIX_SURVEY_TOOL       = "10022"
R_OBSOLETE              = "10023"

# so we don't miss any
all_resolutions = [ R_NEEDS_MOREINFO, R_FIXED, R_NO_TIME_TO_DO_THIS, R_DUPLICATE, R_OUTOFSCOPE, R_ASDESIGNED, R_WONTFIX, R_FIXED_BY_OTHER_TICKET, R_NOTREPRO, R_FIX_SURVEY_TOOL, R_OBSOLETE, R_FIXED_NON_REPO, R_INVALID ]

# constants for jira_issue.fields.issuetype.id
# <https://unicode-org.atlassian.net/rest/api/2/issuetype>
I_ICU_USERGUIDE         = "10010"
I_TASK                  = "10003"

# constants for jira_issue.fields.status.id
# <https://unicode-org.atlassian.net/rest/api/2/status>
S_REVIEWING             = "10001"
S_DONE                  = "10002"
S_REVIEW_FEEDBACK       = "10003"

def jira_issue_under_review(jira_issue):
    """
    Yields True if ticket is considered "under review"
    """
    # TODO: should be data driven from a config file.
    if jira_issue.issue.fields.status.id in [S_REVIEWING, S_REVIEW_FEEDBACK]:
        return True
    else:
        return False

def make_commit_wanted(jira_issue):
    """Yields a CommitWanted enum with the policy decision for this particular issue"""
    # TODO: should be data driven from a config file.
    if not jira_issue.fields.resolution:
        commit_wanted = CommitWanted["OPTIONAL"]
    elif jira_issue.fields.resolution.id in [ R_DUPLICATE, R_ASDESIGNED, R_OUTOFSCOPE, R_NOTREPRO, R_INVALID, R_NEEDS_MOREINFO, R_OBSOLETE, R_NO_TIME_TO_DO_THIS ]:
        commit_wanted = CommitWanted["FORBIDDEN"]
    elif jira_issue.fields.resolution.id in [ R_FIXED_NON_REPO, R_FIX_SURVEY_TOOL, R_FIXED_BY_OTHER_TICKET ]:
        commit_wanted = CommitWanted["FORBIDDEN"]
    elif jira_issue.fields.issuetype.id in [ I_ICU_USERGUIDE, I_TASK ]:
        commit_wanted = CommitWanted["OPTIONAL"]
    elif jira_issue.fields.resolution.id in [ R_FIXED ]:
        commit_wanted = CommitWanted["REQUIRED"]
    elif jira_issue.fields.resolution.id == R_FIXED_BY_OTHER_TICKET:
        commit_wanted = CommitWanted["FORBIDDEN"]
    elif jira_issue.fields.resolution.id != R_FIXED:
        commit_wanted = CommitWanted["ERROR"]
    else:
        commit_wanted = CommitWanted["REQUIRED"]
    return commit_wanted


flag_parser = argparse.ArgumentParser(
    description = "Generates a Markdown report for commits on main since the 'latest' tag.",
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
    "--commit-metadata",
    help = "path to COMMIT_METADATA.md",
    default = "COMMIT_METADATA.md"
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
    "--fix-version",
    help = "Assumed Fix version. Used for commit metadata",
    required = False
)
flag_parser.add_argument(
    "--cache-for-dev",
    help = "DEV USE ONLY: prefix of path to a jira cache",
    required = False
)
flag_parser.add_argument(
    "--github-url",
    help = "Base URL of the GitHub repo",
    default = "https://github.com/unicode-org/icu"
)
flag_parser.add_argument(
    "--nocopyright",
    help = "Omit ICU copyright",
    action = "store_true"
)


def issue_id_to_url(issue_id, jira_hostname, **kwargs):
    return "https://%s/browse/%s" % (jira_hostname, issue_id)


def pretty_print_commit(commit, github_url, file=None, **kwargs):
    fixed_summary = commit.commit.summary
    if commit.issue_id and fixed_summary[:(len(commit.issue_id)+1)] == commit.issue_id+' ':
        # linkify beginning of commit message
        fixed_summary = fixed_summary[len(commit.issue_id)+1:]
        fixed_summary = '[%s](%s) `%s`' % (commit.issue_id, issue_id_to_url(commit.issue_id, **kwargs), fixed_summary)
    else:
        fixed_summary = '`%s`' % fixed_summary
    print("- [%s](%s) %s" % (commit.commit.hexsha[:7], ("%s/commit/%s" % (github_url, commit.commit.hexsha)), fixed_summary), file=file)
    print("\t- Authored by %s <%s>" % (commit.commit.author.name, commit.commit.author.email), file=file)
    print("\t- Committed at %s" % commit.commit.committed_datetime.isoformat(), file=file)


def pretty_print_issue(issue, type=None, file=None, **kwargs):
    print("- [%s](%s): `%s`" % (issue.issue_id, issue_id_to_url(issue.issue_id, **kwargs), issue.issue.fields.summary), file=file)
    if type:
        print("\t- _%s_" % type, file=file)
    if issue.issue.fields.assignee:
        print("\t- Assigned to %s" % issue.issue.fields.assignee.displayName, file=file)
    else:
        print("\t- No assignee!", file=file)
    # If actually under review, print reviewer
    if jira_issue_under_review(issue) and issue.issue.fields.customfield_10031:
        print("\t- Reviewer: %s" % issue.issue.fields.customfield_10031.displayName, file=file)
    print("\t- Status: %s" % issue.issue.fields.status.name, file=file)
    if(issue.issue.fields.resolution):
        print("\t- Resolution: " + issue.issue.fields.resolution.name, file=file)
    if(issue.issue.fields.fixVersions):
        for version in issue.issue.fields.fixVersions:
            print("\t- Fix Version: " + version.name, file=file)
    else:
        print("\t- Fix Version: _none_", file=file)
    if issue.issue.fields.components and len(issue.issue.fields.components) > 0:
        print("\t- Component(s): " + (' '.join(sorted([str(component.name) for component in issue.issue.fields.components]))), file=file)

def get_commits(commit_metadata_obj, repo_root, rev_range, **kwargs):
    """
    Yields an ICUCommit for each commit in the user-specified rev-range.
    """
    repo = Repo(repo_root)
    for commit in repo.iter_commits(rev_range):
        issue_id = None
        if commit_metadata_obj:
            # read from the non-skip list
            from_commit_metadata = commit_metadata_obj.get_commit_info(commit.hexsha, skip=None)
            if from_commit_metadata:
                (sha, issue_id, message) = from_commit_metadata
                if message:
                    # Update message if found
                    commit.message = str(commit.message) + "\nCOMMIT_METADATA: " + message
        if not issue_id:
            match = re.search(r"^(\w+-\d+) ", commit.message)
            if match:
                issue_id = match.group(1)
        yield ICUCommit(issue_id, commit)

def get_cherrypicked_commits(repo_root, rev_range, **kwargs):
    """
    Yields a set of commit SHAs (strings) that should be EXCLUDED from
    "missing jira" consideration, because they have already been cherry-picked onto the maint branch.
    """
    repo = Repo(repo_root)
    [a, b] = splitRevRange(rev_range)
    branchCut = get_branchcut_sha(repo_root, rev_range)
    print ("## git cherry %s %s %s (branch cut)" % (a, b, branchCut), file=sys.stderr)
    cherries = repo.git.cherry(a, b, branchCut)
    lns = cherries.split('\n')
    excludeThese = set()
    for ln in lns:
        [symbol, sha] = ln.split(' ')
        if(symbol == '-'):
            # print("Exclude: %s" % sha, file=sys.stderr)
            excludeThese.add(sha)
    print("## Collected %d commit(s) to exclude" % len(excludeThese))
    return excludeThese

def splitRevRange(rev_range):
    """
    Return the start and end of the revrange
    """
    return rev_range.split('..')

def get_branchcut_sha(repo_root, rev_range):
    """
    Return the sha of the 'branch cut', that is, the merge-base.
    Returns a git commit
    """
    repo = Repo(repo_root)
    [a, b] = splitRevRange(rev_range)
    return repo.merge_base(a, b)[0]

def get_jira_instance(jira_hostname, jira_username, jira_password, **kwargs):
    jira_url = "https://%s" % jira_hostname
    if jira_username and jira_password:
        jira = JIRA(jira_url, basic_auth=(jira_username, jira_password))
    else:
        jira = JIRA(jira_url)
    return (jira_url, jira)

def make_icu_issue(jira_issue):
    """Yields an ICUIssue for the individual jira object"""
    commit_wanted = make_commit_wanted(jira_issue)
    return ICUIssue(jira_issue.key, jira_issue.fields.status.id == S_DONE, commit_wanted, jira_issue)


def get_jira_issues(jira_query, **kwargs):
    """
    Yields an ICUIssue for each issue in the user-specified query.
    """
    jira_url, jira = get_jira_instance(**kwargs)
    # Jira limits us to query the API using a limited batch size.
    start = 0
    batch_size = 100 # https://jira.atlassian.com/browse/JRACLOUD-67570
    while True:
        issues = jira.search_issues(jira_query, startAt=start, maxResults=batch_size)
        if len(issues) > 0:
            print("Loaded issues %d-%d" % (start + 1, start + len(issues)), file=sys.stderr)
        else:
            print(":warning: No issues matched the query.") # leave this as a warning
        for jira_issue in issues:
            yield make_icu_issue(jira_issue)
        if len(issues) < batch_size:
            break
        start += batch_size

jira_issue_map = dict() # loaded in main()
commit_metadata = None
fixversion_to_skip = None

def get_issue_cache_path(args):
    return '%s-%s.pickle' % (args.cache_for_dev, sha256(args.jira_query.encode()).hexdigest())

def load_issues(args):
    global jira_issue_ids, jira_issue_map, closed_jira_issue_ids

    if args.cache_for_dev:
        issue_cache_path = get_issue_cache_path(args)
        if os.path.exists(issue_cache_path):
            with open(issue_cache_path, 'rb') as f:
                (issues, jira_issue_ids, closed_jira_issue_ids, jira_issue_map) = pickle.load(f)
                print("Loaded jira cache to " + issue_cache_path, file=sys.stderr)
                return issues
    # proceed to load
    issues = list(get_jira_issues(**vars(args)))
    # add all queried issues to the cache
    for issue in issues:
        jira_issue_map[issue.issue_id] = issue
    # only the issue ids in-query
    jira_issue_ids = set(issue.issue_id for issue in issues)
    # only the closed issue ids in-query
    closed_jira_issue_ids = set(issue.issue_id for issue in issues if issue.is_closed)

    return issues

def save_issues_to_cache(args, issues):
    if args.cache_for_dev:
        issue_cache_path = get_issue_cache_path(args)
        with open(issue_cache_path, 'wb') as f:
            pickle.dump((issues, jira_issue_ids, closed_jira_issue_ids, jira_issue_map), f)
            print("Wrote cache to " + issue_cache_path, file=sys.stderr)

def get_single_jira_issue(issue_id, **kwargs):
    """
    Returns a single ICUIssue for the given issue ID.
    This can always be used (in- or out- of query issues), because it
    uses the jira_issue_map as the backing store.
    """
    if issue_id in jira_issue_map:
        # print("Cache hit: issue %s " % (issue_id), file=sys.stderr)
        return jira_issue_map[issue_id]
    jira_url, jira = get_jira_instance(**kwargs)
    jira_issue = jira.issue(issue_id)
    # singleCount = singleCount + 1
    if jira_issue:
        icu_issue = make_icu_issue(jira_issue)
    else:
        icu_issue = None
    jira_issue_map[issue_id] = icu_issue
    print("Loaded single issue %s (%d in cache) " % (issue_id, len(jira_issue_map)), file=sys.stderr)
    return icu_issue

def toplink(file=None):
    print("[🔝Top](#table-of-contents)", file=file)
    print(file=file)

def sectionToFragment(section):
    return re.sub(r' ', '-', section.lower())

# def aname(section):
#     """convert section name to am anchor"""
#     return "<a name=\"%s\"></a>" % sectionToFragment(section)

def print_sectionheader(section, file=None):
    """Print a section (###) header, including anchor"""
    print("### %s" % (section), file=file)
    #print("### %s%s" % (aname(section), section))

def main():
    global total_problems
    global commits
    global fixversion_to_skip
    args = flag_parser.parse_args()
    print("TIP: Have you pulled the latest main? This script only looks at local commits.", file=sys.stderr)
    if not args.jira_username or not args.jira_password:
        print("WARNING: Jira credentials not supplied. Sensitive tickets will not be found.", file=sys.stderr)
        authenticated = False
    else:
        authenticated = True

    if args.fix_version:
        fixversion_to_skip = 'v%s' % args.fix_version
        args.fixversion_to_skip = fixversion_to_skip

    if args.commit_metadata and os.path.exists(args.commit_metadata):
        commit_metadata = CommitMetadata(args.commit_metadata)
    else:
        commit_metadata = CommitMetadata()  # null
    args.commit_metadata_obj = commit_metadata

    # exclude these, already merged to old maint
    exclude_already_merged_to_old_maint = get_cherrypicked_commits(**vars(args))

    commits = list(get_commits(**vars(args)))

    def exclude_commit_sha(sha) -> bool:
        """Return true if this sha is excluded"""
        exclude = (sha in exclude_already_merged_to_old_maint) or (commit_metadata.get_commit_info(sha, skip=fixversion_to_skip) is not None)
        return exclude

    # commit_issue_ids is all commits in the git query. Excluding cherry exclusions.
    commit_issue_ids = set(commit.issue_id for commit in commits if commit.issue_id is not None and not exclude_commit_sha(commit.commit.hexsha))
    # which issues have commits that were excluded
    excluded_commit_issue_ids = set(commit.issue_id for commit in commits if commit.issue_id is not None and exclude_commit_sha(commit.commit.hexsha))
    excluded_commits = set(commit for commit in commits if exclude_commit_sha(commit.commit.hexsha))

    # grouped_commits is all commits and issue_ids in the git query, regardless of issue status
    # but NOT including cherry exclusions
    grouped_commits = [
        (issue_id, [commit for commit in commits if commit.issue_id == issue_id and not exclude_commit_sha(commit.commit.hexsha)])
        for issue_id in sorted(commit_issue_ids)
    ]

    issues = load_issues(args)

    # keep track of issues that we already said have no commit.
    no_commit_ids = set()

    # constants for the section names.
    CLOSED_NO_COMMIT = "Closed Issues with No Commit"
    CLOSED_ILLEGAL_RESOLUTION = "Closed Issues with Illegal Resolution or Commit"
    COMMIT_NO_JIRA = "Commits without Jira Issue Tag"
    COMMIT_OPEN_JIRA = "Commits with Open Jira Issue"
    COMMIT_JIRA_NOT_IN_QUERY = "Commits with Jira Issue Not Found"
    ISSUE_UNDER_REVIEW = "Issue is under Review"
    EXCLUDED_COMMITS = "Excluded Commits"

    # this controls the ordering of the sections
    ALL_SECTIONS = [CLOSED_NO_COMMIT, CLOSED_ILLEGAL_RESOLUTION, COMMIT_NO_JIRA, COMMIT_JIRA_NOT_IN_QUERY, COMMIT_OPEN_JIRA, ISSUE_UNDER_REVIEW, EXCLUDED_COMMITS]
    # a buffer for each section
    section_buffer = {}
    # a count for each section
    section_count = {}
    total_problems = 0
    if not args.nocopyright:
        print("<!--")
        print("Copyright (C) 2021 and later: Unicode, Inc. and others.")
        print("License & terms of use: http://www.unicode.org/copyright.html")
        print("-->")

    print("Commit Report")
    print("=============")
    print()
    print("Environment:")
    print("- Now: %s" % datetime.datetime.now().isoformat())
    print("- Latest Commit: %s/commit/%s" % (args.github_url, commits[0].commit.hexsha))
    print("- Jira Query: `%s`" % args.jira_query)
    print("- Rev Range: `%s`" % args.rev_range)
    print("- Authenticated: %s" % ("`Yes`" if authenticated else "`No` (sensitive tickets not shown)"))
    if fixversion_to_skip:
        print("- fix version (for COMMIT_METADATA SKIP) %s" % fixversion_to_skip)
    print()

    # big subfunction to calculate each section
    def run_section(section, out) -> int:
        """Calculate the text of a section

        Args:
            section (str): section name such as COMMIT_NO_JIRA
            out (file): file-ish, or None for immediate

        Returns:
            int: count of items in this section
        """
        global total_problems
        global commits
        count = 0
        if section == CLOSED_NO_COMMIT:
            print("Tip: Tickets with type 'Task' or 'User Guide' or resolution 'Fixed by Other Ticket' are ignored.", file=out)
            print(file=out)
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
                count += 1
                no_commit_ids.add(issue.issue_id)
                pretty_print_issue(issue, type=CLOSED_NO_COMMIT, file=out, **vars(args))
                # for debugging:
                # print("\t - commit class was %s or %s" % (make_commit_wanted(issue.issue), issue.commit_wanted), file=out)
                if issue.issue_id in excluded_commit_issue_ids:
                    print("\t - **Note: Has excluded/cherry-picked commits. Fix Version may be wrong.**", file=out)
                print(file=out)
        elif section == CLOSED_ILLEGAL_RESOLUTION:
            print("Tip: Fixed tickets should have resolution 'Fixed by Other Ticket' or 'Fixed'.", file=out)
            print("Duplicate tickets should have their fixVersion tag removed.", file=out)
            print("Tickets with resolution 'Fixed by Other Ticket' are not allowed to have commits.", file=out)
            print(file=out)
            found = False
            for issue in issues:
                if not issue.is_closed:
                    continue
                if issue.issue_id in no_commit_ids:
                    continue # we already complained about it above. don't double count.
                if issue.issue.fields.resolution and issue.issue.fields.resolution.id not in all_resolutions:
                    # Special case. Resolution is not in our list.
                    pretty_print_issue(issue, type=CLOSED_ILLEGAL_RESOLUTION, file=out, **vars(args))
                    print("\t- INTERNAL ERROR: resolution ID# %s is unknown, not in all_resolutions[]. Fix the commit checker." % issue.issue.fields.resolution.id, file=out)
                    count += 1
                    found = True
                    continue
                if issue.commit_wanted == CommitWanted["OPTIONAL"]:
                    continue
                if issue.issue_id in commit_issue_ids and issue.commit_wanted == CommitWanted["REQUIRED"]:
                    continue
                if issue.issue_id not in commit_issue_ids and issue.commit_wanted == CommitWanted["FORBIDDEN"]:
                    continue
                found = True
                total_problems += 1
                count += 1
                pretty_print_issue(issue, type=CLOSED_ILLEGAL_RESOLUTION, file=out, **vars(args))
                if issue.issue_id not in commit_issue_ids and issue.commit_wanted == CommitWanted["REQUIRED"]:
                    print("\t- No commits, and they are REQUIRED.", file=out)
                if issue.issue_id in commit_issue_ids and issue.commit_wanted == CommitWanted["FORBIDDEN"]:
                    print("\t- Has commits, and they are FORBIDDEN.", file=out)
                print(file=out)
        elif section == COMMIT_NO_JIRA:
            # TODO: This section should usually be empty due to the PR checker.
            # Pre-calculate the count and omit it.
            print("Tip: If you see your name here, make sure to label your commits correctly in the future.", file=out)
            print(file=out)
            found = False
            for commit in commits:
                if commit.issue_id is not None:
                    continue
                found = True
                total_problems += 1
                count += 1
                pretty_print_commit(commit, type=COMMIT_NO_JIRA, file=out, **vars(args))
                print(file=out)
        elif section == COMMIT_JIRA_NOT_IN_QUERY:
            print("Tip: Check that these tickets have the correct fixVersion tag.", file=out)
            print(file=out)
            found = False
            for issue_id, commits in grouped_commits:
                if issue_id in jira_issue_ids:
                    continue
                found = True
                total_problems += 1
                count += 1
                print("#### Issue %s" % issue_id, file=out)
                print(file=out)
                print("_issue was not found in `%s`_" % args.jira_query, file=out) # TODO: link to query?
                jira_issue = get_single_jira_issue(issue_id, **vars(args))
                if jira_issue:
                    pretty_print_issue(jira_issue, file=out, **vars(args))
                else:
                    print("*Jira issue does not seem to exist*", file=out)
                print(file=out)
                print("##### Commits with Issue %s" % issue_id, file=out)
                print(file=out)
                for commit in commits:
                    pretty_print_commit(commit, file=out, **vars(args))
                    print(file=out)
        elif section == COMMIT_OPEN_JIRA:
            print("Tip: Consider closing the ticket if it is fixed.", file=out)
            print(file=out)
            found = False
            componentToTicket = {}
            def addToComponent(component, issue_id):
                if component not in componentToTicket:
                    componentToTicket[component] = set()
                componentToTicket[component].add(issue_id)
            # first, scan ahead for the components
            for issue_id, commits in grouped_commits:
                if issue_id in closed_jira_issue_ids:
                    continue
                jira_issue = get_single_jira_issue(issue_id, **vars(args))
                if jira_issue and jira_issue.is_closed:
                    # JIRA ticket was not in query, but was actually closed.
                    continue
                if jira_issue_under_review(jira_issue):
                    # note: skip has to be recalculated in the other section
                    # issues_in_review.add(issue_id)
                    continue
                # OK. Now, split it out by component
                if jira_issue.issue.fields.components and len(jira_issue.issue.fields.components) > 0:
                    for component in jira_issue.issue.fields.components:
                        addToComponent(component.name, issue_id)
                else:
                    addToComponent("(no component)", issue_id)

            print("#### Open Issues by Component", file=out)  # "bonus" sub-section.
            print(file=out)
            for component in sorted(componentToTicket.keys()):
                print(" - **%s**: %s" % (component,  ' '.join("[%s](#issue-%s)" % (issue_id, sectionToFragment(issue_id)) for issue_id in componentToTicket[component])), file=out)

            print(file=out)
            print(file=out)

            # now, actually show the ticket list.
            for issue_id, commits in grouped_commits:
                if issue_id in closed_jira_issue_ids:
                    continue
                jira_issue = get_single_jira_issue(issue_id, **vars(args))
                if jira_issue and jira_issue.is_closed:
                    # JIRA ticket was not in query, but was actually closed.
                    continue
                if jira_issue_under_review(jira_issue):
                    # We already added it to the review list above.
                    continue
                print("#### Issue %s" % issue_id, file=out)
                print(file=out)
                print("_Jira issue is open_", file=out)
                if jira_issue:
                    pretty_print_issue(jira_issue, file=out, **vars(args))
                else:
                    print("*Jira issue does not seem to exist*", file=out)
                print(file=out)
                print("##### Commits with Issue %s" % issue_id, file=out)
                print(file=out)
                found = True
                total_problems += 1
                count += 1
                for commit in commits:
                    # print("@@@@ %s = %s / %s" % (issue_id, commit, commit.commit.summary), file=sys.stderr)
                    pretty_print_commit(commit, file=out, **vars(args))
                    print(file=out)
            # end COMMIT_OPEN_JIRA
        elif section == ISSUE_UNDER_REVIEW:
            print("These issues are otherwise accounted for above, but are in review.", file=out)
            # list of issues that are in review
            issues_in_review = set()
            # TODO: this next loop is copied from the above section, to make this section independent from COMMIT_OPEN_JIRA
            # TODO: ideally this would be calculated once above.
            for issue_id, commits in grouped_commits:
                if issue_id in closed_jira_issue_ids:
                    continue
                jira_issue = get_single_jira_issue(issue_id, **vars(args))
                if jira_issue and jira_issue.is_closed:
                    # JIRA ticket was not in query, but was actually closed.
                    continue
                if jira_issue_under_review(jira_issue):
                    print("skipping for now- %s is under review" % issue_id, file=sys.stderr)
                    issues_in_review.add(issue_id)
            for issue_id in sorted(issues_in_review):
                jira_issue = get_single_jira_issue(issue_id, **vars(args))
                pretty_print_issue(jira_issue, type=ISSUE_UNDER_REVIEW, **vars(args), file=out)
                count += 1
        elif section == EXCLUDED_COMMITS:
            if len(excluded_commits) > 0:
                print("These commits are not considered as part of current version tickets.", file=out)
                print(file=out)
                count = len(excluded_commits)
                for commit in excluded_commits:
                    pretty_print_commit(commit, file=out, **vars(args))
                    from_commit_metadata = commit_metadata.get_commit_info(commit.commit.hexsha, skip=fixversion_to_skip)
                    if from_commit_metadata:
                        (sha, id, message) = from_commit_metadata
                        # Print any notes in the skip list here.
                        if id:
                            print("\t- COMMIT_METADATA.md: %s" % id, file=out)
                        if message:
                            print("\t- COMMIT_METADATA.md: %s" % message, file=out)
                    if commit.commit.hexsha in exclude_already_merged_to_old_maint:
                        print("\t- NOTE: excluded due to already being merged to old maint", file=out)
        else:
            print("### @@@@@ Unknown section: %s" % section, file=out)
        return count

    print("-----")
    # Now, run each section
    for section in ALL_SECTIONS:
        # empty buffer
        section_buffer[section] = io.StringIO();
        # For debugging: setting to None will cause immediate output
        # section_buffer[section] = None
        section_count[section] = run_section(section, section_buffer[section])
    print("-----")
    print("_(anything between the above two lines is an error)_")

    print()
    print("Total problem(s): %d" % total_problems)
    print()

    # Now run the ToC
    print("## Table Of Contents")
    print("Note: empty categories are omitted.")
    for section in ALL_SECTIONS:
        if section_count[section] > 0:
            print("- [%s](#%s) %d" % (section, sectionToFragment(section), section_count[section]))
        else:
            print("- _%s_" % (section))
    print()
    print("## Problem Categories")
    for section in ALL_SECTIONS:
        if section_count[section] > 0:
            print_sectionheader(section)
            toplink()
            print("_%d item(s)_" % section_count[section])
            print(section_buffer[section].getvalue()) # print that section
    # save out the dev cache here, so that any 1-off issues are also included in the cache
    save_issues_to_cache(args, issues)

if __name__ == "__main__":
    main()
