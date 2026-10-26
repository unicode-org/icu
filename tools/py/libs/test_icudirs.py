# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Tests for the directories system module."""

import os
import tempfile
import unittest

from libs import icudirs


class TestIcuDirs(unittest.TestCase):
  """Test class for the directories system module."""

  root_dir: tempfile.TemporaryDirectory[str]

  def test_icu_dir_default(self):
    """Test icu directory, basic"""
    icu_dir = icudirs.icu_dir()
    self.assertIsNotNone(icu_dir)
    self.assertNotEqual(icu_dir, '')
    self.assertTrue(os.path.isdir(os.path.join(icu_dir, 'icu4j')))

    expected = os.path.join(self.root_dir.name, 'icu')
    os.environ['ICU_DIR'] = expected
    icu_dir = icudirs.icu_dir()
    self.assertEqual(icu_dir, expected)

  def test_cldr_dir(self):
    """Test cldr directory, including many expected failures"""
    # Env variable undefined
    os.environ['CLDR_DIR'] = ''
    with self.assertRaises(SystemExit) as _:
      icudirs.cldr_dir()

    # Point to something that does not exist
    os.environ['CLDR_DIR'] = 'something_that_does_not_exist'
    with self.assertRaises(SystemExit) as _:
      icudirs.cldr_dir()

    # Point CLDR_DIR to a file, should fail
    fake_dir = os.path.join(self.root_dir.name, 'cldr/pom.xml')
    os.environ['CLDR_DIR'] = fake_dir
    with self.assertRaises(SystemExit) as _:
      icudirs.cldr_dir()

    # Point CLDR_DIR to the icu_dir, should fail as environment variable
    # is defined, but the folder structure does not match the expected one.
    fake_dir = os.path.join(self.root_dir.name, 'icu')
    os.environ['CLDR_DIR'] = fake_dir
    with self.assertRaises(SystemExit) as _:
      icudirs.cldr_dir()

    # Point to a correct path, should succeed
    fake_dir = os.path.join(self.root_dir.name, 'cldr')
    os.environ['CLDR_DIR'] = fake_dir
    cldr_dir = icudirs.cldr_dir()
    self.assertEqual(cldr_dir, fake_dir)

  def test_cldr_prod_dir(self):
    """Test cldr-staging directory, for completeness"""
    fake_dir = os.path.join(self.root_dir.name, 'cldr-staging')
    os.environ['CLDR_TMP_DIR'] = fake_dir
    cldr_prod_dir = icudirs.cldr_prod_dir()
    self.assertEqual(cldr_prod_dir, fake_dir)

  @classmethod
  def _create_fake_folder_struct(
      cls,
      folder_name: str,
      dirs_to_check: list[str] | None = None,
      files_to_check: list[str] | None = None,
  ):
    """Prepare a mock directory structure for icu, cldr, and cldr-staging"""
    fake_dir = os.path.join(cls.root_dir.name, folder_name)
    if dirs_to_check:
      for fake_dir_name in dirs_to_check:
        os.makedirs(os.path.join(fake_dir, fake_dir_name), exist_ok=True)
    if files_to_check:
      for fake_file_name in files_to_check:
        fake_file = os.path.join(fake_dir, fake_file_name)
        with open(fake_file, 'w', encoding='utf-8') as f:
          f.write('Fake, for testing')  # Expected file

  @classmethod
  def setUpClass(cls):
    super().setUpClass()
    cls.root_dir = tempfile.TemporaryDirectory(prefix='icudir_test_')
    # Create a fake CLDR folder
    cls._create_fake_folder_struct(
        'icu',
        ['icu4c', 'icu4j', 'tools'],
        ['pom.xml', 'README.md', 'CONTRIBUTING.md'],
    )
    # Create a fake CLDR folder
    cls._create_fake_folder_struct(
        'cldr',
        ['common', 'keyboards', 'specs', 'tools'],
        ['pom.xml', 'README.md', 'CONTRIBUTING.md'],
    )
    # Create a fake CLDR-staging folder
    cls._create_fake_folder_struct(
        'cldr-staging',
        ['births', 'docs/charts', 'production/common/main'],
        ['pom.xml', 'README-common.md', 'README-keyboards.md'],
    )

  @classmethod
  def tearDownClass(cls):
    super().tearDownClass()
    cls.root_dir.cleanup()


if __name__ == '__main__':
  unittest.main()
