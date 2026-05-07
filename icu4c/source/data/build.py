#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
"""Generates data in cldr-staging/production from the cldr main repo"""

import argparse
import os
import sys
import datetime
import subprocess

try:
  from libs import icudirs
  from libs import icufs
  from libs import iculog
  from libs import icuproc
except (ModuleNotFoundError, ImportError) as e:
  print("Make sure you define PYTHONPATH pointing to the ICU modules:")
  print("  export PYTHONPATH=<icu_root>/tools/py")
  print("On Windows:")
  print("  set PYTHONPATH=<icu_root>\\tools\\py")
  sys.exit(1)


basedir = "."
cldr_tmp_dir = None
cldr_prod_dir = None
cldrtools_jar = None
cldr_tmp_dir = None
notes_dir: str = "./notes"


def _init():
  """Initialization. Check folders existence, cldr-code.jar exists, etc."""
  iculog.subtitle("init()")
  iculog.info(str(datetime.datetime.now()))

  cldr_dir = icudirs.cldr_dir()

  cldrtools_dir = os.path.join(cldr_dir, "tools")
  iculog.info(f"cldr_dir:{cldr_dir}")
  iculog.info(f"cldrtools_dir:{cldrtools_dir}")
  if not os.path.isdir(cldrtools_dir):
    iculog.failure(
        "Please make sure that the CLDR tools directory"
        " is checked out into CLDR_DIR"
    )

  dir_to_check = f"{cldrtools_dir}/cldr-code/target/classes"
  if not os.path.isdir(dir_to_check):
    iculog.failure(f"Can't find {dir_to_check}. Please build cldr-code.jar.")

  global cldrtools_jar
  cldrtools_jar = f"{cldrtools_dir}/cldr-code/target/cldr-code.jar"
  if not os.path.isfile(cldrtools_jar):
    iculog.failure(
        f"CLDR classes not found in {cldrtools_dir}/cldr-code/target/classes."
        " Please build cldr-code.jar."
    )

  global cldr_tmp_dir
  cldr_tmp_dir = icudirs.cldr_prod_dir()
  global cldr_prod_dir
  cldr_prod_dir = f"{cldr_tmp_dir}/production/"

  global notes_dir
  notes_dir = os.environ.get("NOTES", "./notes")

  subprocess.run("mvn -version", encoding="utf-8", shell=True, check=True)
  iculog.info(f"cldr tools dir: {cldrtools_dir}")
  iculog.info(f"cldr tools jar: {cldrtools_jar}")
  iculog.info(f"CLDR_TMP_DIR: {cldr_tmp_dir} ")
  iculog.info(f"cldr.prod_dir (production data): {cldr_prod_dir}")
  iculog.info(f"notes_dir: {notes_dir}")


def cleanprod():
  """Remove the data in cldr-staging/production"""
  iculog.title("cleanprod()")
  icufs.rmdir(f"{cldr_prod_dir}/common")
  icufs.rmdir(f"{cldr_prod_dir}/keyboards")


def restoreprod():
  """Restore the git version of data in cldr-staging/production"""
  iculog.title("restoreprod()")
  if not cldr_prod_dir:
    iculog.failure("cldr_prod_dir not configured")
    return
  old_dir = icufs.pushd(cldr_prod_dir)
  icufs.rmdir("common")
  icuproc.run_with_logging(
      "git checkout -- common",
      logfile=os.path.join(notes_dir, "cldr-newData-restorecommonLog.txt"),
  )
  icufs.rmdir("keyboards")
  icuproc.run_with_logging(
      "git checkout -- keyboards",
      logfile=os.path.join(notes_dir, "cldr-newData-restorekeyboardsLog.txt"),
  )
  icufs.popd(old_dir)


def proddata():
  """Generates data in cldr-staging/production"""
  cleanprod()
  iculog.title("proddata()")
  iculog.info(f"Rebuilding {cldr_prod_dir} - takes a while!")
  # setup prod data
  icuproc.run_with_logging(
      "java"
      f" -cp {cldrtools_jar}"
      " org.unicode.cldr.tool.GenerateProductionData"
      " -v",
      logfile=os.path.join(notes_dir, "cldr-newData-proddataLog.txt"),
  )


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument(
      "-c", "--cleanprod", help="remove all build targets", action="store_true"
  )
  parser.add_argument(
      "-p",
      "--proddata",
      help="Rebuilds files in cldr-staging/production",
      action="store_true",
  )
  parser.add_argument(
      "-r",
      "--restore",
      help="Restore (from git) the filed removed by cleanprod",
      action="store_true",
  )
  cmd = parser.parse_args()

  if cmd.cleanprod:
    _init()
    cleanprod()
  elif cmd.proddata:
    _init()
    proddata()
  elif cmd.restore:
    _init()
    restoreprod()
  else:
    parser.print_help()

  return 0


if __name__ == "__main__":
  sys.exit(main())
