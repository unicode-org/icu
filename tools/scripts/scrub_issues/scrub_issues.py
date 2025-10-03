# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Examines code in ICU4C and ICU4J branches, finding lines with 'knownIssue' and
# 'TODO'. Then uses JIRA API to check the status of each of the found issues.

# Note that the "jira" module must be available to run this.

# Types of results:
#  non-JIRA issue ids that are not short forms of a JIRA id
#  issues with a JIRA number but that don't start with 'ICU-' or 'CLDR-'
#  JIRA issues that are resolved
#  Unresolved JIRA issues found in code

# Running this:
#  python scrub_issues.py --icu_base <full path to "/icu/" on local computer
#
# option parameter 'detail_level' may be set to 'summary' to prevent output of
#   lines in code for each issue.

from jira import JIRA

import argparse
import logging
import os.path
import re
import subprocess
import sys

# Globals
jira = JIRA(server='https://unicode-org.atlassian.net')

projects = jira.projects()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('scrub issues')

def setup_args(args):
    parser = argparse.ArgumentParser(prog='scrub_issues')
    parser.add_argument('--icu_base',
                        help='base directory of icu release',
                        required=True)

    parser.add_argument('--detail_level',
                        help='how much info to give',
                        choices=['detail', 'summary'],
                        default = 'detail'
    )

    parser.add_argument('--test_jira',
                        help='run single test for connection to JIRA',
                        required=False,
                        default=False)

    new_args = parser.parse_args(args)


    return new_args


def create_test_files(base_directory):
    # TODO: run 'grep -rn' for strings TODO and logKnownIssue in these places
    # generates the raw files for analysis in the given base directory
    file_subpaths = [
        {'subdir': 'icu4j',
         'match_string': 'logKnownIssue',
         'output_file': 'icu4j/logKnownIssue.txt'},
        {'subdir': 'icu4j',
         'match_string': 'TODO',
         'output_file': 'icu4j/TODOs.txt'},
        {'subdir': 'icu4c/source',
         'match_string': 'logKnownIssue',
         'output_file': 'icu4c/source/logKnownIssue.txt'},
        {'subdir': 'icu4c/source',
         'match_string': 'TODO',
         'output_file': 'icu4c/source/TODOs.txt'},
    ]

    # Use subprocess to run grep on the given subdirectories of the ICU release
    all_results = []
    for item in file_subpaths:
        target_dir = os.path.join(base_directory, item['subdir'])
        logging.info('Running: %s on target directory %s', item, target_dir)

        result = subprocess.run(['grep', '-rn',
                                 item['match_string'],
                                 target_dir],
                                capture_output=True)

        logging.debug('!!! Command gives result %s',
                     result.returncode)

        result_lines = result.stdout.splitlines()

        # This may include None items
        issue_details = [extract_issue_detail(base_directory, str(line)) for line in result_lines]

        all_results.append([item['output_file'], issue_details])

    return all_results


check_issue_re = re.compile(r'(logKnownIssue|TODO)\(([^\)]+)')
# Get the file name if it's a match
file_linenum_re = re.compile(r'^([^:]+):(\d+)')

def extract_issue_detail(base_directory, line):
    code_file = None
    code_line = -1
    issue_detail = None

    # Try to get the TODO or the knownIssue
    ki_search = check_issue_re.search(line)
    if ki_search:
        file_search = file_linenum_re.search(line)
        if file_search:
            code_file = file_search.group(1)
            code_line = int(file_search.group(2))
    else:
        # There's no match for logKnownIssue or TODO
        return None

    issue_type = None
    short_file_path = ''
    if ki_search:
        issue_type = ki_search.group(1)
        issue_id = ki_search.group(2)
        issue_id = issue_id.replace('\"', '')
        comma_pos = issue_id.find(',')
        if comma_pos >= 0:
            issue_id = issue_id[0:comma_pos]

        # Use a shorter path name
        short_file_path = code_file.replace(base_directory, '')

        if short_file_path == '':
            print('--- WHY??? %s' % line)

        if code_file:
            issue_detail = [issue_id, issue_type, short_file_path, code_line]

    return issue_detail


def show_detail(issue_dict, id_key):
        try:
            lines_from_grep = issue_dict[id_key]
            for line in lines_from_grep:
                logger.info('       %s', line)
        except KeyError as err:
            logging.warn('Issue %s not found in issue dictionary', id_key)


def test_using_jira_api():
    # One instance for testing JIRA connection
    logger.info('TESTING THE JIRA CONNECTION')
    logger.info('%d projects found in %s', len(projects), jira)

    logger.info('Projects = %s', projects)

    # Example: Get an issue.
    issue = jira.issue('ICU-21859')

    logger.info('ISSUE: %s' % (issue))
    logger.info('ISSUE fields: %s' % (issue.fields.labels))

    fields = issue.__dict__
    keys = fields.keys()
    logger.info('ISSUE keys: %s', keys)

    logger.info('STATUS: %s' % fields['raw'].get('fields').get('status').get('name'))

    for key in keys:
        logger.debug('%s: %s' % (key, fields[key]))


