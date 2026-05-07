# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""Get the paths for various directories, checking for some files / folders."""

import os
import sys

from libs import iculog


def _get_save_dir(
    key: str,
    defalt_value: str | None = None,
    dirs_to_check: tuple[str, ...] = (),
    files_to_check: tuple[str, ...] = (),
) -> str | None:
  """Get the path for a directory, checking for some files / folders."""
  result: str | None = os.environ.get(key)
  # Variable not set, or set to empty string.
  if not result:
    if not defalt_value:
      iculog.error(f'Environment variable {key} is not set.')
      return None
    iculog.info(
        f'Environment variable {key} is not set.\n'
        + f'  Using the default value: "{defalt_value}".'
    )
    result = defalt_value
  # Options: either complain if it is not an absolute path,
  # or make it absolute and set the environment.
  # result = os.path.abspath(result)
  # os.environ[key] = result
  result = os.path.normpath(result)
  # Does not point to a directory.
  if not os.path.isdir(result):
    iculog.error(
        f'Environment variable {key} is set to {result}.\n'
        + '  But it does not point to a directory.'
    )
    return None
  # Does not contain the expected subdirectories.
  for chk_dir in dirs_to_check:
    if not os.path.isdir(os.path.join(result, chk_dir)):
      iculog.error(
          f'Environment variable {key} is set to {result}.\n'
          + '  But it does not seem to point to a valid directory.\n'
          + f'  Missing subdirectory: {chk_dir}/'
      )
      return None
  # Does not contain the expected files.
  for chk_file in files_to_check:
    if not os.path.isfile(os.path.join(result, chk_file)):
      iculog.error(
          f'Environment variable {key} is set to {result}.\n'
          + '  But it does not seem to point to a valid directory.\n'
          + f'  Missing file: {chk_file}'
      )
      return None
  return result


def icu_dir(defalt_value: str | None = None) -> str:
  """Get the path for the 'icu' repository directory."""
  if not defalt_value:
    # If a default value is not provided, determine it based on the location of
    # this file, assuming this file is located at <icu_root>/tools/py/libs/
    defalt_value = os.path.abspath(os.path.dirname(__file__)).rsplit(os.sep, 3)[
        0
    ]
    iculog.info(
        'Determined defalt_value based on the location of the python module.\n'
        + f'  defaultvalue = "{defalt_value}".'
    )
  result = _get_save_dir(
      'ICU_DIR',
      defalt_value,
      ('icu4c', 'icu4j', 'tools'),
      ('pom.xml', 'README.md', 'CONTRIBUTING.md'),
  )
  if not result:
    iculog.failure(
        'Please set the ICU_DIR environment variable to the top level '
        "'icu' source dir (containing 'icu4c' and 'icu4j')."
    )
    sys.exit(1)
  return result


def cldr_dir(defalt_value: str | None = None) -> str:
  """Get the path for the 'cldr' repository directory."""
  result = _get_save_dir(
      'CLDR_DIR',
      defalt_value,
      ('common', 'keyboards', 'specs', 'tools'),
      ('pom.xml', 'README.md', 'CONTRIBUTING.md'),
  )
  if not result:
    iculog.failure(
        'Please set the CLDR_DIR environment variable to the top level'
        " 'cldr' source dir (containing 'common')."
    )
    sys.exit(1)
  return result


def cldr_prod_dir(defalt_value: str | None = None) -> str:
  """Get the path for the 'cldr-staging' repository directory."""
  result = _get_save_dir(
      'CLDR_TMP_DIR',
      defalt_value,
      ('births', 'docs/charts', 'production'),
      ('pom.xml', 'README-common.md', 'README-keyboards.md'),
  )
  if not result:
    iculog.failure(
        'Please set the CLDR_TMP_DIR environment variable to the top level'
        " 'cldr-staging' source dir (containing 'production')."
    )
    sys.exit(1)
  return result


def report_dirs():
  """List the values of the known directories with the current environment."""
  iculog.debug(f'__file__(): {__file__}')
  try:
    iculog.info(f'icu_dir(): {icu_dir()}')
  except SystemExit as _:
    pass
  try:
    iculog.info(f'cldr_dir(): {cldr_dir()}')
  except SystemExit as _:
    pass
  try:
    iculog.info(f'cldr_prod_dir(): {cldr_prod_dir()}')
  except SystemExit as _:
    pass


if __name__ == '__main__':
  report_dirs()
