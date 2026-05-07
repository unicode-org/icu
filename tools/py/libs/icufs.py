# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""File system operations, more reliable and with logging."""

import os
import shutil
import stat

from libs import iculog


def _remove_readonly(func, path: str, _):
  """Clear the readonly bit and reattempt the removal."""
  try:
    iculog.warning(f'problem removing {path}')
    os.chmod(path, stat.S_IWRITE)
    func(path)
  except OSError as ex:
    iculog.error(f'Skipped: {path}, because:\n{ex}')


def rmdir(dir_name: str) -> None:
  """Remove an existing folder (if it exists)."""
  iculog.info(f'[rmdir] {dir_name}')
  if os.path.isdir(dir_name):
    # Using `shutil.rmtree` with `onexc` / `onerror` fails in the callback
    # when trying to call the function with:
    #   TypeError: open() missing required argument 'flags' (pos 2)
    # This hapens for read-only files in a no-read / no-exec folder.
    # My guess is that `shutil.rmtree` tries to open the folder to iterate
    # it and remove files, and that fails.
    # And the callback calling `open` (func) again fails because it does
    # not have the right arguments ("missing required argument 'flags'").
    os.chmod(dir_name, stat.S_IRWXU)  # rwx for user, 0o700
    shutil.rmtree(dir_name, onerror=_remove_readonly)


def mkdir(dir_name: str) -> None:
  """Create a folder, if it does no exist already.

  If it is a deep folder, it creates all the parents needed.

  Args:
    dir_name: the folder create
  """
  iculog.info(f'[mkdir] {dir_name}')
  os.makedirs(dir_name, exist_ok=True)
  if not os.path.isdir(dir_name):
    iculog.failure(f'unable to create folder: {dir_name}')


def makecleandir(dir_name: str):
  """Remove an existing folder (if it exists) and create a clean one."""
  rmdir(dir_name)
  mkdir(dir_name)


def _ignore_git_dir(directory: str, files: list[str]) -> list[str]:  # pylint: disable=unused-argument
  # This is a callback for shutil.copytree, and must take those arguments.
  # But we don't use them, hence the pylint disable.
  return ['.git']


def copycleandir(src_dir_name: str,
                 trg_dir_name: str,
                 skip_git_dir: bool = False):
  """Remove an existing folder (if it exists) and copy the source folder."""
  rmdir(trg_dir_name)
  iculog.info(
      f'[copytree] {src_dir_name} {trg_dir_name} skip_git_dir={skip_git_dir}')
  if skip_git_dir:
    shutil.copytree(src_dir_name, trg_dir_name, ignore=_ignore_git_dir)
  else:
    shutil.copytree(src_dir_name, trg_dir_name)


def copyfile(src_file_name: str, trg_file_name: str):
  """Remove an existing file (if it exists) and copy the source file."""
  rmfile(trg_file_name)
  iculog.info(f'[copyfile] {src_file_name} {trg_file_name}')
  shutil.copyfile(src_file_name, trg_file_name)


def rmfile(file_name: str) -> None:
  """Remove a file, if it exists and it is a file."""
  iculog.info(f'[rmfile] {file_name}')
  if os.path.isfile(file_name):
    try:
      os.remove(file_name)
    except PermissionError:
      os.chmod(file_name, stat.S_IWUSR)  # Make it writeable
      os.remove(file_name)
  elif os.path.exists(file_name):
    iculog.warning(f'[rmfile] Tried to remove a non-file: {file_name}')


def pushd(new_dir: str) -> str:
  """Changes current working folder, returning the previous one.

  This is not a proper `pushd` / `popd` implementation, as it does not save the
  previous folder to return to it. That is something left to the caller.

  Args:
    new_dir: the folder to change to
  Returns:
    returns: the current folder before change
  """
  iculog.info(f'[pushd] {new_dir}')
  cwd = os.getcwd()
  os.chdir(new_dir)
  return cwd


def popd(old_dir: str) -> None:
  """Restores the current working folder."""
  iculog.info(f'[popd] {old_dir}')
  os.chdir(old_dir)