def check_jira_issue(issue_id):
    try:
        jira_entry = jira.issue(issue_id)

        # Try to get the status of this entry
        fields = jira_entry.__dict__
        status_name = fields['raw'].get('fields').get('status').get('name')

        status = None
        return status_name
    except BaseException as error:
        return None


def check_jira_status_for_all_issues(issue_dict, settings):
    logger.info('----------------------------------------------------------------')
    resolved_issue_ids = []
    unresolved_issue_ids = []
    problem_ids = []
    for issue in issue_dict:
        # Check if issue id string is fully qualified for Jira to recognize it,
        # (ex: ICU-XXXX, CLDR-XXXX).
        status = check_jira_issue(issue)
        try_id = None
        if not status:
            # TODO: check if the issue id can be fixed
            try:
                int_id = int(issue.replace('#', ''))
                try_id = 'ICU-%d' % int_id
                status = check_jira_issue(try_id)

                if not status:
                    # Try CLDR
                    try_id = 'CLDR-%d' % int_id
                    status = check_jira_issue(try_id)

            except BaseException as error:
                # Not a simple number. Try other replacements
                try_id = issue.replace('cldrbug:', 'CLDR-')
                if try_id != issue:
                    status = check_jira_issue(try_id)

                if not status:
                    # Some items have 'Jira ' in the issue name
                    try_id = issue.replace('Jira ', '')
                    status = check_jira_issue(try_id)

                if not status:
                    # Some items have 'cldrbug:' in the issue name
                    try_id = issue.replace('cldrbug: ', 'CLDR-')
                    status = check_jira_issue(try_id)

        if not status:
            logger.debug('Not in JIRA: issue "%s"', issue)
            problem_ids.append(issue)
            if settings.detail_level != 'summary':
                show_detail(issue_dict, issue)
        else:
            if try_id:
                logger.info('REPLACEMENT %s --> %s (%s)' %
                            (issue, try_id, status))
                if settings.detail_level != 'summary':
                    show_detail(issue_dict, issue)
            else:
                # There is status but try_id is still None. Can that happen?
                # If so, skip the item.
                pass

            if status == 'Done':
                resolved_issue_ids.append(issue)
            else:
                unresolved_issue_ids.append([issue, status])

    return resolved_issue_ids, problem_ids, unresolved_issue_ids


def main(args):
    settings = setup_args(args[1:])

    if settings.test_jira:
        test_using_jira_api()  # Checking if the API is OK
        return

    todo_known_issues = create_test_files(settings.icu_base)
    logging.debug('CREATE_TEST_FILES gives %s items',
                 len(todo_known_issues))
    issue_dict = {}

    logging.info('Base Directory: %s', settings.icu_base)

    all_issue_ids = []

    for item in todo_known_issues:
        all_issue_ids.append(item)

    # TODO: if detail_level is detail, output info on the file and line for each item
    # For all these ids, get a set of the issues to look up.
    for group in all_issue_ids:
        if not group:
            continue

        input_file = group[0]
        issues = group[1]
        for issue_data in issues:
            if not issue_data:
                continue

            # Need to remember the base icu4c/icu4j
            issue_id = issue_data[0].strip()
            if issue_id in issue_dict:
                issue_dict[issue_id].append(issue_data)
            else:
                issue_dict[issue_id] = [issue_data]

    # Now we have a dictionary of all the issues to be checked.
    # Next, look at the status of all of these in JIRA
    closed_issue_ids, problem_ids, unresolved_ids = check_jira_status_for_all_issues(issue_dict, settings)

    logger.info('')
    logger.info('----------------------------------------------------------------')
    logger.info('PROBLEM IDS: %d not found in JIRA' % (len(problem_ids)))
    for id in sorted(problem_ids):
        issues = issue_dict[id]
        logger.warning('Not in JIRA : missing id = "%s" (%d instances)', id, len(issues))
        if settings.detail_level != 'summary':
            show_detail(issue_dict, id)

        for issue in issues:
            logger.debug('       %s', issue)

    logger.info('')
    logger.info('----------------------------------------------------------------')
    logger.info('Unresolved IDS: %d still not "Done" in Jira' % (len(unresolved_ids)))
    for id in sorted(unresolved_ids):
        logger.info('id: %s' % id)
        issue_key = id[0]
        if settings.detail_level != 'summary':
            show_detail(issue_dict, issue_key)

    # Check on all the problem ids
    logger.info('')
    logger.info('----------------------------------------------------------------')
    logger.info('%d Closed ids: %s',
                len(closed_issue_ids), (closed_issue_ids))
    for id in sorted(closed_issue_ids):
        lines_from_grep = issue_dict[id]
        for line in lines_from_grep:
            logger.info('       %s',  line)

    # Try to fix the comment in the file(s) for each resolved ID.

if __name__ == '__main__':
    main(sys.argv)
