/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  nmake.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul18
*   created by: Vladimir Weinstein
*
* Emit a NMAKE makefile
*/

#include "makefile.h"
#include <stdio.h>

char linebuf[2048];

/* Write any setup/initialization stuff */
void
pkg_mak_writeHeader(FileStream *f, const UPKGOptions *o)
{
  sprintf(linebuf, "## Makefile for %s created by pkgtool\n"
                   "## from ICU Version %s\n"
                   "\n",
          o->shortName,
          U_ICU_VERSION);
  T_FileStream_writeLine(f, linebuf);

  sprintf(linebuf, "NAME=%s\n"
          "TARGETDIR=%s\n"
          "TEMP_DIR=%s\n"
          "MODE=%s\n"
          "MAKEFILE=%s\n"
          "\n\n\n",
          o->shortName,
          o->targetDir,
          o->tmpDir,
          o->mode,
          o->makeFile);
  T_FileStream_writeLine(f, linebuf);

  sprintf(linebuf, "## List files [%d] containing data files to process (note: - means stdin)\n"
                            "LISTFILES= ",
                         pkg_countCharList(o->fileListFiles));
  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->fileListFiles, " ", " \\\n", 0);

  T_FileStream_writeLine(f, "\n\n\n");

  sprintf(linebuf, "## Data Files [%d]\n"
                            "DATAFILES= ",
                         pkg_countCharList(o->files));

  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->files, " ", " \\\n", -1);

  T_FileStream_writeLine(f, "\n\n\n");

  sprintf(linebuf, "## Data File Paths [%d]\n"
                            "DATAFILEPATHS= ",
                         pkg_countCharList(o->filePaths));

  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->filePaths, " ", " \\\n", 1);

  T_FileStream_writeLine(f, "\n\n\n");

}

/* Write a stanza in the makefile, with specified   "target: parents...  \n\n\tcommands" [etc] */
void
pkg_mak_writeStanza(FileStream *f, const UPKGOptions *o, 
                    const char *target,
                    CharList* parents,
                    CharList* commands)
{
	/* intentionally left blank */
}

/* write any cleanup/post stuff */
void
pkg_mak_writeFooter(FileStream *f, const UPKGOptions *o)
{
	/* intentionally left blank */
}

