# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Execute external commands, reliably, with logging."""

import os
import subprocess

from libs import icufs
from libs import iculog


def run_with_logging(
    command: str,
    logfile: str = 'last_run.log',
    root_dir: str = os.getenv('ICU_ROOT'),
    ok_result: list[int]|None = None,
) -> subprocess.CompletedProcess[str]:
  """The method that executes the step proper.

  Args:
    command: the external command to execute
    logfile: the log file where the result, stdout, and stderr are collected
    root_dir: the path where the log file is saved
    ok_result: a list of acceptable execution return codes,
        because not all non-zero return code means an error
  Returns:
    the result of the subprocess execution.
  """
  if root_dir is None:
    root_dir = '.'
  if ok_result is None:
    ok_result = [0]
  iculog.info(f'[execute] {command}\n        logfile: {logfile}')
  if ok_result and len(ok_result) == 0:
    should_check = True
  elif ok_result and len(ok_result) == 1 and ok_result[0] == 0:
    should_check = True
  else:
    should_check = False
  try:
    result: subprocess.CompletedProcess[str] = subprocess.run(
        command,
        encoding='utf-8',
        stdout=subprocess.PIPE,  # Capture stdout
        stderr=subprocess.STDOUT,  # Merge stderr into stdout
        shell=True,
        check=should_check
    )
  except subprocess.CalledProcessError as ex:
    iculog.error(f'  output     : {ex.output}')
    iculog.failure(f'Execution failed with return code: {ex.returncode}')
    exit(ex.returncode)

  if logfile and root_dir:
    abs_logdir = os.path.join(root_dir, 'target', 'pylogs')
    icufs.mkdir(abs_logdir)
    abs_logfile = os.path.join(abs_logdir, logfile)
    icufs.rmfile(abs_logfile)
    with open(abs_logfile, 'w', encoding='utf-8') as f:
      f.write('==================\n')
      f.write('= args           =\n')
      f.write('==================\n')
      f.write(result.args + '\n')
      f.write('==================\n')
      f.write('= returncode     =\n')
      f.write('==================\n')
      f.write(str(result.returncode) + '\n')
      f.write('==================\n')
      f.write('= stdout         =\n')
      f.write('==================\n')
      if result.stdout:
        f.write(result.stdout + '\n')
      else:
        f.write('NONE\n')
      # There is no need to show stderr because in `subprocess.run` we
      # merged stderr and stdout (stderr=subprocess.STDOUT)
      f.write('==================\n')

  # Certain non-zero exit codes are acceptable.
  # For example diff returns 1 if there are differences found.
  if result.returncode not in ok_result:
    iculog.failure(f'Failed with unexpected return code: {result.returncode}\n'
                  f'Acceptable codes: {ok_result}, see log file for details')
    exit(result.returncode)

  return result
