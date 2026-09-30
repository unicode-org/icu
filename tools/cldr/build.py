#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""This build file is the single mechanism for working with
  CLDR code and data when building ICU data.

It includes:
* Building ICU data form CLDR data via cldr-to-icu.
* Building the CLDR libraries needed to support ICU data conversion.
* Copying CLDR test data for ICU regression tests.

Follow the instructions in:
  <icu_root>/docs/processes/cldr-icu.md
which is best viewed as
  https://unicode-org.github.io/icu/processes/cldr-icu.html
"""

import argparse
import os
import sys

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

cldr_dir = icudirs.cldr_dir()
icu_dir = icudirs.icu_dir()
test_data_dir_4c = os.path.join(icu_dir, "icu4c/source/test/testdata/cldr")
test_data_dir_4j = os.path.join(
    icu_dir, "icu4j/main/core/src/test/resources/com/ibm/icu/dev/data/cldr"
)


def _create_catalog(test_data_dir: str, contents: list[str]):
  catalog_file_name = os.path.join(test_data_dir, "personNameTest/catalog.txt")
  icufs.copyfile(
      os.path.join(test_data_dir, "personNameTest/_header.txt"),
      catalog_file_name,
  )
  with open(catalog_file_name, "a", encoding="utf-8") as f:
    for line in contents:
      f.write(line)
      f.write("\n")


def copy_cldr_testdata():
  """Copies CLDR test data directories, after deleting previous
  contents to prevent inconsistent state."""
  clean_cldr_testdata()
  src_dir_base = os.path.join(cldr_dir, "common/testData")
  # CLDR test data directories to be copied into ICU.
  # Add directories here to control which test data is installed.
  cldr_test_data = [
      "localeIdentifiers",
      "messageFormat",  # Used in MessageFormatter tests
      "personNameTest",  # Used in ExhaustivePersonNameTest
      "units",  # Used in UnitsTest tests
  ]
  for test_dir in cldr_test_data:
    src_dir = os.path.join(src_dir_base, test_dir)
    iculog.subtitle(f"Copying CLDR test data to {src_dir}")
    icufs.copycleandir(src_dir, os.path.join(test_data_dir_4c, test_dir))
    icufs.copycleandir(src_dir, os.path.join(test_data_dir_4j, test_dir))

  iculog.subtitle("Creating catalog.txt file")
  # collect the file names in the cldr/personNameTest directory
  contents = os.listdir(os.path.join(src_dir_base, "personNameTest"))
  contents.sort()
  contents = list(filter(lambda x: not x.startswith("_"), contents))
  _create_catalog(test_data_dir_4c, contents)
  _create_catalog(test_data_dir_4j, contents)


def clean_cldr_testdata():
  """Deletes CLDR test data"""
  iculog.title("Removing test dirs")
  icufs.rmdir(test_data_dir_4c)
  icufs.rmdir(test_data_dir_4j)


def reset_cldr_testdata():
  """Restores CLDR test data"""
  iculog.title("Git-restore test dirs")
  icuproc.run_with_logging(f"git checkout -- {test_data_dir_4c}")
  icuproc.run_with_logging(f"git checkout -- {test_data_dir_4j}")

def build_prereqs():
  """build all prereqs"""
  iculog.title("Build Prereqs: ICU4J, CLDR")
  old_dir = icufs.pushd(icu_dir)
  icuproc.run_with_logging("mvn -B clean install -f icu4j -DskipTests -DskipITs", logfile="build_icu4j.log")
  icufs.popd(old_dir)
  old_dir = icufs.pushd(cldr_dir)
  icuproc.run_with_logging(f"mvn -B clean install -pl :cldr-all,:cldr-code -DskipTests -DskipITs", logfile="build_cldr.log")
  icufs.popd(old_dir)
  build()

def build():
  """build cldr-to-icu"""
  iculog.title("Build cldr-to-icu")
  old_dir = icufs.pushd(f"{icu_dir}/tools/cldr/cldr-to-icu/")
  icuproc.run_with_logging(f"mvn -B clean package -DskipTests -DskipITs", logfile="build_cldr-to-icu.log")
  icufs.popd(old_dir)

def convert_data():
  """Convert CLDR to ICU"""
  iculog.title("Convert CLDR to ICU")
  old_dir = icufs.pushd(f"{icu_dir}/tools/cldr/cldr-to-icu/")
  icuproc.run_with_logging(f"java -jar target/cldr-to-icu-1.0-SNAPSHOT-jar-with-dependencies.jar", logfile="convert.log")
  icufs.popd(old_dir)

def proddata():
  """Regenerate production data"""
  iculog.title("Regenerate production data from CLDR")
  cldrtools_jar = f"{cldr_dir}/tools/cldr-code/target/cldr-code.jar"
  icuproc.run_with_logging(
      "java"
      f" -cp {cldrtools_jar}"
      " org.unicode.cldr.tool.GenerateProductionData"
      " -v", logfile="proddata.log")

def main() -> int:
  parser = argparse.ArgumentParser()
  parser.add_argument(
      "-cp",
      "--copy-cldr-testdata",
      help="Copies CLDR test data directories, after deleting"
      " previous contents to prevent inconsistent state.",
      action="store_true",
  )
  parser.add_argument(
      "-rm",
      "--remove-cldr-testdata",
      help="Deletes CLDR test data",
      action="store_true",
  )
  parser.add_argument(
      "-reset",
      "--reset-cldr-testdata",
      help="Restores the CLDR test data from git",
      action="store_true",
  )
  parser.add_argument(
    "--build-prereqs",
    help="Build ICU and CLDR prereqs (and the tool)",
    action="store_true",
  )
  parser.add_argument(
    "--build",
    help="Build tooling",
    action="store_true",
  )
  parser.add_argument(
    "--convert",
    help="Convert CLDR to ICU data",
    action="store_true",
  )
  parser.add_argument(
    "--proddata",
    help="Generate CLDR production data",
    action="store_true",
  )
  cmd = parser.parse_args()

  if cmd.remove_cldr_testdata:
    clean_cldr_testdata()
  elif cmd.reset_cldr_testdata:
    reset_cldr_testdata()
  elif cmd.convert:
    if cmd.build_prereqs:
      build_prereqs()  # Implies and calls --build
    elif cmd.build:
      build()
    if cmd.proddata:
      proddata()
    convert_data()
    if cmd.copy_cldr_testdata:
      copy_cldr_testdata()
  elif cmd.build_prereqs:
    build_prereqs()
  elif cmd.proddata:
    proddata()
  elif cmd.build:
    build()
  elif cmd.copy_cldr_testdata:
    copy_cldr_testdata()
  else:
    parser.print_help()
  return 0


if __name__ == "__main__":
  sys.exit(main())
