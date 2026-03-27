#!/usr/bin/env python3 -B
#
# Copyright (C) 2026 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

"""This is the build file for ICU tools."""

import argparse
import os
import sys
import datetime
import subprocess

try:
  from libs import iculog
  from libs import icuproc
except (ModuleNotFoundError, ImportError) as e:
  print("Make sure you define PYTHONPATH pointing to the ICU modules:")
  print("  export PYTHONPATH=<icu_root>/tools/py")
  print("On Windows:")
  print("  set PYTHONPATH=<icu_root>\\tools\\py")
  sys.exit(1)


basedir = "."
rsrc_dir = os.path.join(basedir, "src/main/resources")
apireport_jar = "target/icu4c-apireport.jar"
newdir = ""
olddir = ""


def _init() -> bool:
  """Checks that .jar to run exists"""
  iculog.info(str(datetime.datetime.now()))
  subprocess.run("mvn -version", encoding="utf-8", shell=True, check=True)
  iculog.info(f"tools jar={apireport_jar}")
  iculog.info(f"basedir={basedir}")
  return os.path.isfile(apireport_jar)


def tools():
  """compile release tools"""
  if not _init():
    iculog.failure(f"The {apireport_jar} was not built.")
  icuproc.run_with_logging("mvn package", logfile="-")


def clean():
  """remove all build targets"""
  _init()
  icuproc.run_with_logging("mvn clean", logfile="-")


def apireport():
  tools()
  xslt_dir = f"{rsrc_dir}/com/ibm/icu/dev/tools/docs"
  icuproc.run_with_logging(
      "java"
      f" -jar {apireport_jar}"
      f" --olddir {olddir}"
      f" --newdir {newdir}"
      f" --cppxslt {xslt_dir}/dumpAllCppFunc.xslt"
      f" --cxslt {xslt_dir}/dumpAllCFunc.xslt"
      f" --reportxslt {xslt_dir}/genReport.xslt"
      f" --resultfile {basedir}/APIChangeReport.html",
      logfile="-",
  )


def apireport_md():
  tools()
  xslt_dir = f"{rsrc_dir}/com/ibm/icu/dev/tools/docs"
  icuproc.run_with_logging(
      "java"
      f" -jar {apireport_jar}"
      f" --olddir {olddir}"
      f" --newdir {newdir}"
      f" --cppxslt {xslt_dir}/dumpAllCppFunc.xslt"
      f" --cxslt {xslt_dir}/dumpAllCFunc.xslt"
      f" --reportxslt {xslt_dir}/genReport_md.xslt"
      f" --resultfile {basedir}/APIChangeReport.md",
      logfile="-",
  )


def apireport_xml():
  tools()
  xslt_dir = f"{rsrc_dir}/com/ibm/icu/dev/tools/docs"
  icuproc.run_with_logging(
      "java"
      f" -jar {apireport_jar}"
      f" --olddir {olddir}"
      f" --newdir {newdir}"
      f" --cppxslt {xslt_dir}/dumpAllCppFunc_xml.xslt"
      f" --cxslt {xslt_dir}/dumpAllCFunc_xml.xslt"
      f" --reportxslt {xslt_dir}/genreport_xml.xslt"
      f" --resultfile {basedir}/APIChangeReport.xml",
      logfile="-",
  )


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument(
      "--clean", help="Remove all build targets", action="store_true"
  )
  parser.add_argument(
      "--tools", help="Compile the release tools", action="store_true"
  )
  parser.add_argument(
      "--olddir", help="Directory that contains xml docs of old version"
  )
  parser.add_argument(
      "--newdir", help="Directory that contains xml docs of new version"
  )
  parser.add_argument(
      "--apireport",
      help="Generate the apireport in HTML format",
      action="store_true",
  )
  parser.add_argument(
      "--apireport_md",
      help="Generate the apireport in Markdown format",
      action="store_true",
  )
  parser.add_argument(
      "--apireport_xml",
      help="Generate the apireport in XML format",
      action="store_true",
  )
  cmd = parser.parse_args()

  if cmd.olddir:
    global olddir
    olddir = cmd.olddir
  if cmd.newdir:
    global newdir
    newdir = cmd.newdir

  if cmd.clean:
    clean()
  elif cmd.tools:
    tools()
  elif cmd.apireport:
    apireport()
  elif cmd.clean:
    clean()
  elif cmd.apireport_md:
    apireport_md()
  elif cmd.apireport_xml:
    apireport_xml()
  else:
    parser.print_help()

  return 0


if __name__ == "__main__":
  sys.exit(main())
