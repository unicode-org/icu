# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Executes uconfig variations check.

See
http://site.icu-project.org/processes/release/tasks/healthy-code#TOC-Test-uconfig.h-variations
for more information.
"""

import getopt
import os
import re
import subprocess
import sys

excluded_unit_test_flags = ['UCONFIG_NO_CONVERSION', 'UCONFIG_NO_FILE_IO'];

def ReadFile(filename):
    """Reads a file and returns the content of the file

    Args:
      command: string with the filename.

    Returns:
      Content of file.
    """

    with open(filename, 'r') as file_handle:
      return file_handle.read()

def RunCmd(command):
    """Executes the command, returns output and exit code, writes output to log

    Args:
      command: string with the command.

    Returns:
      stdout and exit code of command execution.
    """

    command += ' >> uconfig_test.log 2>&1'
    print(command)
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, close_fds=True)
    stdout, _ = p.communicate()
    return stdout, p.returncode

def ExtractUConfigNoXXX(uconfig_file):
    """Parses uconfig.h and returns a list of UCONFIG_NO_XXX labels.
    Initializes test result structure.

    Args:
      uconfig_file: common/unicode/uconfig.h as string.

    Returns:
      List of all UCONFIG_NO_XXX flags found in uconfig.h, initialized test
      result structure.
    """

    uconfig_no_flags_all = []
    test_results = {}
    uconfig_no_regex = r'UCONFIG_NO_[A-Z_]*'

    # Collect all distinct occurences of UCONFIG_NO_XXX matches in uconfig.h.
    for uconfig_no_flag in re.finditer(uconfig_no_regex, uconfig_file):
        if uconfig_no_flag.group(0) not in uconfig_no_flags_all:
            uconfig_no_flags_all.append(uconfig_no_flag.group(0))

    # All UCONFIG_NO_XXX flags found in uconfig.h come in form of a guarded
    # definition. Verify the existence, report error if not found.
    for uconfig_no_flag in uconfig_no_flags_all:
        uconfig_no_def_regex = r'(?m)#ifndef %s\n#\s+define %s\s+0\n#endif$' % (
        uconfig_no_flag, uconfig_no_flag)
        uconfig_no_def_match = re.search(uconfig_no_def_regex, uconfig_file)
        if not uconfig_no_def_match:
            print('No definition for flag %s found!\n' % uconfig_no_flag)
            sys.exit(1)

    test_results = {f: {'unit_test': False, 'hdr_test': False} for f in uconfig_no_flags_all}
    test_results['all_flags'] = {'unit_test': False, 'hdr_test' : False}

    return uconfig_no_flags_all, test_results

def BuildAllFlags(uconfig_no_list):
    """Builds sequence of -Dflag=1 with each flag from the list."""

    flag_list = ['-D' + uconfig_no + '=1' for uconfig_no in uconfig_no_list]

    return ' '.join(flag_list)

def RunUnitTests(uconfig_no_list, test_results):
    """Iterates over all flags, sets each individually during ICU configuration
       and executes the ICU4C unit tests.

    Args:
      uconfig_no_list: list of all UCONFIG_NO_XXX flags to test with.
      test_results: dictionary to record test run results.

    Returns:
      test_results: updated test result entries.
    """

    for uconfig_no in uconfig_no_list:
        _, exit_code = RunCmd(
                './runConfigureICU Linux CPPFLAGS=\"-D%s=1"' % uconfig_no)
        if exit_code != 0:
            print('ICU4C configuration for flag %s failed' % uconfig_no)
            sys.exit(1)
        print('Running unit tests with %s set to 1.' % uconfig_no)
        _, exit_code = RunCmd('make -j2 check')
        test_results[uconfig_no]['unit_test'] = (exit_code == 0)
        RunCmd('make clean')

    # Configure ICU with all UCONFIG_NO_XXX flags set to 1 and execute
    # the ICU4C unit tests.
    all_unit_test_config_no = BuildAllFlags(uconfig_no_list)
    _, exit_code = RunCmd(
        'CPPFLAGS=\"%s\" ./runConfigureICU Linux' % all_unit_test_config_no)
    if exit_code != 0:
        print('ICU configuration with all flags set failed')
        sys.exit(1)
    print('Running unit tests with all flags set to 1.')
    _, exit_code = RunCmd('make -j2 check')
    test_results['all_flags']['unit_test'] = (exit_code == 0)
    RunCmd('make clean')

    return test_results

def RunHeaderTests(uconfig_no_list, test_results):
    """Iterates over all flags and executes the header test.

    Args:
      uconfig_no_list: list of all UCONFIG_NO_XXX flags to test with.
      test_results: dictionary to record test run results.

    Returns:
      test_results: updated test result entries.
    """

    # Header tests needs different setup.
    RunCmd('mkdir /tmp/icu_cnfg')
    out, exit_code = RunCmd('./runConfigureICU Linux --prefix=/tmp/icu_cnfg')
    if exit_code != 0:
        print('ICU4C configuration for header test failed!')
        print(out)
        sys.exit(1)

    _, exit_code = RunCmd('make -j2 install')
    if exit_code != 0:
        print('make install failed!')
        sys.exit(1)

    for uconfig_no in uconfig_no_list:
        print('Running header tests with %s set to 1.' % uconfig_no)
        _, exit_code = RunCmd(
            'PATH=/tmp/icu_cnfg/bin:$PATH make -C test/hdrtst UCONFIG_NO=\"-D%s=1\" check' % uconfig_no)
        test_results[uconfig_no]['hdr_test'] = (exit_code == 0)

    all_hdr_test_flags = BuildAllFlags(uconfig_no_list)
    print('Running header tests with all flags set to 1.')
    _, exit_code = RunCmd(
        'PATH=/tmp/icu_cnfg/bin:$PATH make -C test/hdrtst UCONFIG_NO=\"%s\" check' % all_hdr_test_flags)
    test_results['all_flags']['hdr_test'] = (exit_code == 0)

    return test_results

def main():
    # Read the options and determine what to run.
    run_hdr = False
    run_unit = False
    optlist, _ = getopt.getopt(sys.argv[1:], "pu")
    for o, _ in optlist:
        if o == "-p":
            run_hdr = True
        elif o == "-u":
            run_unit = True

    os.chdir('icu4c/source')
    orig_uconfig_file = ReadFile('common/unicode/uconfig.h')

    all_uconfig_no_flags, test_results = ExtractUConfigNoXXX(orig_uconfig_file)
    if not all_uconfig_no_flags:
        print('No UCONFIG_NO_XXX flags found!\n')
        sys.exit(1)

    if run_unit:
        RunUnitTests(
            [u for u in all_uconfig_no_flags if u not in excluded_unit_test_flags],
            test_results)
    if run_hdr:
        RunHeaderTests(all_uconfig_no_flags, test_results)

    # Review test results and report any failures.
    # 'outcome' will be returned by sys.exit(); 0 indicates success, any
    # other value indicates failure.
    outcome = 0
    print('Summary:\n')
    for uconfig_no in all_uconfig_no_flags:
        if run_unit and (uconfig_no not in excluded_unit_test_flags):
            if not test_results[uconfig_no]['unit_test']:
                outcome = 1
                print('%s: unit tests fail' % uconfig_no)
        if run_hdr and not test_results[uconfig_no]['hdr_test']:
            outcome = 1
            print('%s: header tests fails' % uconfig_no)
    if run_unit and not test_results['all_flags']['unit_test']:
        outcome = 1
        print('all flags to 1: unit tests fail!')
    if run_hdr and not test_results['all_flags']['hdr_test']:
        outcome = 1
        print('all flags to 1: header tests fail!')
    if outcome == 0:
        print('Tests pass for all uconfig variations!')
    sys.exit(outcome)


if __name__ == '__main__':
  main()
