# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for the file system module."""

import os
import tempfile
import unittest

from libs import icufs


class TestIcuFs(unittest.TestCase):
  """Test class for the file system utilities."""

  def _makedir(self, dir_name: str):
    self.assertFalse(os.path.exists(dir_name), 'folder/file already exists')
    icufs.mkdir(dir_name)
    self.assertTrue(os.path.exists(dir_name), 'unable to create folder')
    self.assertTrue(os.path.isdir(dir_name), 'not a folder')

  def _makefile(self, file_name: str):
    self.assertFalse(os.path.exists(file_name), 'folder/file already exists')
    # Create the file. We want to control when it is closed, so that remove does
    # will not fail because it is still opened.
    file = open(file_name, 'w', encoding='utf-8')
    file.write('Content of the new file.')
    file.close()
    self.assertTrue(os.path.exists(file_name), 'unable to create file')
    self.assertTrue(os.path.isfile(file_name), 'not a file')

  def test_create_invalidname_dir(self):
    dir_name = os.path.join(self.root_dir.name, 'bad_:_?_*_\000_\\folder')
    try:
      icufs.mkdir(dir_name)
      self.fail('bad folder name should fail')
    except ValueError as _:
      pass

  def test_remove_non_existing_dir(self):
    dir_name = os.path.join(self.root_dir.name, 'no_existing_folder')
    icufs.rmdir(dir_name)

  def test_remove_non_existing_file(self):
    file_name = os.path.join(self.root_dir.name, 'no_existing_file.txt')
    icufs.rmfile(file_name)

  def test_folder_readonly(self):
    """Test makecleandir. Which deletes an existing directory, if it exists."""
    dir_name = os.path.join(self.root_dir.name, 'folder_ro')
    self._makedir(dir_name)
    file_name = os.path.join(dir_name, 'some_file_to_delete_ro1.txt')
    self._makefile(file_name)
    os.chmod(dir_name, 0o000)  # Make the folder readonly
    icufs.rmdir(dir_name)  # Remove the folder
    self.assertFalse(os.path.exists(dir_name), 'unable to remove folder')

  def test_rmfile_readonly(self):
    """Test makecleandir. Which deletes an existing directory, if it exists."""
    file_name = os.path.join(self.root_dir.name, 'some_file_to_delete_ro.txt')
    self._makefile(file_name)
    os.chmod(file_name, 0o000)  # Make it readonly
    icufs.rmfile(file_name)  # Remove the file
    self.assertFalse(os.path.exists(file_name), 'unable to remove file')

  def test_rmfile(self):
    """Test makecleandir. Which deletes an existing directory, if it exists."""
    file_name = os.path.join(self.root_dir.name, 'some_file_to_delete.txt')
    self.assertFalse(os.path.exists(file_name), 'file already exists')
    # Create the file. I want to explicitly close.
    file = open(file_name, 'w', encoding='utf-8')
    file.write('Content of the new file.')
    file.close()
    self.assertTrue(os.path.exists(file_name), 'unable to create file')
    self.assertTrue(os.path.isfile(file_name), 'not a file')
    # Remove the file
    icufs.rmfile(file_name)
    self.assertFalse(os.path.exists(file_name), 'unable to remove file')

  def test_makecleandir(self):
    """Test makecleandir. Which deletes an existing directory, if it exists."""
    dir_name = os.path.join(self.root_dir.name, 'tmp_dirty_dir')
    self._makedir(dir_name)
    file_name = os.path.join(dir_name, 'some_file.txt')
    self._makefile(file_name)
    icufs.makecleandir(dir_name)
    self.assertTrue(os.path.exists(dir_name),
                    'unable to remove old folder or create new one')
    self.assertFalse(os.path.exists(file_name),
                     'the old file still exists in the folder')
    icufs.rmdir(dir_name)

  def test_mkdir_rmdir(self):
    """Test mkdir and rmdir."""
    dir_name = os.path.join(self.root_dir.name, 'tmp_dir')
    self._makedir(dir_name)
    os.chmod(dir_name, 0o000)  # Make it readonly
    icufs.rmdir(dir_name)
    self.assertFalse(os.path.exists(dir_name), 'unable to remove folder')

  def test_pushd_popd(self):
    """Test pushd and popd."""
    cwd: str = os.getcwd()
    old_dir: str = icufs.pushd(self.root_dir.name)
    self.assertEqual(old_dir, cwd,
                     'the dir returned by `pushd` is not the one where we were')
    self.assertEqual(os.getcwd(), os.path.realpath(self.root_dir.name),
                     'we failed to change the folder')
    icufs.popd(old_dir)
    self.assertEqual(os.getcwd(), os.path.realpath(old_dir),
                     'we failed to return to the original folder')

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    cls.root_dir: tempfile.TemporaryDirectory[str] = (
        tempfile.TemporaryDirectory(prefix='icufs_test_')
    )

  @classmethod
  def tearDownClass(cls):
    super().tearDownClass()
    cls.root_dir.cleanup()


if __name__ == '__main__':
  unittest.main()
