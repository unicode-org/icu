# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for the external commands execution module."""

import subprocess
import tempfile
import unittest

from libs import icufs
from libs import iculog
from libs import icuproc


class TestIcuProc(unittest.TestCase):
  """Test class for the file system utilities."""

  def test_run_cmd(self):
    ok_result = [0, 2]
    result: subprocess.CompletedProcess[str] = icuproc.run_with_logging(
        'grep --help', ok_result=ok_result
    )
    # WARNING: on some system it returns 0, on some it returns 2. Both are OK.
    self.assertIn(result.returncode, ok_result)
    self.assertRegex(result.stdout, 'pattern')
    self.assertRegex(result.stdout, 'directories')
    self.assertRegex(result.stdout, 'binary')

  def test_run_cmd_bad_flag(self):
    # Test valid command with invalid flag
    iculog.warning('The ERROR and CRITICAL messages here are expected')
    with self.assertRaises(SystemExit) as _:
      icuproc.run_with_logging('grep -badxyz .')

  def test_run_cmd_bad_arg(self):
    # Test valid command with invalid argument
    iculog.warning('The ERROR and CRITICAL messages here are expected')
    with self.assertRaises(SystemExit) as _:
      icuproc.run_with_logging('grep xyz bad')

  def test_run_bad_cmd(self):
    iculog.warning('The ERROR and CRITICAL messages here are expected')
    with self.assertRaises(SystemExit) as _:
      icuproc.run_with_logging('no_such_cmd')

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    # Would be good to add argument `delete=True`, from Python 3.12
    cls.root_dir: tempfile.TemporaryDirectory[str] = (
        tempfile.TemporaryDirectory(prefix='icuproc_test_')
    )
    icufs.mkdir(cls.root_dir.name)

  @classmethod
  def tearDownClass(cls):
    super().tearDownClass()
    cls.root_dir.cleanup()


if __name__ == '__main__':
  unittest.main()
